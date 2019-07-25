(ns effective-avro.demo2.a-schemas
  (:require [abracad.avro :as avro]
            [effective-avro.demo2.util :refer [register-schema!]]))

;; ADDRESS SCHEMA

(def address-schema
  {:type :record
   :namespace :common
   :name :address
   :fields [{:name :line1 :type [:null :string]}
            {:name :line2 :type [:null :string]}
            {:name :city :type [:null :string]}
            {:name :state :type [:null :string]}
            {:name :zipcode :type [:null :string]}]})

;; PERSON SCHEMA (v1)

(def person-schema-v1
  {:type :record
   :namespace :demo
   :name :person
   :fields [{:name :first-name :type :string}
            {:name :last-name :type :string}
            {:name :address :type address-schema}
            {:name :parents :type [:null {:type :array :items :person}]}]})

;; PERSON SCHEMA (v2)

(def person-schema-v2
  {:type :record
   :namespace :demo
   :name :person
   :fields [{:name :first-name :type :string}
            {:name :last-name :type :string}]})



