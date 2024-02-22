(ns goose.console
  (:require [goose.broker :as b]))

(defn- handler [{{:keys [broker]} :client-opts :as req}]
  (b/handler broker req))

(defn app-handler [client-opts req]
  (handler (assoc req :client-opts client-opts)))