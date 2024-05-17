(ns goose.console-client
  (:require
    [clojure.tools.namespace.repl :refer [refresh]]
    [compojure.core :refer [context defroutes]]
    [compojure.route :as route]
    [goose.brokers.redis.broker :as redis]
    [goose.brokers.rmq.broker :as rmq]
    [goose.client :as c]
    [goose.console :as console]

    [ring.adapter.jetty :as jetty])
  (:gen-class))

(def redis-url
  (let [host (or (System/getenv "GOOSE_REDIS_HOST") "localhost")
        port (or (System/getenv "GOOSE_REDIS_PORT") "6379")]
    (str "redis://" host ":" port)))

(def rmq-url
  (let [host (or (System/getenv "GOOSE_RABBITMQ_HOST") "localhost")
        port (or (System/getenv "GOOSE_RABBITMQ_PORT") "5672")
        username (or (System/getenv "GOOSE_RABBITMQ_USERNAME") "guest")
        password (or (System/getenv "GOOSE_RABBITMQ_PASSWORD") "guest")]
    (str "amqp://" username ":" password "@" host ":" port)))

(def redis-producer (redis/new-producer
                      (merge redis/default-opts {:url redis-url})))

(def rmq-producer (rmq/new-producer (merge rmq/default-opts
                                           {:settings {:url rmq-url}})))

(def broker (or (System/getenv "GOOSE_BROKER") "redis"))

(defn add-enqueued-jobs []
  (let [client-opts (assoc c/default-opts
                      :broker redis-producer)]
    (c/perform-async client-opts `my-fn "foo" :nar)
    (c/perform-async client-opts `prn nil "foo" \q ["a" 1 2] {"a"    "b"
                                                              1      :2
                                                              2      3
                                                              true   234
                                                              "true" false
                                                              "p"    \p})
    (mapv #(c/perform-async (assoc c/default-opts
                              :queue "random"
                              :broker redis-producer) `prn "foo" %) (range 22))
    (c/perform-async (assoc c/default-opts
                       :queue "long-queue-name-exceeding-10-chars"
                       :broker redis-producer) `prn "foo" :bar)))

(defn brokers [broker]
  (when (= broker "redis")
    (add-enqueued-jobs))
  (get {:redis redis-producer
        :rmq   rmq-producer} (keyword broker)))

(def default-console-opts
  {:broker       (brokers broker)
   :app-name     "Goose's client"
   :route-prefix ""})

(defonce server (atom nil))

(defn routes [console-opts]
  (defroutes goose-routes
             (context (:route-prefix console-opts) []
                      (partial console/app-handler console-opts))
             (route/not-found "<h1>Page not found </h1>")))

(defn start-server [& {:keys [port console-opts]}]
  (println "Starting server!!")
  (reset! server (jetty/run-jetty (routes (merge default-console-opts console-opts))
                                  {:port  (or port 3000)
                                   :join? false})))

(defn -main [& console-opts]
  (start-server console-opts))

(defn stop-server []
  (when-let [s @server]
    (.stop s)
    (println s "Stopped server")))

(defn restart []
  (stop-server)
  (refresh :after 'goose.goose-client/start-server))
