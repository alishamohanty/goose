(ns goose.playground.redis
  "Playground to use Goose API's for Redis as broker"
  (:require
    [clojure.tools.logging :as log]
    [goose.api.cron-jobs :as cron-jobs]
    [goose.api.dead-jobs :as dead-jobs]
    [goose.api.enqueued-jobs :as enqueued-jobs]
    [goose.api.scheduled-jobs :as scheduled-jobs]
    [goose.batch :as batch]
    [goose.brokers.redis.api.batch :as batch-jobs]
    [goose.brokers.redis.broker :as redis]
    [goose.client :as c]
    [goose.metrics.statsd :as statsd]
    [goose.worker :as w]))

(defn my-fn
  [arg1 arg2]
  (println "my-fn called with" arg1 arg2))

; ----------- Define only producers ----------

(def redis-producer (redis/new-producer redis/default-opts))
(def client-opts (assoc c/default-opts :broker redis-producer))

; ----------- Define producer and push work to redis ----------

(let [redis-producer (redis/new-producer redis/default-opts)
      ;; Along with Redis, Goose supports RabbitMQ as well.
      client-opts (assoc c/default-opts :broker redis-producer)]
  ;; Supply a fully-qualified function symbol for enqueuing.
  ;; Args to perform-async are variadic.
  (c/perform-async client-opts `my-fn "foo" :bar)
  (c/perform-in-sec client-opts 10 `my-fn "foo" :bar))

;-----------  Scheduled a job -----------

(c/perform-in-sec client-opts 10 `my-fn "scheduled" :job)

; --------- Define worker, start worker and stop worker in same block -------

;;; 'my-app' namespace should be resolvable by worker.
(let [redis-consumer (redis/new-consumer redis/default-opts)
      ;; Along with Redis, Goose supports RabbitMQ as well.
      worker-opts (assoc w/default-opts :broker redis-consumer
                                        :metrics-plugin (statsd/new statsd/default-opts))
      worker (w/start worker-opts)]
  ;; When shutting down worker...
  (w/stop worker))                                          ; Performs graceful shutsdown.

; ------------------ start and stop worker at own time ------------------

(def redis-consumer (redis/new-consumer redis/default-opts))
(def worker-opts (assoc w/default-opts :broker redis-consumer
                                       :metrics-plugin (statsd/new statsd/default-opts)))

(comment (def worker (w/start worker-opts)))
(comment (w/stop worker))


;------- Define another queue --------
(comment (def client-opts-queue1 (assoc c/default-opts
                                   :queue "queue1"
                                   :broker redis-producer)))

;------- Create a job in the new queue(other than default) --------
(c/perform-async client-opts-queue1 `my-fn "foo" :bar)
(enqueued-jobs/size redis-producer "queue1")


;----- Throw Class cast excpetion in the function
(defn wait-and-throw
  [wait-time-sec]
  (do
    (Thread/sleep (* wait-time-sec 1000))
    (throw ClassCastException)))
(c/perform-async client-opts `wait-and-throw 5)
;-----

; --------  API trial for Redis  --------

; -------- Enqueued API ---------
(enqueued-jobs/list-all-queues redis-producer)
(enqueued-jobs/size redis-producer "default")
(enqueued-jobs/size redis-producer "something")
(enqueued-jobs/find-by-id redis-producer "default" "506c86f3-922d-4853-a80e-aae0810bc8b8")
(let [pattern-match? (fn [job] (do (println job)
                                   (= (:execute-fn-sym job) 'goose.redis/my-fn)))
      limit 5]
  (enqueued-jobs/find-by-pattern redis-producer "default" pattern-match? limit))
;Fetch all the jobs
(let [pattern-match? (fn [_job] true)
      limit 50]
  (enqueued-jobs/find-by-pattern redis-producer "default" pattern-match? limit))

(let [pattern-match? (fn [job] (do (println job)
                                   (= (:id job) "a100eed2-ba9a-4162-93a2-5e39bef13034")))
      limit 5]
  (enqueued-jobs/find-by-pattern redis-producer "default" pattern-match? limit))

(let [async-job (enqueued-jobs/find-by-id redis-producer "default" "1641b37a-6a9f-45a5-bb15-b11890fcc4a1")
      _ (println async-job)]
  (enqueued-jobs/prioritise-execution redis-producer async-job))

(let [job (enqueued-jobs/find-by-id redis-producer "default" "506c86f3-922d-4853-a80e-aae0810bc8b8")]
  (enqueued-jobs/delete redis-producer job))
(enqueued-jobs/purge redis-producer "default")

;-------- Scheduled a job in a different queue ------
(def client-opts
  (assoc c/default-opts :broker redis-producer))
(def client-opts-1
  (assoc c/default-opts
    :queue "something"
    :broker redis-producer))
(c/perform-in-sec client-opts-1 10 `my-fn "scheduled" :job)
;------ Scheduled Jobs API ----------
(scheduled-jobs/size redis-producer)
(scheduled-jobs/find-by-id redis-producer "964940fa-8e2d-49cf-b065-486499bbb15f")
;Check all scheduled jobs
(let [pattern-match? (fn [_job] true)
      limit 5]
  (scheduled-jobs/find-by-pattern redis-producer pattern-match? limit))
