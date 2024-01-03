(ns goose.console.server
  (:require [ring.adapter.jetty :as jetty]
    [ring.util.response :as response]
    [ring.middleware.content-type :refer [wrap-content-type]]))

(defonce server (atom nil))

(defn handler [_request]
      (-> (response/file-response "src/goose/console/landing_page.html")))

(defn start-server []
  (reset! server (jetty/run-jetty handler {:port  3001
                                           :join? false})))

(defn stop-server []
  (when-let [s @server]
    (.stop s)
    (reset! server nil)
    (println "Server stopped")))

(comment (start-server))
(comment (stop-server))

