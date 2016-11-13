(ns next-train.nodehttp
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.nodejs :as nodejs]
            [cemerick.url :refer (url url-encode)]
            [cljs.core.async :as async]))


(def http (nodejs/require "http"))


(defn build-request-options [target method]
  "Creates the js object used as options the the node/http.request"
  (clj->js {:host (:host target)
            :path (:path target)
            :method method}))


(defn handle-response [res cb out]
  "Collect the entire response and then perform the specified callback"
  (let [collected-body (atom "")
        status (.-statusCode res)]
    (if (= status 200)
      (-> res
          (.on "data" (fn [chunk] (swap! collected-body (fn [old] (str old chunk)))))
          (.on "end" (fn [] (cb @collected-body))))
      (do
        (println (str "Error: " status))
        (async/close! out)))))


(defn get [target]
  "Get takes a path that must start with http:// and a callback function"
  (let [out (async/chan)]
    (http.get
     (build-request-options (url target) "GET")
     #(handle-response % (fn [x] (async/put! out x)) out))
    out))


(defn post [target body]
  "Post takes a path that must start with http:// and a callback function"
  (let [out (async/chan)]
    (doto (http.request
           (build-request-options (url target) "POST")
           #(handle-response % (fn [x] (async/put! out x)) out))
      (.write body)
      (.end))
    out))
