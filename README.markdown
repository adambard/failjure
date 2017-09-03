# Failjure

[![Travis](https://img.shields.io/travis/adambard/failjure.svg?maxAge=2592000)](https://travis-ci.org/adambard/failjure)
[![Clojars Project](https://img.shields.io/clojars/v/failjure.svg)](https://clojars.org/failjure)

Failjure is a utility library for working with failed computations in Clojure.
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
a `Failure` or a java `Exception`.

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


### `HasFailed`

`HasFailed` is the protocol that describes a failed result. This library implements
HasFailed for Object (the catch-all not-failed implementation), Exception, and the
built-in Failure record type, but you can add your own very easily:

```clojure
(defrecord AnnotatedFailure [message data]
  f/HasFailed
  (failed? [self] true)
  (message [self] (:message self)))
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
