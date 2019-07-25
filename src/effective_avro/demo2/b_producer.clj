(ns effective-avro.demo2.b-producer
  (:require [abracad.avro :as avro]
            [effective-avro.demo2.util :refer [send-message]]))

;; (send-message topic subject version record)

(def person-alpha
  {:first-name "alpha"
   :last-name "smith"
   :address {:city "austin"}})

(def person-beta
  {:first-name "beta"
   :last-name "smith"})
