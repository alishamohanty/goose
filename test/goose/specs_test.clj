(ns goose.specs-test
  (:require
    [goose.brokers.redis.broker :as redis]
    [goose.brokers.rmq.broker :as rmq]
    [goose.client :as c]
    [goose.defaults :as d]
    [goose.metrics.statsd :as statsd]
    [goose.specs :as specs]
    [goose.test-utils :as tu]
    [goose.worker :as w]

    [clojure.test :refer [deftest is are]]))

(defn single-arity-fn [_] "dummy")
(def now (java.time.Instant/now))

(deftest specs-test
  (specs/instrument)
  (are [sut]
    (is
      (thrown-with-msg?
        clojure.lang.ExceptionInfo
        #"Call to goose.* did not conform to spec."
        (sut)))

    ; Client specs
    ; :execute-fn-sym
    #(c/perform-async tu/redis-client-opts 'my-fn)
    #(c/perform-async tu/redis-client-opts `my-fn)
    #(c/perform-async tu/redis-client-opts `tu/redis-client-opts)

    ; :args
    #(c/perform-async tu/redis-client-opts `tu/my-fn specs-test)

    ; :sec
    #(c/perform-in-sec tu/redis-client-opts 0.2 `tu/my-fn)

    ; :instant
    #(c/perform-at tu/redis-client-opts "22-July-2022" `tu/my-fn)

    ; Worker specs
    #(w/start (assoc tu/redis-worker-opts :threads -1.1))
    #(w/start (assoc tu/redis-worker-opts :graceful-shutdown-sec -2))
    #(w/start (assoc tu/redis-worker-opts :metrics-plugin :invalid))

    ; :statad-opts
    #(statsd/new (assoc statsd/default-opts :enabled? 1))
    #(statsd/new (assoc statsd/default-opts :host 127.0))
    #(statsd/new (assoc statsd/default-opts :port "8125"))
    #(statsd/new (assoc statsd/default-opts :prefix :symbol))
    #(statsd/new (assoc statsd/default-opts :sample-rate 1))
    #(statsd/new (assoc statsd/default-opts :tags '("service:maverick")))

    ; Common specs
    ; :broker
    #(c/perform-async (assoc tu/redis-client-opts :broker :invalid) `tu/my-fn)

    ; :queue
    #(c/perform-async (assoc tu/redis-client-opts :queue :non-string) `tu/my-fn)
    #(w/start (assoc tu/redis-worker-opts :queue (str (range 300))))
    #(c/perform-at (assoc tu/redis-client-opts :queue d/schedule-queue) now `tu/my-fn)
    #(c/perform-in-sec (assoc tu/redis-client-opts :queue d/dead-queue) 1 `tu/my-fn)
    #(c/perform-async (assoc tu/redis-client-opts :queue d/cron-queue) `tu/my-fn)
    #(c/perform-async (assoc tu/redis-client-opts :queue d/cron-entries) `tu/my-fn)
    #(c/perform-async (assoc tu/redis-client-opts :queue (str d/queue-prefix "olttwa")) `tu/my-fn)

    ; :retry-opts
    #(c/perform-async (assoc-in tu/redis-client-opts [:retry-opts :max-retries] -1) `tu/my-fn)
    #(c/perform-at (assoc-in tu/redis-client-opts [:retry-opts :retry-queue] :invalid) now `tu/my-fn)
    #(c/perform-in-sec (assoc-in tu/redis-client-opts [:retry-opts :error-handler-fn-sym] `single-arity-fn) 1 `tu/my-fn)
    #(c/perform-async (assoc-in tu/redis-client-opts [:retry-opts :death-handler-fn-sym] `single-arity-fn) `tu/my-fn)
    #(c/perform-at (assoc-in tu/redis-client-opts [:retry-opts :retry-delay-sec-fn-sym] 'non-fn-sym) now `tu/my-fn)
    #(c/perform-in-sec (assoc-in tu/redis-client-opts [:retry-opts :skip-dead-queue] 1) 1 `tu/my-fn)
    #(c/perform-async (assoc-in tu/redis-client-opts [:retry-opts :extra-key] :foo-bar) `tu/my-fn)

    ; :redis-opts
    #(redis/new (assoc redis/default-opts :url :invalid-url))
    #(redis/new (assoc redis/default-opts :pool-opts :invalid-pool-opts))
    #(redis/new (assoc redis/default-opts :scheduler-polling-interval-sec 0))

    ; :rmq-opts
    #(rmq/new {:settings :invalid})
    #(rmq/new (assoc rmq/default-opts :publisher-confirms {:strategy :invalid}))
    #(rmq/new (assoc rmq/default-opts :publisher-confirms {:strategy :sync :timeout 0}))
    #(rmq/new (assoc rmq/default-opts :publisher-confirms {:strategy :async :ack-handler 'invalid}))
    #(rmq/new (assoc rmq/default-opts :publisher-confirms {:strategy :async :nack-handler `my-fn}))))