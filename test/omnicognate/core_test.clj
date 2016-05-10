(ns omnicognate.core-test
  (:require [clojure.test :refer :all]
            [omnicognate.core :refer :all]
            [datomic.api :as d]))

(def empty-log (datom-tx-log #{:in :employs}))

(def authur
  {:tx [[-1 :name "Authur"]]
   :ids {-1 1}})

(def ford-and-zaphod
  {:tx [[-1 :name "Ford"]
        [-2 :name "Zaphod"]]
   :ids {-1 2
         -2 3}})

(defn heart-of-gold [zaphod]
  {:tx [[-1 :ship "Heart of Gold"]
        [zaphod :in -1]]
   :ids {-1 4}})

(defn trillian [authur]
  {:tx [[-1 :name "Trillian"]
        [-1 :knows authur]]
   :ids {-1 5}})

(def log-data
  [authur
   ford-and-zaphod
   (heart-of-gold 3)
   (trillian 1)])

(def log
  (reduce (fn [log {:keys [tx ids]}]
            (commit log tx ids))
          empty-log
          log-data))

(deftest the-empty-log
  (testing "The empty log is a log"
    (is (tx-log? empty-log)))
  (testing "There is nothing at the head"
    (is (= nil (head empty-log))))
  (testing "The set of heads is also empty"
    (is (= nil (heads empty-log)))))

(deftest log-head
  (testing "The head is the first commit"
    (is (= (:tx authur) (head log))))
  (testing "Both the first ans second commits are at the head"
    (is (= #{(:tx authur)
             (:tx ford-and-zaphod)}
           (heads log)))))

(deftest rebase-authur
  (testing "Rebasing the first commit"
    (let [tx (:tx authur)
          new-log (rebase log tx {-1 100})]
      (is (= (:tx ford-and-zaphod) (head new-log)))
      (is (= #{(:tx ford-and-zaphod)
               (:tx (trillian 100))}
             (heads new-log))))))

(deftest rebase-authur-ford-and-zaphod
  (testing "Rebasing the first commit"
    (let [new-log (-> log
                      (rebase (:tx authur) {-1 100})
                      (rebase (:tx ford-and-zaphod) {-1 101 -2 102}))]
      (is (= (:tx ford-and-zaphod) (head new-log)))
      (is (= #{(heart-of-gold 102)
               (trillian 100)}
             (heads new-log))))))
