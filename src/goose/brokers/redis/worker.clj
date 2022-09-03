(ns goose.brokers.redis.worker
  {:no-doc true}
  (:require
    [goose.brokers.redis.consumer :as redis-consumer]
    [goose.brokers.redis.heartbeat :as redis-heartbeat]
    [goose.brokers.redis.metrics :as redis-metrics]
    [goose.brokers.redis.orphan-checker :as redis-orphan-checker]
    [goose.brokers.redis.retry :as redis-retry]
    [goose.brokers.redis.scheduler :as redis-scheduler]
    [goose.defaults :as d]
    [goose.job :as job]
    [goose.metrics.middleware :as metrics-middleware]
    [goose.utils :as u]

    [clojure.tools.logging :as log]
    [com.climate.claypoole :as cp])
  (:import
    [java.util.concurrent TimeUnit]))

(defn- internal-stop
  "Gracefully shuts down the worker threadpool."
  [{:keys [thread-pool internal-thread-pool graceful-shutdown-sec]
    :as   opts}]
  ; Set state of thread-pool to SHUTDOWN.
  (log/warn "Shutting down thread-pool...")
  (cp/shutdown thread-pool)
  (cp/shutdown internal-thread-pool)

  ; Interrupt scheduler to exit sleep & terminate thread-pool.
  ; If not interrupted, shutdown time will increase by
  ; max(graceful-shutdown-sec, sleep time)
  (cp/shutdown! internal-thread-pool)

  ; Give jobs executing grace time to complete.
  (log/warn "Awaiting executing jobs to complete.")

  (.awaitTermination
    thread-pool
    graceful-shutdown-sec
    TimeUnit/SECONDS)

  (redis-heartbeat/stop opts)

  ; Set state of thread-pool to STOP.
  (log/warn "Sending InterruptedException to close threads.")
  (cp/shutdown! thread-pool))

(defn- chain-middlewares
  [middlewares]
  (let [call (if middlewares
               (-> redis-consumer/execute-job (middlewares))
               redis-consumer/execute-job)]
    (-> call
        (metrics-middleware/wrap-metrics)
        (job/wrap-latency)
        (redis-retry/wrap-failure))))

(defn start
  [{:keys [threads queue middlewares] :as common-opts}]
  (let [thread-pool (cp/threadpool threads)
        ; Internal threadpool for scheduler, orphan-checker & heartbeat.
        internal-thread-pool (cp/threadpool d/redis-internal-thread-pool-size)
        random-str (subs (str (random-uuid)) 24 36) ; Take last 12 chars of UUID.
        id (str queue ":" (u/hostname) ":" random-str)
        call (chain-middlewares middlewares)
        redis-opts {:id                   id
                    :thread-pool          thread-pool
                    :internal-thread-pool internal-thread-pool
                    :call                 call

                    :process-set          (str d/process-prefix queue)
                    :prefixed-queue       (d/prefix-queue queue)
                    :in-progress-queue    (redis-consumer/preservation-queue id)}
        opts (merge redis-opts common-opts)]

    (cp/future internal-thread-pool (redis-metrics/run opts))
    (cp/future internal-thread-pool (redis-heartbeat/run opts))
    (cp/future internal-thread-pool (redis-scheduler/run opts))
    (cp/future internal-thread-pool (redis-orphan-checker/run opts))

    (dotimes [_ threads]
      (cp/future thread-pool (redis-consumer/run opts)))

    #(internal-stop opts)))
