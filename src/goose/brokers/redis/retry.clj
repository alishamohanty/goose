(ns goose.brokers.redis.retry
  {:no-doc true}
  (:require
    [goose.brokers.redis.commands :as redis-cmds]
    [goose.defaults :as d]
    [goose.retry]
    [goose.utils :as u]))


(defn- retry-job
  [{:keys [redis-conn error-service-cfg]}
   {{:keys [retry-delay-sec-fn-sym
            error-handler-fn-sym]} :retry-opts
    {:keys [retry-count]}          :state
    :as                            job}
   ex]
  (let [error-handler (u/require-resolve error-handler-fn-sym)
        retry-delay-sec ((u/require-resolve retry-delay-sec-fn-sym) retry-count)
        retry-at (u/add-sec retry-delay-sec)
        job (assoc-in job [:state :retry-at] retry-at)]
    (u/log-on-exceptions (error-handler error-service-cfg job ex))
    (redis-cmds/enqueue-sorted-set redis-conn d/prefixed-retry-schedule-queue retry-at job)))

(defn- bury-job
  [{:keys [redis-conn error-service-cfg]}
   {{:keys [skip-dead-queue
            death-handler-fn-sym]} :retry-opts
    {:keys [last-retried-at]}      :state
    :as                            job}
   ex]
  (let [death-handler (u/require-resolve death-handler-fn-sym)
        dead-at (or last-retried-at (u/epoch-time-ms))
        job (assoc-in job [:state :dead-at] dead-at)]
    (u/log-on-exceptions (death-handler error-service-cfg job ex))
    (when-not skip-dead-queue
      (redis-cmds/enqueue-sorted-set redis-conn d/prefixed-dead-queue dead-at job))))

(defn wrap-failure
  [next]
  (fn [opts job]
    (try
      (next opts job)
      (catch Exception ex
        (let [failed-job (goose.retry/set-failed-config job ex)
              retry-count (get-in failed-job [:state :retry-count])
              max-retries (get-in failed-job [:retry-opts :max-retries])]
          (if (< retry-count max-retries)
            (retry-job opts failed-job ex)
            (bury-job opts failed-job ex)))))))
