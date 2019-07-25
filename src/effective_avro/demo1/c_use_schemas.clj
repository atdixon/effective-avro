(ns effective-avro.demo1.c-use-schemas
  (:import (org.apache.avro Schema$Parser)
           (org.apache.avro.generic GenericDatumWriter GenericRecordBuilder)
           (java.io ByteArrayOutputStream)
           (org.apache.avro.io EncoderFactory)))

;;; ******* PARSE *******

(def green-schema
  (->> "src/effective_avro/demo1/001-schema-green.json"
       slurp
       (.parse (Schema$Parser.))))

(def blue-schema
  (->> "src/effective_avro/demo1/002-schema-blue.json"
       slurp
       (.parse (Schema$Parser.))))

;; note: ^^ using only avro library ^^


;;; ****** SERIALIZE ******

(defn make-green-bytes [val]
  (let [out (ByteArrayOutputStream.)
        encoder (.binaryEncoder (EncoderFactory/get) out nil)
        writer (GenericDatumWriter. green-schema)]
    (.write writer val encoder)
    (.flush encoder)
    (.toByteArray out)))


(def green-record
  (->
    (GenericRecordBuilder. green-schema)
    (.set "username" "boogie")
    (.set "timestamp" "2019-11-01")
    (.build)))


;; {:username "boogie" :timestamp "2019-11-01"}
