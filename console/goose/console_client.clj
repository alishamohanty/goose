(ns goose.console-client
  (:require
    [clojure.tools.namespace.repl :refer [refresh]]
    [compojure.core :refer [context defroutes]]
    [compojure.route :as route]
    [goose.brokers.redis.broker :as redis]
    [goose.client :as c]
    [goose.console :as console]

    [ring.adapter.jetty :as jetty]))

(def redis-url
  (let [host (or (System/getenv "GOOSE_REDIS_HOST") "localhost")
        port (or (System/getenv "GOOSE_REDIS_PORT") "6379")]
    (str "redis://" host ":" port)))

(def redis-producer (redis/new-producer (merge redis/default-opts {:url redis-url})))

(def default-console-opts
  {:broker       redis-producer
   :app-name     "Aal's app"
   :route-prefix ""})

(defonce server (atom nil))

(defn routes [console-opts]
  (defroutes goose-routes
             (context (:route-prefix console-opts) []
                      (partial console/app-handler console-opts))
             (route/not-found "<h1>Page not found </h1>")))

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

(defn start-server [& {:keys [port console-opts]}]
  (add-enqueued-jobs)
  (prn "Hello world!!!" console-opts default-console-opts)
  (reset! server (jetty/run-jetty (routes (merge default-console-opts console-opts))
                                  {:port  (or port 3000)
                                   :join? false})))
(defn stop-server []
  (when-let [s @server]
    (.stop s)
    (println s "Stopped server")))

(defn restart []
  (stop-server)
  (refresh :after 'goose.goose-client/start-server))

