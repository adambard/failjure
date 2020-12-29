(ns failjure.core
  #?@(:cljs [(:require [goog.string :refer [format]]
                       [goog.string.format])]))

; Public API
; failed?, message part of protocol
(declare fail)
(declare attempt-all)
(declare if-failed)
(declare when-failed)
(declare attempt->)
(declare attempt->>)


(defprotocol HasFailed
  (failed? [self])
  (message [self]))


(extend-protocol HasFailed
  nil
  (failed? [self] false)
  (message [self] "nil")

  #?@(:clj [Object
            (failed? [self] false)
            (message [self] (str self))

            Exception
            (failed? [self] true)
            (message [self] (.getMessage self))]

      :cljs [default
             (failed? [self] false)
             (message [self] (str self))

             js/Error
             (failed? [self] true)
             (message [self] (.-message self))]))


(defn ok? [v] (not (failed? v)))


; Define a failure
(defrecord Failure [message]
  HasFailed
  (failed? [self] true)
  (message [self] (:message self)))


(defn fail
  ([msg] (->Failure msg))
  ([msg & fmt-parts]
   (->Failure (apply format msg fmt-parts))))

(defn attempt
  "Accepts an error-handling function and a (possibly failed) value"
  {:added "2.1"}
  [handle-fn val-or-failed]
  (if (failed? val-or-failed)
    (handle-fn val-or-failed)
    val-or-failed))

