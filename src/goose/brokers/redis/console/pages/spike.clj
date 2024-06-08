(ns goose.brokers.redis.console.pages.spike
  (:require [clojure.math :as math]
            [clojure.string :as str]
            [goose.console :as console]
            [goose.defaults :as d]
            [hiccup.util :as hiccup-util]))
;;
;;(def page {:jobs jobs-page
;;           :job  job-page})

(defn jobs-page [{:keys [heading class sidebar filter pagination table action-btn purge]} {:keys [total-jobs] :as data}]
  [:div {:class class}
   [:h1 heading]
   [:div.content
    (when-let [{sidebar :component} sidebar]
      (sidebar data))
    [:div (when-let [{sticky-header                :component
                      {filter-types :filter-types} :args} filter]
            (sticky-header filter-types data))]
    [:div.pagination
     (when-let [{pagination :component} pagination]
       (when total-jobs
         (pagination data)))]
    [:div
     (when-let [{:keys      [args]
                 jobs-table :component} table]
       (jobs-table (merge args data {:action-btn (:component action-btn)})))]
    (when-let [{purge :component} purge]
      [:div.bottom
       (when (and total-jobs (> total-jobs 0))
         (purge data))])]])

(defn- sidebar [{:keys [prefix-route queues queue]}]
  [:div#sidebar
   [:h3 "Queues"]
   [:div.queue-list
    [:ul
     (for [q queues]
       [:a {:href  (prefix-route "/enqueued/queue/" q)
            :class (when (= q queue) "highlight")}
        [:li.queue-list-item q]])]]])

(defn sticky-header [filter-types {:keys                                    [base-path]
                                   {:keys [filter-type filter-value limit]} :params}]

  [:div.header
   [:form.filter-opts {:action base-path
                       :method "get"}
    [:div.filter-opts-items
     [:select {:name "filter-type" :class "filter-type"}
      (for [type filter-types]
        [:option {:value type :selected (= type filter-type)} type])]
     [:div.filter-values

      ;; filter-value element is dynamically changed in JavaScript based on filter-type
      ;; Any attribute update in field-value should be reflected in JavaScript file too

      (if (= filter-type "type")
        [:select {:name "filter-value" :class "filter-value"}
         (for [val ["unexecuted" "failed"]]
           [:option {:value val :selected (= val filter-value)} val])]
        [:input {:name  "filter-value" :type "text" :placeholder "filter value"
                 :class "filter-value" :value filter-value}])]]
    [:div.filter-opts-items
     [:span.limit "Limit"]
     [:input {:type  "number" :name "limit" :id "limit" :placeholder "custom limit"
              :value (if (str/blank? limit) d/limit limit)
              :min   "0"
              :max   "10000"}]]
    [:div.filter-opts-items
     [:button.btn.btn-cancel
      [:a. {:href base-path :class "cursor-default"} "Clear"]]
     [:button.btn {:type "submit"} "Apply"]]]])

(defn pagination-stats [first-page curr-page last-page]
  {:first-page first-page
   :prev-page  (dec curr-page)
   :curr-page  curr-page
   :next-page  (inc curr-page)
   :last-page  last-page})

(defn pagination [{:keys [page total-jobs base-path]}]
  (let [{:keys [first-page prev-page curr-page
                next-page last-page]} (pagination-stats d/page page
                                                        (int (math/ceil (/ total-jobs d/page-size))))
        page-uri (fn [p] (str base-path "?page=" p))
        hyperlink (fn [page label visible? disabled? & class]
                    (when visible?
                      [:a {:class (conj class (when disabled? "disabled"))
                           :href  (page-uri page)} label]))
        single-page? (<= total-jobs d/page-size)]
    [:div
     (hyperlink first-page (hiccup-util/escape-html "<<") (not single-page?) (= curr-page first-page))
     (hyperlink prev-page prev-page (> curr-page first-page) false)
     (hyperlink curr-page curr-page (not single-page?) true "highlight")
     (hyperlink next-page next-page (< curr-page last-page) false)
     (hyperlink last-page (hiccup-util/escape-html ">>") (not single-page?) (= curr-page last-page))]))

(defn format-arg [arg]
  (condp = (type arg)
    String (str "\"" arg "\"")
    nil "nil"
    Character (str "\\" arg)
    arg))

(defn delete-confirm-dialog [question]
  [:dialog {:class "delete-dialog"}
   [:div question]
   [:div.dialog-btns
    [:input.btn.btn-md.btn-cancel {:type "button" :value "cancel" :class "cancel"}]
    [:input.btn.btn-md.btn-danger {:type "submit" :name "_method" :value "delete"}]]])

(defn jobs-table [{:keys [action-btn base-path jobs]}]
  [:form {:action (str base-path "/jobs")
          :method "post"}
   [:div.actions
    (for [button action-btn]
      (button {:disabled true}))]
   [:table.jobs-table
    [:thead
     [:tr]]
    [:tbody
     (for [{:keys             [id queue execute-fn-sym args enqueued-at]
            {:keys [died-at]} :state
            :as               j} jobs]
       [:tr])]]])

(defn prioritise-btn [disabled]
  [:input.btn {:type "submit" :value "Prioritise" :disabled disabled}])

(defn delete-btn [disabled]
  (delete-confirm-dialog
    [:div "Are you sure you want to delete selected jobs in queue?"])
  [:input.btn.btn-danger
   {:type "button" :value "Delete" :class "delete-dialog-show" :disabled disabled}])

(defn purge-confirmation-dialog [{:keys [total-jobs base-path]}]
  [:dialog {:class "purge-dialog"}
   [:div "Are you sure, you want to remove " [:span.highlight total-jobs] " jobs ?"]
   [:form {:action base-path
           :method "post"
           :class  "dialog-btns"}
    [:input {:name "_method" :type "hidden" :value "delete"}]
    [:input {:type "button" :value "Cancel" :class "btn btn-md btn-cancel cancel"}]
    [:input {:type "submit" :value "Confirm" :class "btn btn-danger btn-md"}]]])

(defn purge [data]
  [:div.bottom
   (purge-confirmation-dialog data)
   [:button {:class "btn btn-danger btn-lg purge-dialog-show"} "Purge"]])

(def components {:header   [{:route "/enqueued" :label "Enqueued"}]
                 :enqueued {:jobs {:component jobs-page
                                   :args      {:heading    "Enqueued Jobs"
                                               :class      "redis-enqueued"
                                               :sidebar    {:component sidebar}
                                               :filter     {:component sticky-header :args {:filter-types ["id" "execute-fn-sym" "type"]}}
                                               :pagination {:component pagination}
                                               :table      {:component jobs-table :args {}}
                                               :action-btn {:component [prioritise-btn, delete-btn]}
                                               :purge      {:component purge}}}}
                 :dead     {:jobs {}
                            :job  {}}})
;; job-data || job-context || data || context

(defn view [job-type page-type]
  (let [header-links (get components :header)
        page-component (get-in components [job-type page-type :component])
        args (get-in components [job-type page-type :args])]
    (console/layout (partial console/header header-links) (partial page-component args))))

#_(view :enqueued :jobs (assoc data :base-path (prefix-route "/enqueued/queue/" queue)
                                    :params params
                                    :app-name app-name
                                    :prefix-route prefix-route))
