(ns effective-avro.demo1.f-use-schemas-more
  (:require [abracad.avro :as avro]))

;;; ******* PARSE *******

(def green-schema
  (avro/parse-schema
   {:type :record
    :name :green
    :fields [{:name :username :type :string}
             {:name :timestamp :type :string}]}))

(def blue-schema
  (avro/parse-schema
   {:type :record
    :name :green
    :fields [{:name :username :type :string}
             {:name :event-type :type :string}
             {:name :timestamp :type :string}]}))


;;; ****** SERIALIZE ******

(def green-bytes
  (avro/binary-encoded
   green-schema {:username "alpha" :timestamp "2017-01-01"}))

(def blue-bytes
  (avro/binary-encoded
   blue-schema {:username "alpha" :event-type "click" :timestamp "2017-01-01"}))


;;; ****** DE-SERIALIZE ******

(def green
  (avro/decode green-schema green-bytes))