(let [pattern-match? (fn [job] (do (println job)
                                   (= (:execute-fn-sym job) 'goose.redis/my-fn)))
      limit 5]
  (scheduled-jobs/find-by-pattern redis-producer pattern-match? limit))
;Schedule run at 1701329237419
(let [async-job (scheduled-jobs/find-by-id redis-producer "dc236d78-c25c-44c7-b200-93e4c332fa4a")
      _ (println async-job)]
  (scheduled-jobs/prioritise-execution redis-producer async-job))
(let [job (scheduled-jobs/find-by-id redis-producer "ecd1fa67-313e-44cf-9c24-97df5b84bf27")]
  (scheduled-jobs/delete redis-producer job))
(scheduled-jobs/purge redis-producer)

;--------- Batch jobs API -------------
(defn send-emails
  [email-id]
  (log/infof "Sending email to: %s" email-id))

(defn multi-arity-fn
  [arg1 arg2 & args]
  (log/info "Received args:" arg1 arg2 args))

(defn my-callback
  [batch-id status]
  (condp = status
    batch/status-success (log/infof "Batch: %s successful." batch-id)
    batch/status-dead (log/infof "Batch: %s dead." batch-id)
    batch/status-partial-success (log/infof "Batch: %s partially successful." batch-id)))

(let [redis-producer (redis/new-producer redis/default-opts)
      ;; Along with Redis, Goose supports RabbitMQ as well.
      client-opts (assoc c/default-opts :broker redis-producer)
      batch-opts {:callback-fn-sym `my-callback
                  :linger-sec      86400}
      ;; For single-arity functions
      email-ids ["foo@gmail.com" "bar@gmail.com" "baz@gmail.com"]
      email-args-coll (map list email-ids)
      ;; Use Goose's utility function to construct args-coll
      ;; for multi-arity or variadic functions.
      multi-args-coll (-> []
                          (batch/construct-args :foo :bar :baz)
                          (batch/construct-args :fizz :buzz))]

  (c/perform-batch client-opts batch-opts `send-emails email-args-coll)
  (c/perform-batch client-opts batch-opts `multi-arity-fn multi-args-coll))

(let [batch-id "75488f23-3255-4a3f-8b98-a978506f9d43"]
  (batch-jobs/status redis-producer batch-id))

(batch-jobs/delete redis-producer "a56c3adf-3fd8-4963-b994-7ddc679130bd")

;-------- periodic/cron jobs API ----------
(let [redis-producer (redis/new-producer redis/default-opts)
      ;; Along with Redis, Goose supports RabbitMQ as well.
      client-opts (assoc c/default-opts :broker redis-producer)

      name "my-periodic-job-2"
      cron-schedule "*/3 * * * *"                           ; Runs every 5 mins.
      timezone "US/Pacific"                                 ; Defaults to System timezone if not mentioned.
      cron-opts {:cron-name     name
                 :cron-schedule cron-schedule
                 :timezone      timezone}]
  (c/perform-every client-opts cron-opts `my-fn :static "arg"))

(cron-jobs/size redis-producer)
(cron-jobs/find-by-name redis-producer "my-periodic-job-1")
(cron-jobs/find-by-name redis-producer "my-periodic-job")
(cron-jobs/delete redis-producer "my-periodic-job")

; --------- Dead Jobs ------------
(dead-jobs/size redis-producer)
(dead-jobs/pop redis-producer)
(let [_dead-job (dead-jobs/find-by-id redis-producer "946f9d97-b276-4a65-844f-662050d71f32")
      d-job (dead-jobs/find-by-pattern redis-producer (fn [j] (= (:id j)
                                                                 "946f9d97-b276-4a65-844f-662050d71f32")))
      _ (dead-jobs/replay-job redis-producer d-job)
      _ (dead-jobs/replay-n-jobs redis-producer 10)
      _ (dead-jobs/delete redis-producer d-job)
      _ (dead-jobs/delete-older-than redis-producer 1659090656000)]
  (dead-jobs/purge redis-producer))
