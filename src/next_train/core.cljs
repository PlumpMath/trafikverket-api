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

(def result-limit 1)


(defn print-it [data] (println data))


(defn build-xml []
  (xml (clj->js {:REQUEST
                 [{:LOGIN
                   [{:_attr {:authenticationkey api-key}}]}
                  {:QUERY
                   [{:_attr {:objecttype "TrainAnnouncement"
                             :limit result-limit}}
                    {:FILTER
                     [{:EQ
                       [{:_attr {:name "LocationSignature"
                                 :value "Gm"}}]}]}
                    {:INCLUDE "Prognisticated"}
                    {:INCLUDE "AdvertisedLocationName"}
                    {:INCLUDE "LocationSignature"}
                    {:INCLUDE "AdvertisedTimeAtLocation"}
                    {:INCLUDE "ToLocation"}
                    {:INCLUDE "TrackAtLocation"}]}]})))


(defn -main []
  (println api-key)
  (go
    (println (async/<!
              (http/post url (build-xml))))))


(set! *main-cli-fn* -main)


(comment

  (-main)

  (build-xml)

  (go
    (println (async/<!
              (http/post url (build-xml)))))

    (go
      (println (async/<!
                (http/get "http://www.aftonbladet.se"))))

  )
