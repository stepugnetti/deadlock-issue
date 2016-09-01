(defproject http-deadlock "0.1.0-SNAPSHOT"
  :description "A mimnimal example for http issue #305"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/core.async "0.2.385"]
                 [http-kit "2.2.0"]
                 [stylefruits/gniazdo "1.0.0"]]
  :main ^:skip-aot http-deadlock.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
