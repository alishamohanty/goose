(ns goose.playground.console-ui.core
  "Spike multiple designs for landing page of Goose Console (based on brokers)"
  (:require [goose.api.enqueued-jobs :as enqueued-jobs]
            [goose.brokers.redis.broker :as redis]
            [goose.brokers.rmq.broker :as rmq]
            [ring.middleware.params :refer [wrap-params]]
            [ring.adapter.jetty :as jetty]
            [ring.util.response :as response]))

(defonce server (atom nil))

;----- Option 1---------
;The user will select a Broker in the landing page.
;Once the user selects which broker and the connection URL, the UI will page for that broker

(defn landing-page []
  "<h1> Landing Page </h1>
    <form action=\"/redis\" method=\"post\">
      <label for=\"url\">Redis URL:</label><br>
      <input type=\"text\" id=\"url\" name=\"url\" value=\"redis://localhost:6379\"><br>
      <input type=\"submit\" value=\"Submit\">\n
     </form>
     <p> OR </p>
     <form action=\"/rabbitmq\" method=\"post\">
      <label for=\"url\">RabbitMQ URL:</label><br>
      <input type=\"text\" id=\"url\" name=\"url\" value=\"amqp://guest:guest@localhost:5672\"><br>
      <input type=\"submit\" value=\"Submit\">
     </form>")

(defn enqueued-jobs [producer]
  (let [jobs-in-default-q (enqueued-jobs/size producer "default")]
    (str "<h1>Number of enqueued jobs in default queue: " jobs-in-default-q "</h1>")))

(defn landing-page-redis [req]
  (let [url (get-in req [:form-params "url"])
        opts (merge redis/default-opts (when (not (= url ""))
                                         {:url url}))
        redis-producer (redis/new-producer opts)]
    (enqueued-jobs redis-producer)))

(defn landing-page-rabbitmq [req]
  (let [url (get-in req [:form-params "url"])
        opts (update-in rmq/default-opts [:settings :uri] (fn [existing-url] (or url existing-url)))
        rmq-producer (rmq/new-producer opts)]
    (enqueued-jobs rmq-producer)))

(defn route-handler-option-1 [request]
  (let [uri (:uri request)
        method (:request-method request)]
    (cond
      (= uri "/") (response/response (landing-page))
      (and (= uri "/redis") (= method :post)) (response/response (landing-page-redis request))
      (and (= uri "/rabbitmq") (= method :post)) (response/response (landing-page-rabbitmq request))
      :else (response/response "<h1>Not Found</h1>"))))

(defn start-server []
  (reset! server (jetty/run-jetty (-> route-handler-option-1
                                      wrap-params) {:port  3001
                                                    :join? false}))
  (println "Server started on port 3001"))

(defn stop-server []
  (when-let [s @server]
    (.stop s)
    (reset! server nil)
    (println "Server stopped")))

(comment (start-server))
(comment (stop-server))


; -------- Option-2 ----------
;The broker is already selected by the User(in the code) and the UI only serves requests for the that broker.
;There won't be any options in UI to select among brokers.
;The same UI can be used for all brokers.

(defn enqueued-jobs [{:keys [producer]}]
  (let [jobs-in-default-q (enqueued-jobs/size producer "default")]
    (str "<h1>Number of enqueued jobs: " jobs-in-default-q "</h1>")))

(defn route-handler-option-2 [producer]
  (fn [req]
    (let [request (assoc req :producer producer)
          uri (:uri request)]
      (cond
        (= uri "/enqueued-jobs") (response/response (enqueued-jobs request))
        :else (response/response "<h1>Not Found</h1>")))))

(defn start-server [producer]
  (reset! server (jetty/run-jetty (route-handler-option-2 producer)
                                  {:port  3001 :join? false}))
  (println "Server started on port 3001"))

(defn stop-server []
  (when-let [s @server]
    (.stop s)
    (reset! server nil)
    (println "Server stopped")))

;Options 2
(comment (start-server (redis/new-producer redis/default-opts)))
(comment (start-server (rmq/new-producer rmq/default-opts)))
(comment (stop-server))
