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

    ; Test ok-> (and therefore attempt->)
    (testing "ok->"
      (is (= "Ok!"
          (ok->
            ""
            (str "O")
            (str "k")
            (str "!")))) 

      (is (= (fail "Not OK!")
             (ok->
               ""
               (str "Not OK!")
               (fail)
               (str "kay-O!")
               (reverse)))))

    ; Test attempt->>
    (testing "attempt->>"
      (is (= "Ok"
          (ok->>
            ""
            (str "k")
            (str "O")))) 

      (is (= (fail "Not OK!")
             (ok->>
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
        (is (= "You failed." (message (fail "You failed."))))))
    
    (testing "if-let-ok?"

      (is (= "Hello"
             (if-let-ok? [v "Hello"] v)))

      (is (failed?
            (if-let-ok? [v (fail "FAIL")] "OK")))

      (is (= "Hello"
             (if-let-ok? [v :ok] "Hello" "Goodbye")))

      (is (= "Goodbye"
             (if-let-ok? [v (fail "Hello")] "Hello" "Goodbye")))
      )

    (testing "when-let-ok?"
      (let [result (atom nil)]
        (is (= "Hello"
               (when-let-ok? [v "Hello"]
                 (reset! result :ok)
                 v)))
        (is (= :ok @result)))

      (let [result (atom nil)]
        (is (failed?
              (when-let-ok? [v (fail "FAIL")]
                (reset! result :ok)
                "OK")))
        (is (nil? @result))
        )
      )

    (testing "if-let-failed?"

      (is (= "Hello"
             (if-let-failed? [v "Hello"] "FAILED" v)))

      (is (failed?
            (if-let-failed? [v (fail "FAIL")] v)))

      (is (ok?
            (if-let-failed? [v "Didn't fail"] v)))

      (is (= "Goodbye"
             (if-let-failed? [v :ok] "Hello" "Goodbye")))

      (is (= "Hello"
             (if-let-failed? [v (fail "Hello")] "Hello" "Goodbye")))
      )

    (testing "when-let-failed?"
      (let [result (atom nil)]
        (is (= "Hello"
              (when-let-failed?
                [v "Hello"]
                (reset! result :ok)
                v)))
        (is (nil? @result)))

      (let [result (atom nil)]
        (is (= "OK"
              (when-let-failed?
                [v (fail "FAIL")]
                (reset! result :ok)
                "OK")))
        (is (= :ok @result))))

  (testing "Assertions"
    ;; assert-some? basically covers everything
    (testing "Assert some?"
      (is (= (fail "msg") (assert-some? nil "msg")))
      (is (= "it" (assert-some? "it" "msg"))))))




(comment
  (run-tests)
  )
