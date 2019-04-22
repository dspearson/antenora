(ns antenora.http
  (:require [aleph.http :as http]
            [byte-streams :as bs]))

(def http-connection-pool
  (http/connection-pool
   {:dns-options
    {:name-servers '("127.0.0.1:5350")} ;; Tor DNS resolver
    :connection-options
    {:proxy-options
     {:host "127.0.0.1" ;; Tor HTTP CONNECT proxy
      :port 9080
      :tunnel? true}}}))

(defn get-wrapper
  [node endpoint]
  (try
    (->
     @(http/get (str "http://" node endpoint) {:pool http-connection-pool})
     :body
     bs/to-string)
    (catch Exception e nil)))