(ns antenora.core
  (:require [ring.middleware.cookies :refer :all]
            [ring.middleware.content-type :refer :all]
            [ring.middleware.params :refer :all]
            [ring.middleware.keyword-params :refer :all]
            [ring.middleware.nested-params :refer :all]
            [ring.middleware.session :refer :all]
            [ring.util.response :refer :all]
            [ring.middleware.multipart-params :refer :all]
            [ring.middleware.reload :as reload]
            [manifold.stream :as s]
            [manifold.deferred :as d]
            [manifold.bus :as bus]
            [compojure.core :refer :all]
            [buddy.core.codecs :refer :all]
            [compojure.route :as r]
            [aleph.http :as http]
            [aleph.tcp :as tcp]
            [clojure.core.async :as async]
            [clojure.tools.logging :as log]
            [byte-streams :as bs]
            [msgpack.core :as msg]
            [clojure.pprint :refer :all]
            [byte-streams :as bs]
            [octet.core :as o]
            [antenora.vfs :as vfs]
            [msgpack.clojure-extensions :refer :all]
            [phlegyas.core :as phlegyas])
  (:gen-class))

;; (def name-resolver (aleph.netty/dns-resolver-group {:name-servers '("127.0.0.1:5350")}))

;; (defn core-route
;;   [req]
;;   (d/let-flow [net (d/catch
;;                        (http/websocket-connection req)
;;                        (fn [_] nil))]
;;                (if net
;;                  (let [in (s/stream)
;;                        out (s/stream)
;;                        ninep-server (phlegyas/server! in out :root-filesystem-constructor #'antenora.vfs/filesystem!)]
;;                    (s/connect net in)
;;                    (s/connect out net))))
;;   nil)

;; (defn ws-connect
;;   []
;;   @(http/websocket-client "ws://localhost:34567/ws"))

;; (defn tor-ws-connect
;;   [uri]
;;   @(http/websocket-client uri {:name-resolver name-resolver
;;                                :proxy-options {:host "127.0.0.1" :port 9080 :tunnel? true}}))

(defroutes core
  (GET "/" [] "Antenora.")
  ;; (GET "/ws" [] core-route)
  (r/not-found "404"))

(def entrypoint
  (-> (reload/wrap-reload #'core)
      (wrap-keyword-params)
      (wrap-cookies)
      (wrap-params)
      (wrap-multipart-params)
      (wrap-session)
      (wrap-nested-params)))

(defn start-server
  [join?]
  (http/start-server entrypoint {:host "localhost" :port 34567 :join? join?}))

(defn tcp-route
  [s info]
  (let [in (s/stream)
        out (s/stream)
        ninep-server (phlegyas/server! in out :root-filesystem-constructor #'vfs/control-plane)]
    (s/connect s in)
    (s/connect out s)))

(defn start-tcp-server
  []
  (tcp/start-server tcp-route {:port 34568 :join? false}))

(defn -main
  [& args]
  (log/info "Starting server.")
  (start-server true))
