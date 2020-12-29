# Failjure

![Run Tests](https://github.com/adambard/failjure/workflows/Run%20Tests/badge.svg)
[![Clojars Project](https://img.shields.io/clojars/v/failjure.svg)](https://clojars.org/failjure)

Failjure is a utility library for working with failed computations in Clojure(Script).
It provides an alternative to exception-based error handling for applications
where functional purity is more important.

It was inspired by [Andrew Brehaut's error monad](https://brehaut.net/blog/2011/error_monads)
implementation.

## Installation

Add the following to your build dependencies:

[![Clojars Project](https://img.shields.io/clojars/v/failjure.svg)](https://clojars.org/failjure)

You can also include the specs via the [failjure-spec](https://github.com/adambard/failjure-spec) project,
if you're into that sort of thing:

[![Clojars Project](https://img.shields.io/clojars/v/failjure-spec.svg)](https://clojars.org/failjure-spec)

## Example

```clojure

(require '[failjure.core :as f])

;; Write functions that return failures
(defn validate-email [email]
    (if (re-matches #".+@.+\..+" email)
      email
      (f/fail "Please enter a valid email address (got %s)" email)))

(defn validate-not-empty [s]
  (if (empty? s)
    (f/fail "Please enter a value")
    s))

;; Use attempt-all to handle failures
(defn validate-data [data]
  (f/attempt-all [email (validate-email (:email data))
                  username (validate-not-empty (:username data))
                  id (f/try* (Integer/parseInt (:id data)))]
    {:email email
     :username username}
    (f/when-failed [e]
      (log-error (f/message e))
      (handle-error e))))
```

## Quick Reference

### `HasFailed`

The cornerstone of this library, `HasFailed` is the protocol that describes a failed result.
Failjure implements HasFailed for Object (the catch-all not-failed implementation), Exception, and the
built-in Failure record type, but you can add your own very easily:

```clojure
(defrecord AnnotatedFailure [message data]
  f/HasFailed
  (failed? [self] true)
  (message [self] (:message self)))
```

### `fail`

`fail` is the basis of this library. It accepts an error message
with optional formatting arguments (formatted with Clojure's
format function) and creates a Failure object.

```clojure
(f/fail "Message here") ; => #Failure{:message "Message here"}
(f/fail "Hello, %s" "Failjure") ; => #Failure{:message "Hello, Failjure"}
```

### `failed?` and `message`

These two functions are part of the `HasFailed` protocol underpinning
failjure. `failed?` will tell you if a value is a failure (that is,
a `Failure`, a java `Exception` or a JavaScript `Error`.

### `attempt`

_Added in 2.1_

Accepts a value and a function. If the value is a failure, it is passed
to the function and the result is returned. Otherwise, value is returned.

```clojure
(defn handle-error [e] (str "Error: " (f/message e)))
(f/attempt handle-error "Ok")  ;=> "Ok"
(f/attempt handle-error (f/fail "failure"))  ;=> "Error: failure"
```

Try it with `partial`!

### `attempt-all`

`attempt-all` wraps an error monad for easy use with failure-returning
functions. You can add any number of bindings and it will short-circuit
on the first error, returning the failure.

```clojure
(f/attempt-all [x "Ok"] x)  ; => "Ok"
(f/attempt-all [x "Ok"
              y (fail "Fail")] x) ; => #Failure{:message "Fail"}
```

You can use `when-failed` to provide a function that will handle an error:

```clojure
(f/attempt-all [x "Ok"
                y (fail "Fail")]
  x
  (f/when-failed [e]
    (f/message e))) ; => "Fail"
```

### `ok->` and `ok->>`

If you're on-the-ball enough that you can represent your problem
as a series of compositions, you can use these threading macros
instead. Each form is applied to the output of the previous
as in `->` and `->>` (or, more accurately, `some->` and `some->>`),
 except that a failure value is short-circuited and returned immediately.

*Previous versions of failjure used `attempt->` and `attempt->>`, which
do not short-circuit if the starting value is a failure. `ok->` and `ok->>`
correct this shortcoming*

```clojure

(defn validate-non-blank [data field]
  (if (empty? (get data field))
    (f/fail "Value required for %s" field)
    data))

(let [result (f/ok->
              data
              (validate-non-blank :username)
              (validate-non-blank :password)
              (save-data))]
  (when (f/failed? result)
    (log (f/message result))
    (handle-failure result)))
```

### `as-ok->`

_Added in 2.1_

Like clojure's built-in `as->`, but short-circuits on failures.

```clojure

(f/as-ok-> "k" $
  (str $ "!")
  (str "O" $))) ; => Ok!

(f/as-ok-> "k" $
  (str $ "!")
  (f/try* (Integer/parseInt $))
  (str "O" $))) ; => Returns (does not throw) a NumberFormatException
```

### `try*`

This library does not handle exceptions by default. However,
you can wrap any form or forms in the `try*` macro, which is shorthand for:

```clojure
(try
  (do whatever)
  (catch Exception e e))
```

Since failjure treats returned exceptions as failures, this can be used
to adapt exception-throwing functions to failjure-style workflows.

### `try-all`

A version of `attempt-all` which automatically wraps each right side of its
bindings in a `try*` is available as `try-all` (thanks @lispyclouds):

```clojure
(try-all [x (/ 1 0)
          y (* 2 3)]
  y)  ; => java.lang.ArithmeticException (returned, not thrown)
```

### `(if-|when-)-let-(ok?|failed?)`

Failjure provides the helpers `if-let-ok?`, `if-let-failed?`, `when-let-ok?` and `when-let-failed?` to help
with branching. Each has the same basic structure:

```clojure
(f/if-let-failed? [x (something-which-may-fail)]
  (handle-failure x)
  (handle-success x))
```

* If no else is provided, the `if-` variants will return the value of x
* The `when-` variants will always return the value of x

### `assert-with`

The `assert-with` helper is a very basic way of adapting non-failjure-aware
functions/values to a failure context. The source is simply:

```clojure
(defn assert-with
  "If (pred v) is true, return v
   otherwise, return (f/fail msg)"
  [pred v msg]
  (if (pred v) v (fail msg)))
```

The usage looks like this:

```clojure
(f/attempt-all
  [x (f/assert-with some? (some-fn) "some-fn failed!")
   y (f/assert-with integer? (some-integer-returning-fn) "Not an integer.")]
  (handle-success x)
  (f/when-failed [e] (handle-failure e)))
```

The pre-packaged helpers `assert-some?`, `assert-nil?`, `assert-not-nil?`, `assert-not-empty?`, and `assert-number?`
are provided, but if you like, adding your own is as easy as `(def assert-my-pred? (partial f/assert-with my-pred?))`.


## Changelog

#### 2.1.1

Fix a deployment whoopsie causing `attempt` to have reversed argument order from what is documented
here. It was fine in my REPL, I swear!

#### 2.1.0

**USE 2.1.1 INSTEAD**

Added `attempt` and `as-ok->`. Changed from boot to leiningen for builds.

#### 2.0.0

Added ClojureScript support. Since the jar now includes .cljc instead of .clj files, which could
break older builds, I've decided this should be a major version. It should in general be totally
backwards-compatible though.

Notable changes:

* ClojureScript support (thanks @snorremd)
* `*try` now wraps its inputs in a function and returns `(try-fn *wrapped-fn*)`. This was necessary
  to keep the clj and cljs APIs consistent, but could break some existing use cases (probably).

#### 1.5.0

Added `try-all` feature

#### 1.4.0

Resolved issues caused by attempting to destructure failed results.

#### 1.3.0

Fix bug where `ok->/>` would sometimes double-eval initial argument.

#### 1.2.0

Refactored `attempt-all`, `attempt->`, and `attempt->>` to remove dependency on monads

#### 1.1.0

Added assert helpers

#### 1.0.1

This version is fully backwards-compatible with 0.1.4, but failjure
 has been in use long enough to be considered stable. Also I added
a .1 because nobody trusts v1.0.0.

* Added `ok?`, `ok->`, `ok->>`, `if-let-ok?`, `when-let-ok?`, `if-let-failed?` and `when-let-failed?`

#### 0.1.4

* Added changelog.

## License

Copyright 2016 [Adam Bard](https://adambard.com/) and [Andrew Brehaut](https://brehaut.net/)

Distributed under the Eclipse Public License v1.0 (same as Clojure).
