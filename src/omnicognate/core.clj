(ns omnicognate.core
  (:require [taoensso.truss :as truss :refer [have have! have?]]
            [clojure.set :as set]))

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

(defn inverse-attr [a]
  (let [ns (namespace a)
        n (apply str "_" (name a))]
    (keyword ns n)))

(defn references [refs tx]
  (into #{}
        (mapcat (fn [datom]
                  (cond
                    (sequential? datom)
                    (let [[op e a v] datom]
                      (if (refs a)
                        [e v]
                        [e]))

                    (map? datom)
                    (keep (fn [[k v]]
                            (when (refs k) v))
                          datom)))
                tx)))

(defn tx-ready? [{:keys [tempids deps]}]
  (empty? deps))

(defn ready-txes [tx-info log]
  (filter (fn [tx]
            (tx-ready? (tx-info tx)))
          log))

(defn update-ids [tx old-id->new-id refs]
  (mapv (fn [datom]
          (cond
            (sequential? datom)
            (let [[op e a v] datom
                  e (or (old-id->new-id e) e)
                  v (if (refs a)
                      (or (old-id->new-id v) v)
                      v)]
              [op e a v])

            (map? datom)
            (into {} (map (fn [[k v]]
                            [k (if (refs k)
                                 (or (old-id->new-id v) v)
                                 v)])
                          datom))))
        tx))

(defrecord DatomTxLog [refs log tx-info ids]
  TxLog
  (-commit [self tx tempids]
    (let [deps (set/intersection ids (references refs tx))]
      (-> self
          (update :log conj tx)
          (update :tx-info assoc tx {:tempids tempids :deps deps})
          (update :ids into (vals tempids)))))
  (-rebase [self tx tempids]
    (if-let [tx-info' (tx-info tx)]
      (let [tempids' (:tempids tx-info')
            new-log (remove #{tx} log)
            old-ids (set (vals tempids'))
            new-ids (set (vals tempids))
            old-id->new-id (into {} (keep (fn [[tempid new-id]]
                                            (when-let [old-id (tempids' tempid)]
                                              [old-id new-id]))
                                          tempids))
            new-tx-info (into {} (map (fn [tx']
                                        (let [info (update (tx-info tx')
                                                           :deps
                                                           set/difference
                                                           old-ids)
                                              tx' (update-ids tx' old-id->new-id refs)]
                                          [tx' info]))
                                      new-log))]
        (-> self
            (update :ids set/difference old-ids)
            (update :ids set/union new-ids)
            (assoc :tx-info new-tx-info)
            (assoc :log (keys new-tx-info))))
      self))
  (-head [self]
    (first (ready-txes tx-info log)))
  (-heads [self]
    (into #{} (ready-txes tx-info log))))

(defn datom-tx-log [refs]
  (let [inverse (map inverse-attr (have set? refs))
        refs (into refs inverse)]
    (->DatomTxLog refs
                  []
                  {}
                  #{})))
