(ns goose.console.server
  (:require [goose.console.ui :as ui]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.file :refer [wrap-file]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.util.response :as response]))

(defonce server (atom nil))

(defn landing-page []
  (let [stats {:enqueued 231 :scheduled 5 :periodic 3 :dead 43}
        landing-page-html (ui/landing-page stats)]
    (response/response landing-page-html)))

(defn routes [{:keys [uri] :as req}]
  (if (= uri "/")
    (landing-page)
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
