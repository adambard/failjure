(defproject failjure "2.2.0"
  :description "Simple helpers for treating failures as values"
  :url         "https://github.com/adambard/failjure"
  :license     {:name "Eclipse Public License"
                :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies []

  :repl-options {:init-ns failjure.core}

  :plugins [[lein-cljsbuild "1.1.8"]
            [lein-doo "0.1.10"]]

  :aot [failjure.core]

  :profiles
  {:provided {:dependencies [[org.clojure/clojure       "1.10.1"]
                             [org.clojure/clojurescript "1.10.764"]]}}
  :cljsbuild
  {:builds [{:id "test"
             :source-paths ["src" "test"]
             :compiler {:output-to "target/testable.js"
                        :main failjure.runner
                        :target :nodejs
                        :optimizations :none}}]})
