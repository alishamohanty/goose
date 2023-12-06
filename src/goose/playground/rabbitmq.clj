(ns goose.playground.rabbitmq
  "Playground to use Goose APIs with rabbitmq as broker"
  (:require [goose.api.dead-jobs :as dead-jobs]
            [goose.client :as c]
            [goose.worker :as w]
            [goose.api.enqueued-jobs :as enqueued-jobs]
            [goose.brokers.rmq.broker :as rmq] ))

; ----- Producer ------
(def rmq-producer (rmq/new-producer rmq/default-opts))
(def client-opts (assoc c/default-opts :broker rmq-producer))

; ------ Consumer ------
(def rmq-consumer (rmq/new-consumer rmq/default-opts))
(def worker-opts (assoc w/default-opts :broker rmq-consumer))

(comment (def worker (w/start worker-opts)))
(comment (def stop (w/stop worker)))

(defn my-fn
  [arg1 arg2]
  (println "my-fn called with" arg1 arg2))

;Enqueue a regular job
(c/perform-async client-opts `my-fn "foo" :bar)
;Scheduled Job
(c/perform-in-sec client-opts 10 `my-fn "scheduled" :job)
;Batch and Periodic Jobs are not supported

;Size of each type of queue, if it exists
(enqueued-jobs/size rmq-producer "default")

;Trying API that is not supported
(enqueued-jobs/prioritise-execution rmq-producer "")
;Error produced:
;Execution error (AbstractMethodError) at goose.api.enqueued-jobs/prioritise-execution (enqueued_jobs.clj:35).
;Receiver class goose.brokers.rmq.broker.RabbitMQ does not define or inherit an implementation of the resolved method 'abstract java.lang.Object enqueued_jobs_prioritise_execution(java.lang.Object)' of interface goose.broker.Broker.

;Purge an enqueued queue
(enqueued-jobs/purge rmq-producer "default")


;------ Dead Jobs API ---------
(dead-jobs/size rmq-producer)
(dead-jobs/pop rmq-producer)
;=> {... map of job ...}

(dead-jobs/replay-n-jobs rmq-producer 20)
(dead-jobs/purge rmq-producer)

;Unusual exception comes in at times:-
;Execution error (AlreadyClosedException) at com.rabbitmq.client.impl.AMQChannel/ensureIsOpen (AMQChannel.java:258).
;channel is already closed due to channel error; protocol method: #method<channel.close>(reply-code=404, reply-text=NOT_FOUND - no queue 'goose/queue:classic' in vhost '/', class-id=50, method-id=10)
