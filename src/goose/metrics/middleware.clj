(ns goose.metrics.middleware
  (:require
    [goose.metrics.keys :as keys]
    [goose.metrics.protocol :as protocol]
    [goose.utils :as u]))

(defn ^:no-doc wrap-metrics
  [next]
  (fn [{:keys [metrics-plugin] :as opts}
       {[job-type latency] :latency
        :keys              [execute-fn-sym queue]
        :as                job}]
    (let [tags {:function execute-fn-sym
                :queue    queue}
          start (u/epoch-time-ms)]
      (try
        (protocol/increment metrics-plugin keys/jobs-processed 1 tags)
        ; When a job is executed using API, latency might be negative.
        ; Ignore negative values.
        (when (pos? latency)
          (protocol/timing metrics-plugin job-type latency tags))
        (next opts job)
        (protocol/increment metrics-plugin keys/jobs-success 1 tags)
        (catch Exception ex
          (protocol/increment metrics-plugin keys/jobs-failure 1 tags)
          (throw ex))
        (finally
          (protocol/timing metrics-plugin keys/execution-time (- (u/epoch-time-ms) start) tags))))))
