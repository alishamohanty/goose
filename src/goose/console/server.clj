(ns goose.console.server
  (:require [ring.adapter.jetty :as jetty]
            [ring.middleware.file :refer [wrap-file]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.util.response :refer [resource-response content-type] :as response]))

(defonce server (atom nil))

(defn routes [{:keys [uri]}]
  (if (= uri "/")
    (some-> (resource-response "index.html" {:root "public"})
            (content-type "text/html; charset=utf-8"))
    (response/response "<h1>Not Found</h1>")))

(def handler
  (-> routes
      (wrap-resource "public")
      (wrap-file "resources/public")))

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
