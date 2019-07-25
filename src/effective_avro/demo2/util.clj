(ns effective-avro.demo2.util
  (:require [abracad.avro :as avro])
  (:import [org.apache.kafka.clients.producer KafkaProducer ProducerRecord]
           (java.io ByteArrayOutputStream)
           (io.confluent.kafka.schemaregistry.client CachedSchemaRegistryClient)
           (java.nio ByteBuffer)
           (org.apache.kafka.common.serialization ByteArraySerializer)))

(def ^:dynamic *schema-registry*
  (CachedSchemaRegistryClient. "http://localhost:8081/" 1000))

(def ^:dynamic *kafka-producer*
  (KafkaProducer. {"bootstrap.servers" "localhost:9092"
                   "key.serializer" ByteArraySerializer
                   "value.serializer" ByteArraySerializer}))

(defn get-schema-id [subject version]
  (.getId (.getSchemaMetadata *schema-registry* subject version)))

(defn- send-one [topic bytes]
  (.send *kafka-producer* (ProducerRecord. topic bytes)))

(defn- to-bytes [subject version record]
  (with-open [out (ByteArrayOutputStream.)]
    (let [schema-id (get-schema-id subject version)]
      (.write out (-> 4 ByteBuffer/allocate (.putInt schema-id) .array))
      (avro/encode (.getById *schema-registry* schema-id) out record)
      (.toByteArray out))))

(defn send-message [topic subject version record]
  (send-one topic (to-bytes subject version record)))

(defn register-schema! [schema]
  (let [schema' (avro/parse-schema schema)]
    (.register *schema-registry* (.getFullName schema') schema')))

