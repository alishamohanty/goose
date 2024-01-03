(ns goose.console.ui
  (:require [reagent.dom :as dom]))

(defn landing-page-html [stats-map]
      [:head
       [:meta {:charset "UTF-8"}]
       [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
       [:title "Goose Dashboard"]]
      [:body
       [:header
        [:nav
         [:div.nav-start
          [:div#logo "AppName"]
          [:div#menu
           [:a {:href "/enqueued"} "Enqueued"]
           [:a {:href "/scheduled"} "Scheduled"]
           [:a {:href "/periodic"} "Periodic"]
           [:a {:href "/batch"} "Batch"]
           [:a {:href "/dead"} "Dead"]]]
         [:div.nav-end
          [:label.toggle-switch
           [:input {:type "checkbox"}]
           [:span.switch-slider]]]]]
       [:main
        [:section.statistics
         (for [stat [{:id :enqueued :label "Enqueued"}
                     {:id :scheduled :label "Scheduled"}
                     {:id :periodic :label "Periodic"}
                     {:id :dead :label "Dead"}]]
              [:div.stat {:id (:id stat)}
               [:span.number (str (get stats-map (:id stat)))]
               [:span.label (:label stat)]])]]])

(defn landing-page []
      (landing-page-html {:enqueued 23 :scheduled 5 :periodic 3 :dead 43}))

(defn ^:export main []
      (dom/render [landing-page]
                  (.getElementById js/document "app")))
