(defproject clj-seh-challenge "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [aysylu/loom "1.0.2"]
                 [cheshire "5.11.0"]
                 [com.taoensso/timbre "6.1.0"]]

  :plugins [[lein-ring "0.12.6"]
            [lein-vanity "0.2.0"]
            [lein-nomis-ns-graph "0.14.6"]
            [lein-ancient "0.7.0"]
            [jonase/eastwood "1.4.0"]
            [lein-kibit "0.1.8"]
            [lein-bikeshed "0.5.2"]
            [venantius/yagni "0.1.7"]
            [lein-check-namespace-decls "1.0.4"]
            [docstring-checker "1.1.0"]]

  :main ^:skip-aot clj-seh-challenge.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
