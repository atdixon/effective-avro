(ns effective-avro.demo0.a-surprises
  (:require [abracad.avro :as avro]))

;; ...MISUSES...

;; DEFAULTS

(def dog-schema
  (avro/parse-schema
   {:name :dog
    :type :record
    :fields [{:name :first-name :type :string :default "Spot"}]}))

(defn roundtrip-with-defaults []
  (->> {}
       (avro/binary-encoded dog-schema)
       (avro/decode dog-schema)))
; => fails!

(def dog-schema-v0
  (avro/parse-schema
   {:name :dog
    :type :record
    :fields [{:name :first-name :type :string}]}))

(defn roundtrip-with-defaults-2 []
  (->> {}
       (avro/binary-encoded dog-schema-v0)
       (avro/decode dog-schema)))

;; ...

;; BYTE CONSUME

(def order-schema-v100
  (avro/parse-schema
   {:name :order
    :type :record
    :fields [{:name :description :type :string}
             {:name :price :type :float}
             {:name :quantity :type :int}
             {:name :customer-id :type :string}]}))

(def person-schema-v5
  (avro/parse-schema
   {:name :person
    :type :record
    :fields [{:name :first-name :type :string}
             {:name :height :type :float}]}))


(defn roundtrip-3 []
  (->> {:description "Boom Box"
        :price 25.00
        :quantity 1
        :customer-id "1234"}
       (avro/binary-encoded order-schema-v100)
       (avro/decode person-schema-v5)))

;; no exception but misuse!
