(ns goose.brokers.redis.console.pages.spike1
  (:require [clojure.string :as str]))


(def components {})

(defn view []
  ())

;; :job-type :page
;; ?landing
;; Redis
#_{:heading   {}
   :home      {}
   :enqueued  {:jobs {}
               :job  {}}
   :dead      {:jobs {}
               :job  {}}
   :batch     {:job {}}
   :scheduled {:jobs {}
               :job  {}}
   :periodic  {:jobs {}
               :job  {}}}

;; Rabbitmq
#_{:theme  :light
   :header {:component header :args {:class        {:light "nav-light"
                                                    :dark  "nav-dark"}
                                     :header-links [{:route "/enqueued" :label "Enqueued"}]}}
   :home   {}
   :dead   {:job {:action-btn [:replay :pop :delete-queue]
                  :table      table}}}

;; How to use theme?
{:theme [:light :dark]}
;; Based on the :light or :dark? the classes would change?
;; Is one class enough? More class : can add a map

;; Based on one class? can the heading and footer change?
{:component "<>"
 :args      {:class "<>"}}


;; Check if header can take a single class to change the colors?

(defn ^:no-doc header
  [{:keys                  [theme]
    {{:keys [header-links]
      class :class} :args} :header} {:keys [app-name prefix-route uri] :or {app-name ""}
                                     :as   _data}]
  (let [css-class (get class theme)
        subroute? (fn [r] (str/includes? uri (prefix-route r)))
        short-app-name (if (> (count app-name) 20)
                         (str (subs app-name 0 17) "..")
                         app-name)]
    [:header
     [:nav {:class css-class}
      [:div.nav-start
       [:div.goose-logo
        [:a {:href (prefix-route "/")}
         [:img {:src (prefix-route "/img/goose-logo.png") :alt "goose-logo"}]]]
       [:div#menu
        [:a {:href (prefix-route "/") :class "app-name"} short-app-name]
        (for [{:keys [route label]} header-links]
          [:a {:href  (prefix-route route)
               :class (when (subroute? route) "highlight")} label])]]]]))

;; How to pass the entire structure?
;; Should we pass args specific to fun or entire structure


(header components {:app-name     "Goose console"
                    :prefix-route str
                    :uri          "/goose/enqueued"})
(def components {:theme    :light
                 :header   {:component header :args {:class        {:light "nav-light"
                                                                    :dark  "nav-dark"}
                                                     :header-links [{:route "/enqueued" :label "Enqueued"}]}}
                 :enqueued {:jobs {:component "jobs-page"
                                   :args      {:heading    "Enqueued Jobs"
                                               :class      "redis-exnqueued"
                                               :sidebar    {:component "sidebar"}
                                               :filter     {:component "sticky-header" :args {:filter-types ["id" "execute-fn-sym" "type"]}}
                                               :pagination {:component "pagination"}
                                               :table      {:component "jobs-table" :args {}}
                                               :action-btn {:component "[prioritise-btn, delete-btn]"}
                                               :purge      {:component "purge"}}}
                            :job  {:component "job-page"
                                   :args      {:heading    "Enqueued Job"
                                               :class      ""
                                               :table      {:component "job-page"}
                                               :action-btn {:component "[prioritise-btn, delete-btn]"}}}}})

{:theme         :light
 :header        {:component header :args {:class        {:a {:light "nav-light"
                                                             :dark  "nav-dark"}}
                                          :header-links [{:route "/enqueued" :label "Enqueued"}]}}
 :enqueued-jobs {:component "jobs-page"
                 :args      {:heading    "Enqueued Jobs"
                             :class      "redis-enqueued"
                             :sidebar    {:component "sidebar"}
                             :filter     {:component "sticky-header"}
                             :pagination {:component "pagination"}
                             :table      {:component "jobs-table" :args {}}
                             :action-btn {:component "[prioritise-btn, delete-btn]"}
                             :purge      {:component "purge"}}}
 :enqueued-job  {:component "jobs-page"
                 :args      {:heading    "Enqueued Job"
                             :class      ""
                             :table      {:component "job-page"}
                             :action-btn {:component "[prioritise-btn, delete-btn]"}}}}



