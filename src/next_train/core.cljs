(ns next-train.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.nodejs :as nodejs]
            [next-train.nodehttp :as http]
            [cljs.core.async :as async]))

(nodejs/enable-util-print!)

(def xml (nodejs/require "xml"))

(def url "http://api.trafikinfo.trafikverket.se/v1.1/data.json")
(def api-key (-> nodejs/process
                     (.-env)
                     (.-TRAFIK_API_KEY)))

(def result-limit 10)


(defn build-xml []
  (xml (clj->js {:REQUEST
                 [{:LOGIN
                   [{:_attr {:authenticationkey api-key}}]}
                  {:QUERY
                   [{:_attr {:objecttype "TrainAnnouncement"
                             :limit result-limit}}
                    {:FILTER
                     [{:AND
                       [{:EQ
                         [{:_attr {:name "LocationSignature"
                                   :value "Gm"}}]}
                       {:EQ
                         [{:_attr {:name "ToLocation.LocationName"
                                   :value "Vö"}}]}
                        {:GT
                         [{:_attr {:name "AdvertisedTimeAtLocation"
                                   :value "$now"}}]}]}]}
                    {:INCLUDE "LocationSignature"}
                    {:INCLUDE "AdvertisedTimeAtLocation"}
                    {:INCLUDE "ToLocation"}
                    {:INCLUDE "AdvertisedTrainIdent"}]}]})))


(defn filter-body [body]
  (->>
   (-> (js->clj (JSON.parse body) :keywordize-keys true)
       :RESPONSE
       :RESULT
       first
       :TrainAnnouncement)
   (reduce #(if-not (contains? % (keyword (:AdvertisedTrainIdent %2)))
             (assoc % (keyword (:AdvertisedTrainIdent %2)) (:AdvertisedTimeAtLocation %2))
             %)
          {})))


(defn -main []
  (go (do (println "Departures:")
          (doseq [[train time] (filter-body (async/<! (http/post url (build-xml))))]
            (println
             (str "Train: " (name train) "\t"  (last (clojure.string/split time "T"))))))))


(set! *main-cli-fn* -main)
