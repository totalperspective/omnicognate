(ns omnicognate.core-test
  (:require [clojure.test :refer :all]
            [omnicognate.core :refer :all]
            [datomic.api :as d]))

(def empty-log (datom-tx-log #{:in :employs}))

(def log-data
  [{:tx [[-1 :name "Authur"]]
    :ids {-1 1}}
   {:tx [[-1 :name "Ford"]
         [-2 :name "Zaphod"]]
    :ids {-1 2
          -2 3}}
   {:tx [[-1 :ship "Heart of Gold"]
         [3 :in -1]]
    :ids {-1 4}}
   {:tx [[-1 :org "Megadodo Publications"]
         [-1 :employs 2]]
    :ids {-1 5}}])

(def log
  (reduce (fn [log {:keys [tx ids]}]
            (commit log tx ids))
          empty-log
          log-data))

(deftest empty-log
  (testing "There is nothing at the head"
    (is (= nil (head empty-log)))
    (is (= nil (heads empty-log)))))

(deftest log-head
  (testing "The head is the first commit"
    (is (= (get-in log-data [0 :tx]) (head log))))
  (testing "Both the first ans second commits are at the head"
    (let [head-txes (into #{} (map :tx (take 2 log-data)))]
      (is (= head-txes (heads log))))))

;; First create a db and add some data
;; Then stage some commits onto it
;; Next push the commits onto the db
