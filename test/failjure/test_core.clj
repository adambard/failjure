(ns failjure.test-core
  (:require [clojure.test :refer :all]
            [failjure.core :refer :all]))

(deftest failjure-core-test
    (testing "attempt-all"
      (testing "basically works"
        (is (= "Ok"
               (attempt-all [x "O"
                             y "k"]
                            (str x y)))))

      ; Fails
      (testing "Returns/short-circuits on failures"
        (is (= (fail "k")
               (attempt-all [x "O"
                             y (fail "k")
                             _ (is false "Did not short-circuit")
                             ]
                            (str x y)))))

      (testing "Returns the exception for try*"
        (let [result (attempt-all [x "O"
                            y (try* (Integer/parseInt "k"))
                            z (is false "Did not short-circuit")]
                           (str x y z))]
          (is (failed? result))
          (is (instance? NumberFormatException result))))

      (testing "Runs when-failed"
        ; Runs when-failed
        (is (= "Fail"
               (attempt-all [x "O"
                             y (fail "Fail")
                             z "!"]
                            (str x y z)
                            (when-failed [e]
                                         (message e))))))

      (testing "Runs if-failed (which is DEPRECATED but still supported)"
        ; Runs if-failed
        (is (= "Fail"
               (attempt-all [x "O"
                             y (fail "Fail")
                             z "!"]
                            (str x y z)
                            (if-failed [e]
                                         (message e))))))

      (testing "NumberFormatException"
        (is (= "Caught"
               (try
                 (attempt-all [x "O"
                               y (Integer/parseInt "k")]
                              "Ok"
                              "Failed")
                 (catch NumberFormatException e "Caught"))))))

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
               (reverse)))))
    
    (testing "failed?"
      (testing "failed? is valid on nullable"
        (is (false? (failed? nil)))
        (is (= "nil" (message nil))))

      (testing "failed? is valid on exception"
        (is (true? (failed? (Exception. "Failed"))))
        (is (= "My Message" (message (Exception. "My Message")))))

      (testing "failed? is valid on failure"
        (is (true? (failed? (fail "You failed."))))
        (is (= "You failed." (message (fail "You failed.")))))))
