(ns next-train.nodehttp
  (:require [cljs.nodejs :as nodejs]
            [cemerick.url :refer (url url-encode)]))


(def http (nodejs/require "http"))


(defn build-request-options [target method]
  "Creates the js object used as options the the node/http.request"
  #js {:host (:host target)
       :path (:path target)
       :method method})


(defn handle-response [res cb]
  "Collect the entire response and then perform the specified callback"
  (let [collected-body (atom "")
        status (.-statusCode res)]
    (if (= status 200)
      (-> res
          (.on "data" (fn [chunk] (swap! collected-body (fn [old] (str old chunk)))))
          (.on "end" (fn [] (cb @collected-body))))
      (println (str "Error: " status)))))


(defn get [target cb]
  "Get takes a path that must start with http:// and a callback function"
  (http.get
   (build-request-options (url target) "GET")
   #(handle-response % cb)))


(defn post [target body cb]
  "Post takes a path that must start with http:// and a callback function"
  (doto (http.request
         (build-request-options (url target) "POST")
         #(handle-response % cb))
    (.write body)
    (.end)))
