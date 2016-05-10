(ns omnicognate.core
  (:require [taoensso.truss :as truss :refer [have have! have?]]))

(defprotocol TxLog
  (-commit [log tx tempids])
  (-rebase [log tx tempids])
  (-head [log])
  (-heads [log]))

(defn tx-log? [x]
  (satisfies? TxLog x))

(defn commit [log tx tempids]
  (-commit (have tx-log? log)
           (have sequential? tx)
           (have map? tempids)))

(defn rebase [log tx tempids]
  (-rebase (have tx-log? log)
           (have sequential? tx)
           (have map? tempids)))

(defn head [log]
  (-head (have tx-log? log)))

(defn heads [log]
  (-heads (have tx-log? log)))

(defrecord DatomTxLog [log refs]
  TxLog
  (-commit [self tx tempids]
    self)
  (-rebase [self tx tempids]
    self)
  (-head [self]
    nil)
  (-heads [self]
    nil))

(defn datom-tx-log [refs]
  (->DatomTxLog [] (have set? refs)))
