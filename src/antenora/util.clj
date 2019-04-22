(ns antenora.util
  (:require  [manifold.deferred :as d]
             [byte-streams :as bs]
             [manifold.stream :as s]
             [buddy.core.codecs :refer :all]
             [clojure.pprint :refer :all]
             [buddy.core.hash :as h]
             [clojure.java.io :as io]
             [clj-time.core :as t]
             [clj-time.format :as f]
             [clj-time.coerce :as c]
             [clojure.data.codec.base64 :refer :all]
             [clojure.tools.logging :as log])
  (:gen-class))

(defn parse-int [s]
  (Integer. (re-find  #"\d+" s)))

(defn uuid!
  []
  (str (java.util.UUID/randomUUID)))

(defn hash-file
  [file & {:keys [hash] :or {hash nil}}]
  (if (nil? hash)
    (-> file io/file io/input-stream h/sha256 bytes->hex)
    hash))

(defn hash-block
   [block]
   (-> block h/sha256 bytes->hex))

;; (defn session-janitor
;;   []
;;   (let [result (delete! db/db :sessions ["expiry_time < ?" (t/now)])]
;;     (println (str (t/now)) "Deleted sessions from database:" (first result))))

;; (defn periodically
;;   "Calls fn every millis. Returns a function that stops the loop."
;;   [fn millis]
;;   (let [p (promise)]
;;     (future
;;       (while
;;           (= (deref p millis "timeout") "timeout")
;;         (fn)))
;;     #(deliver p "cancel")))

(defn slurp-bytes
  "Slurp the bytes from a slurpable thing"
  [x]
  (with-open [out (java.io.ByteArrayOutputStream.)]
    (io/copy (io/input-stream x) out)
    (.toByteArray out)))

(defn b64
  [in]
  (-> (encode in)
      (bs/convert String)))

(defn epoch
  []
  (c/to-long (t/now)))
