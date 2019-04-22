(ns antenora.data
  (:require [clojure.java.io :as io]))

(def blocksize 32768)

(def datastore (str (System/getenv "HOME") "/.antenora/datastore/"))

(defn init-datastore
  []
  (if (.exists (io/file datastore))
    false
    (io/make-parents (str datastore "."))))