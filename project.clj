(defproject antenora "0.0.1"
  :description "a free network"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/java.jdbc "0.7.8"]
                 [org.clojure/data.codec "0.1.1"]
                 [caesium "0.12.0"]
                 [clj-time "0.15.0"]
                 [byte-streams "0.2.4"]
                 [buddy/buddy-core "1.5.0"]
                 [buddy/buddy-hashers "1.3.0"]
                 ;; [aleph "0.4.6"]
                 [gloss "0.2.6"]
                 [funcool/octet "1.1.2"]
                 [org.clojure/tools.logging "0.4.1"]
                 [clojure-msgpack "1.2.1"]
                 [manifold "0.1.9-alpha3"]
                 [org.clojure/core.async "0.4.490"]
                 [compojure "1.6.1"]
                 [com.taoensso/nippy "2.13.0"]
                 [clojurewerkz/buffy "1.1.0"]
                 [primitive-math "0.1.6"]
                 [potemkin "0.4.5"]
                 [io.netty/netty-all "4.1.33.Final"]
                 [io.netty/netty-handler-proxy "4.1.33.Final"]
                 [org.xerial/sqlite-jdbc "3.25.2"]
                 [org.clojure/core.async "0.4.490"]
                 [clojure-msgpack "1.2.1"]
                 [caesium "0.10.0"]
                 [ring "1.7.1"]]
  :plugins [[lein-ring "0.12.4"]]
  :ring {:init antenora.core/ring-init
         :handler antenora.core/node-chain}
  :main ^:skip-aot antenora.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
