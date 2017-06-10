(set-env!
  :source-paths #{"src" "test"}
  :dependencies '[[org.clojure/clojure "1.7.0" :scope "provided"]
                  [org.clojure/algo.monads "0.1.6"]

                  [adzerk/bootlaces "0.1.13" :scope "test"]
                  [adzerk/boot-test "1.1.1" :scope "test"]])


(require '[adzerk.bootlaces :refer :all]
         '[adzerk.boot-test :refer :all])

(def +version+ "1.0.1")
(bootlaces! +version+)

(task-options!
  pom {:project        'failjure
       :version        +version+
       :description    "Simple helpers for treating failures as values"
       :url            "https://github.com/adambard/failjure"
       :scm            {:url "https://github.com/adambard/failjure"}
       :license        {"Eclipse Public License" "http://www.eclipse.org/legal/epl-v10.html"}})

(deftask deploy-clojars []
  (comp
    (build-jar)
    (push-release)))
