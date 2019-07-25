(ns effective-avro.demo1.d-use-schemas-better
  (:require [abracad.avro :as avro]))

;;; ******* PARSE *******

(def green-schema
  (avro/parse-schema
   (slurp "src/effective_avro/demo1/001-schema-green.json")))

(def blue-schema
  (avro/parse-schema
   (slurp "src/effective_avro/demo1/002-schema-blue.json")))

;;; ****** SERIALIZE ******

(def green-bytes
  (avro/binary-encoded
   green-schema {:username "alpha" :timestamp "2017-01-01"}))

(def blue-bytes
  (avro/binary-encoded
   blue-schema {:username "alpha" :event-type "click" :timestamp "2017-01-01"}))
