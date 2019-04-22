(ns antenora.db
  (:require [clojure.java.jdbc :refer :all]
            [msgpack.clojure-extensions :refer :all]
            [msgpack.core :as msg]
            [caesium.util :as u]
            [clojure.core.async :as async :refer [<!!]]
            [byte-streams :as bs]
            [clojure.core.async :as async]
            [antenora.util :refer :all]
            [clojure.tools.logging :as log]))

(def db-path "/home/dsp/.antenora/antenora.sqlite")

;; maybe separate block storage into a separate sqlite?
(def db
  {:classname   "org.sqlite.JDBC"
   :subprotocol "sqlite"
   :subname     db-path})

(def tx-chan (async/chan))
(def txp-chan (async/chan))

(defn persist-transactions
  [transactions]
  (with-db-transaction [con db]
    (loop [txs transactions]
      (if (empty? txs)
        true
        (do
          (insert! con :blocks (peek txs))
          (recur (pop txs)))))))

(defn flush-transactions
  []
  (async/>!! tx-chan false))

(defn insert-to-transaction-queue
  [transaction]
  (async/>!! tx-chan transaction))

(def max-tx-batch 499)

(def transaction-persister (async/thread
                             (while true
                               (let [transactions (async/<!! txp-chan)]
                                 (persist-transactions transactions)))))

(def transaction-handler (async/thread
                           (while true
                             (loop [tx 0 queue clojure.lang.PersistentQueue/EMPTY flush false]
                               (if (or (= tx max-tx-batch) (true? flush))
                                 (async/>!! txp-chan queue)
                                 (let [transaction (async/<!! tx-chan)
                                       flush (if (empty? transaction) true false)]
                                   (recur (+ 1 tx) (conj queue transaction) flush)))))))
(defn block-in-store?
  [hash]
  (not-empty (query db ["SELECT hash FROM blocks WHERE hash = ?" hash])))

(defn persist-block
  [block]
  (let [block-hash (hash-block block)]
    (insert-to-transaction-queue {:hash block-hash :block block})
    block-hash))

(defn persist-header-keys
  [hash nonce key public-signing-key secret-signing-key]
  (insert! db :header_keys
           {:hash (msg/pack hash)
            :nonce (msg/pack nonce)
            :key (msg/pack key)
            :spk (msg/pack (bs/to-byte-array public-signing-key))
            :ssk (msg/pack (bs/to-byte-array secret-signing-key))}))

(defn get-block
  [hash]
  (let [block (-> (query db ["SELECT block FROM blocks WHERE hash = ?" hash]) first :block)]
    (if block
      block
      nil)))

(defn persist-key
  [keypair type]
  (let [public (bs/to-byte-array (:public keypair))
        secret (bs/to-byte-array (:secret keypair))
        name (u/hexify public)
        id (uuid!)]
    (insert! db :key_bundles
             {:public public :secret secret :name name :id id :usecount 0 :type type :associated "master"})
    name))

(defn get-identity-key
  [type]
  (type (first (query db ["SELECT public, secret FROM key_bundles WHERE type = 'identity' AND name = 'master'"]))))

(defn node-in-db?
  [node]
  (let [row (first (query db ["SELECT id from nodelist WHERE id = ?" node]))]
    (if row
      true
      false)))
