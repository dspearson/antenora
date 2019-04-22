(ns antenora.crypto
  (:require [clojure.java.io :as io]
            [clojure.java.jdbc :refer :all]
            [crypto.random :refer [hex]]
            [buddy.hashers :as hashers]
            [buddy.core.codecs :refer :all]
            [buddy.core.crypto :as crypto]
            [buddy.core.hash :as h]
            [buddy.core.padding :as padding]
            [caesium.crypto.secretbox :as sb]
            [caesium.magicnonce.secretbox :as msb]
            [caesium.crypto.box :as box]
            [caesium.crypto.sign :as sign]
            [caesium.byte-bufs :as bu]
            [caesium.util :as u]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clj-time.coerce :as c]
            [byte-streams :as bs]
            [clojure.data.codec.base64 :as b64]
            [buddy.core.nonce :as nonce]
            [buddy.core.kdf :as kdf]
            [msgpack.clojure-extensions :refer :all]
            [msgpack.core :as msg]
            [antenora.db :refer :all]
            [antenora.data :as data]))

(defn encrypt-buffer
  [buf k]
  (msb/secretbox-det buf k))

(defn decrypt-block
  [block k]
  (msb/open block k))

(defn create-manifest-block
  [size manifest metadata & {:keys [file chk? k]}]
  (let [manifest-msgpack (msg/pack {:size size :blocks manifest})
        manifest-block (-> manifest-msgpack (encrypt-buffer k) persist-block)]
    (flush-transactions)
    {:block manifest-block :key (bytes->hex k) :size size}))

(defn encrypt-file
  [file & {:keys [chk? fh signing-keypair metadata k]}]
  (with-open [fh (io/input-stream file)]
    (let [file-hash (h/sha256 file)
          k (or k file-hash)
          file-size (.length (io/file file))
          buf (byte-array (- data/blocksize 16 24))] ; 16 bytes MAC, 24 byte nonce
      (loop [manifest []]
        (let [n (.read fh buf)]
          (if (= n -1)
            (create-manifest-block file-size manifest metadata :chk? chk? :k k)
            (recur (conj manifest [n (-> buf (encrypt-buffer k) persist-block)]))))))))

(defn generate-keypair
  "Generate a new ephemeral public/secret keypair for box encryption."
  []
  (let [keypair (box/keypair!)]
    (persist-key keypair "ephemeral")))

(defn generate-signing-keypair
  []
  (let [keypair (sign/keypair!)]
    (persist-key keypair "signature")))

(defn write-bytes
  [fh data bytes]
  (.write fh data 0 bytes)
  bytes)

(defn write-block
  [fh k block size]
  (let [block-data (-> block get-block (decrypt-block k))]
    (if block-data
      (write-bytes fh block-data size))))

(defn assemble-file
  [manifest k target]
  (with-open [fh (io/output-stream target)]
    (loop [bytes 0 blocks (:blocks manifest)]
      (let [[size block] (first blocks)]
        (if (or (nil? block) (= bytes size manifest))
          true
          (recur (+ bytes (write-block fh k block size)) (rest blocks)))))))

(defn decrypt-file
  [block hk target]
  (let [k (u/unhexify hk)
        manifest (get-block block k)]
    (if (nil? manifest)
      nil
      (assemble-file (msg/unpack manifest) k target))))

(defn encrypt-message
  "Take in a message, and pack/encrypt."
  [message recipient]
  (try
    (let [k (u/unhexify recipient)
          buf (msg/pack message)
          block (box/anonymous-encrypt k buf)]
      (-> block persist-block))
    (catch Exception e nil)))