(defn try-fn [body-fn]
  (try
     (body-fn)
     (catch #?(:clj Exception :cljs :default) e# e#)))


; Exceptions are failures too, make them easier
(defmacro try* [& body]
  `(try-fn (fn [] ~@body)))


;; Validating binding macros
(defmacro if-let-ok?
  "Binding convenience.

  Acts just like let for non-failing values:

    (if-let-ok? [v (something-which-may-fail)]
      (do-something-else v)
      (do-something-on-failure v))

  Note that the value of v is the result of something-which-may-fail
  in either case. If no else branch is provided, nil is returned:

    (if-let-ok? [v (fail \"Goodbye\")]
      \"Hello\")
    ;; Returns #failjure.core.Failure{:message \"Goodbye\"}

  Note that the above is identical in function to simply calling
  (fail \"Goodbye\")"
  ([[v-sym form] ok-branch]
   `(let [result# ~form]
      (if-let-ok? [~v-sym result#] ~ok-branch result#)))
  ([[v-sym form] ok-branch failed-branch]
   `(let [result# ~form
          ~v-sym result#]
      (if (ok? result#)
        ~ok-branch
        ~failed-branch))))


(defmacro when-let-ok?
  "Analogous to if-let-ok? and when-let.

    (when-let-ok? [v (some-fn)]
      (prn \"WAS OK\")
      (do-something v))

  Returns the error in case of failure"
  [[v-sym form] & ok-branches]
  `(if-let-ok? [~v-sym ~form]
     (do ~@ok-branches)))

(defmacro if-let-failed?
  "Inverse of if-let-ok?

    (if-let-failed? [v (some-fn)]
      (prn \"V Failed!\")
      (prn \"V is OK!\"))

  If called with 1 branch, returns the value in case of non-failure:

    (if-let-failed? [v \"Hello\"]
      (prn \"V Failed!\"))  ;; => returns \"Hello\"
  "
  ([[v-sym form] failed-branch]
   `(if-let-ok? [~v-sym ~form] ~v-sym ~failed-branch))
  ([[v-sym form] failed-branch ok-branch]
   `(if-let-ok? [~v-sym ~form] ~ok-branch ~failed-branch)))

(defmacro when-let-failed?
  "Inverse of when-let-ok?

    (when-let-faild? [v (some-fn)]
      (prn \"FAILED\")
      (handle-failure v))

  Returns the value in case of non-failure"
  [[v-sym form] & failed-branches]
  `(if-let-failed? [~v-sym ~form] (do ~@failed-branches))
  )






(defmacro when-failed
  "Use in combination with `attempt-all`. If any binding in `attempt-all` failed,
   run the body given the failure/error as an argument.

  Usage:

  (attempt-all [_ (fail \"Failure\")]
    ; do something
    (when-failed [e]
      (print \"ERROR:\" (message e))))"
  {:added "0.1.3"}
  [arglist & body]
  `(with-meta (fn ~arglist ~@body)
     {:else-fn? true}))

(defmacro if-failed
  "DEPRECATED: Use when-failed instead"
  {:deprecated "0.1.3"}
  [arglist & body]
  `(with-meta (fn ~arglist ~@body)
     {:else-fn? true}))

(defn else* [else-part result]
  (if (:else-fn? (meta else-part))
    (else-part result)
    else-part))


(defn- attempt-all*
  "Rearrange the bindings into a pyramid of `if-let-failed?` calls"
  [bindings body]
  (->> bindings
       (partition 2)
       (reverse)
       (reduce (fn [inner [bind body]]
                 `(let [result# ~body]
                    (if-let-failed? [~bind result#]
                      result#
                      ~inner)))
               body)))

(defmacro attempt-all
  "Used like `let`, but short-circuits in case of
  a failed binding. Can be used in combination with when-failed
  to handle the failure.

  Unlike `let`, only accepts a single form to execute after the bindings.

    (attempt-all [x \"Ok\"
                  y (fail \"Fail\")]
      x
      (when-failed [e]
        (message e))) ; => \"Fail\"

  "
  ([bindings return]
   (attempt-all* bindings return))
  ([bindings return else]
   `(if-let-failed? [result# (attempt-all ~bindings ~return)]
      (else* ~else result#)
      result#)))

(defn- try-wrap
  [bindings]
  (map-indexed #(if (odd? %1)
                  `(try-fn (fn [] ~%2))
                  %2)
               bindings))

(defmacro try-all
  "Similar to `attempt-all` but catches possible exceptions.

  Wraps each arm of the binding in a `try*` to treat them as Failures and short circuit."
  ([bindings return]
   (attempt-all* (try-wrap bindings) return))
  ([bindings return else]
   `(if-let-failed? [result# (try-all ~bindings ~return)]
      (else* ~else result#)
      result#)))

(defmacro attempt->
  "Deprecated. Use ok-> instead."
  ([start] start)
  ([start form] `(-> ~start ~form))
  ([start form & forms]
   `(if-let-failed? [new-start# (attempt-> ~start ~form)]
      new-start#
      (attempt-> new-start# ~@forms))))


(defmacro attempt->>
  "Deprecated. Use ok->> instead."
  ([start] start)
  ([start form] `(->> ~start ~form))
  ([start form & forms]
   `(if-let-failed? [new-start# (attempt->> ~start ~form)]
      new-start#
      (attempt->> new-start# ~@forms))))

(defmacro ok->
  "Like some->, but with ok? instead of some?
   (i.e., short-circuits when it encounters a failure)"
  ([start & forms]
   `(if-let-failed? [v# ~start]
      v#
      (attempt-> v# ~@forms))))

(defmacro ok->>
  "Like some->>, but with ok? instead of some?
   (i.e., short-circuits when it encounters a failure)"
  ([start & forms]
   `(if-let-failed? [v# ~start]
      v#
      (attempt->> v# ~@forms))))

(defmacro as-ok->
  "Like as-> but with ok? "
  {:added "2.1"}
  [expr name & forms]
  `(attempt-all [~name ~expr
                 ~@(interleave (repeat name) (butlast forms))]
    ~(if (empty? forms)
       name
       (last forms))))

;; Assertions: Helpers

(defn assert-with
  "If (pred v) is true, return v
   otherwise, return (f/fail msg)"
  [pred v msg]
  (if (pred v) v (fail msg)))

(def assert-some? (partial assert-with some?))
(def assert-nil? (partial assert-with nil?))
(def assert-not-nil? assert-some?)
(def assert-not-empty? (partial assert-with (comp not empty?)))
(def assert-number? (partial assert-with number?))
