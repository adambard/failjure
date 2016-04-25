(ns failjure.test-core
  (:require [clojure.test :refer :all]
            [failjure.core :refer :all]))

(deftest failjure-core-test
    (testing "attempt-all"
      (is (= "Ok"
             (attempt-all [x "O"
                           y "k"]
                          (str x y))))

      ; Fails
      (is (= (fail "k")
             (attempt-all [x "O"
                           y (fail "k")]
                          (str x y))))

      ; Returns the exception for try*
      (is (failed?
            (attempt-all [x "O"
                          y (try* (Integer/parseInt "k"))
                          z (fail "Another failure")]
                         (str x y z))))

      ; Runs if-failed
      (is (= "Fail"
             (attempt-all [x "O"
                           y (fail "Fail")
                           z "!"]
                          (str x y z)
                          (if-failed [e]
                                     (message e)))))

      ; Doesn't interrupt exceptions normally
      (is (= "Caught"
          (try
            (attempt-all [x "O"
                          y (Integer/parseInt "k")]
                         "Ok"
                         "Failed")
            (catch Exception e "Caught")))))

    ; Test attempt->
    (testing "attempt->"
      (is (= "Ok!"
          (attempt->
            ""
            (str "O")
            (str "k")
            (str "!")))) 

      (is (= (fail "Not OK!")
             (attempt->
               ""
               (str "Not OK!")
               (fail)
               (str "kay-O!")
               (reverse)))))

    ; Test attempt->>
    (testing "attempt->>"
      (is (= "Ok"
          (attempt->>
            ""
            (str "k")
            (str "O")))) 

      (is (= (fail "Not OK!")
             (attempt->>
               ""
               (str "Not OK!")
               (fail)
               (str "O")
               (reverse))))))
