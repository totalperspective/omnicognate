(defproject omnicognate "0.1.2"
  :description "Like git, but for datomic"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [#_[org.clojure/core.match "0.3.0-alpha4" :exclusions [org.clojure/clojure]]
                 [com.taoensso/truss "1.2.0" :exclusions [org.clojure/clojure]]]
  :deploy-repositories [["releases" :clojars]]
  :profiles {:dev
             {:dependencies [[org.clojure/clojure "1.8.0"]
                             [com.datomic/datomic-free "0.9.5359"]]}})
