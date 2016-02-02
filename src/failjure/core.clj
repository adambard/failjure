(ns failjure.core
  (:require [clojure.algo.monads :refer [domonad defmonad]]))

; Public API
; failed?, message part of prototype
(declare fail)
(declare attempt-all)
(declare if-failed)
(declare attempt->)
(declare attempt->>)


; Define a failure
(defrecord Failure [message])
(defn fail
 ([msg] (->Failure msg))
 ([msg & fmt-parts]
  (->Failure (apply format msg fmt-parts))))


; Exceptions are failures too, make them easier
(defmacro try* [& body]
  `(try
     ~@body
     (catch Exception e# e#)))


(defprotocol HasFailed
  (failed? [self])
  (message [self]))


(extend-protocol HasFailed
  Object
  (failed? [self] false)
  (message [self] (str self))

  Failure
  (failed? [self] true)
  (message [self] (:message self))

  Exception
  (failed? [self] true)
  (message [self] (.getMessage self)))


(defmonad error-m
  [m-result identity
   m-bind   (fn [m f] (if (failed? m)
                       m
                       (f m)))])


(defmacro if-failed [arglist & body]
  `(with-meta (fn ~arglist ~@body)
              {:else-fn? true}))


(defn else* [else-part result]
  (if (:else-fn? (meta else-part))
    (else-part result)
    else-part))


(defmacro attempt-all
  ([bindings return] `(domonad error-m ~bindings ~return))
  ([bindings return else]
   `(let [result# (attempt-all ~bindings ~return)]
      (if (failed? result#)
        (else* ~else result#)
        result#))))


(defmacro attempt->
  ([start] start)
  ([start form] `(domonad error-m [x# (-> ~start ~form)] x#))
  ([start form & forms]
   `(let [new-start# (attempt-> ~start ~form)]
      (if (failed? new-start#)
        new-start#
        (attempt-> new-start# ~@forms)))))


(defmacro attempt->>
  ([start] start)
  ([start form] `(domonad error-m [x# (->> ~start ~form)] x#))
  ([start form & forms]
   `(let [new-start# (attempt->> ~start ~form)]
      (if (failed? new-start#)
        new-start#
        (attempt->> new-start# ~@forms)))))




(comment
  (use 'clojure.test)

  (testing "Failjure core functions"
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
      (is (= "Caught")
          (try
            (attempt-all [x "O"
                          y (Integer/parseInt "k")]
                         "Ok"
                         "Failed")
            (catch Exception e "Caught"))))

    ; Test attempt->
    (testing "attempt->"
      (is (= "Ok!")
          (attempt->
            ""
            (str "O")
            (str "k")
            (str "!"))) 

      (is (= (fail "Not OK!")
             (attempt->
               ""
               (str "Not OK!")
               (fail)
               (str "kay-O!")
               (reverse)))))

    ; Test attempt->>
    (testing "attempt->>"
      (is (= "Ok")
          (attempt->
            ""
            (str "k")
            (str "O"))) 

      (is (= (fail "Not OK!")
             (attempt->
               ""
               (str "Not OK!")
               (fail)
               (str "O")
               (reverse)))))))
