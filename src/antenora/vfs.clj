(ns antenora.vfs
  (:require [phlegyas.types :refer :all]
            [phlegyas.vfs :as pvfs]
            [phlegyas.state :as ps]))

(defn server-control
  [stat frame state]
  (with-bindings
    (println offset)))

(defn filesystem!
  []
  (let [root-fs (pvfs/create-filesystem)
        root-path (:root-path root-fs)]
    (-> root-fs
        (pvfs/insert-file! root-path (pvfs/create-synthetic-file "control" #'server-control)))))

(def state-handlers ((fn [] (into {} (for [[k v] frame-byte] [k (or (-> k name symbol resolve)
                                                                   (-> (str "ps/" (name k)) symbol resolve))])))))
