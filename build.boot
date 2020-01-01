(set-env!
  :source-paths #{"src" "test"}
  :dependencies '[[org.clojure/clojure "1.8.0" :scope "provided"]
                  [org.clojure/clojurescript "1.10.520" :scope "provided"]
                  [adzerk/bootlaces "0.1.13" :scope "test"]
                  [adzerk/boot-test "1.2.0" :scope "test"]
                  [adzerk/boot-cljs "2.1.5"   :scope "test"]
                  [crisptrutski/boot-cljs-test "0.3.5-SNAPSHOT" :scope "test"]])


(require '[adzerk.bootlaces :refer :all]
         '[adzerk.boot-test :refer :all]
         '[crisptrutski.boot-cljs-test :refer [test-cljs]])

(def +version+ "1.6.0-SNAPSHOT")
(bootlaces! +version+)

(task-options!
  pom {:project        'failjure
       :version        +version+
       :description    "Simple helpers for treating failures as values"
       :url            "https://github.com/adambard/failjure"
       :scm            {:url "https://github.com/adambard/failjure"}
       :license        {"Eclipse Public License" "http://www.eclipse.org/legal/epl-v10.html"}})

(deftask build []
  (comp
    (build-jar)
    (target "build")))

(deftask deploy-clojars []
  (comp
    (build-jar)
    (push-release)))

(deftask cljs-tests []
  (test-cljs
   :namespaces ["failjure.test-core"]
   :js-env :node
   :exit? true))

(deftask all-tests []
  (comp
   (test)
   (cljs-tests)))
