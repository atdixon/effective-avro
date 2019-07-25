;; Avro wasn't designed primarily for streaming, but it is increasingly being
;; used for this today.
;;
;; Historically, the general Avro use cases require the "writer's schema" to
;; be negotiated with the consumer up front. For example, a large file would
;; write the schema once at the head of the file with the expectation being
;; that all data in the file was written with that schema.

;; In streaming use cases we may have a Kafka queue with messages encoded
;; each by various schemas with varying compatibility between them.

;; This file walks through the implications of this in a step-by-step
;; programmatic way.

;; It's written in Clojure but it should be useful to anyone trying to
;; untangle Avro in the streaming world.

(ns effective-avro.story
  (:require [abracad.avro :as avro]
            [abracad.helpers.schema :as schema]
            [abracad.helpers.clojure]
            [clojure.set :as s])
  (:import (org.apache.avro SchemaValidatorBuilder SchemaValidationException)))

;; Let's begin by making a queue to which we'll write and read our
;; messages. This is usually a durable queue like Kafka.

(def message-queue (atom ()))

;; We also need a schema registry; when streaming Avro messages there
;; is no other way around this. Each single message may be encoded by
;; a different schema. It would be impractical to store a message's whole
;; schema along with it, so we store an identifier instead. The schema
;; registry is used to lookup the actual schema by id.

(def schema-registry (atom {}))

(defn register-schema! [schema]
  (get
   (swap! schema-registry
          #(assoc % schema (or (get % schema) (inc (apply (partial max 0) (vals %))))))
   schema))

;; This function, register-schema!, simply returns a unique id that we
;; can use to later obtain the schema.

(defn get-schema-by-id [id]
  (get (s/map-invert @schema-registry) id))

;; Okay, now that we've stretched our canvas, let's start with a simple
;; message:

(def message-0 {:note "consume me"})

;; And of course we need a schema that we can use to encode (and decode)
;; this message:

(def schema-v1 (avro/parse-schema {:type :record :name :simple_message
                                   :fields [{:name :note :type :string}]}))

;; And here's a handy function to encode and enqueue the encoded message at
;; the same time. Note that we write both the message bytes and the schema id
;; that we used for the encoding; we need both of these things to decode the
;; message later:

(defn enqueue-encoded-message! [schema message]
  (swap! message-queue conj {:schema-id (register-schema! schema)
                             :message-bytes (avro/binary-encoded schema message)}))

;; Finally, we can write our first message:

(enqueue-encoded-message! schema-v1 message-0)

;; Now, before we decode any messages, here is a simple function that can take a queue entry,
;; look up the writer's schema and decode the message using a schema that we desire. In Avro
;; parlance, this desired schema is called the target schema, or the "writer's schema":

(defn decode-queue-message [reader-schema {:keys [:schema-id :message-bytes]}]
  (avro/decode (avro/datum-reader (get-schema-by-id schema-id) reader-schema)
               message-bytes))

;; One last helper function: this one lets us "tail" our message queue; it decodes the n
;; most recent messages from our queue:

(defn consume-tail [reader-schema n]
  (map (partial decode-queue-message reader-schema)
       (reverse (take n @message-queue))))

;; And now we're ready to consume our only message:

(assert (= [{:note "consume me"}]
           (consume-tail schema-v1 1)))

;; Great! Now, let's send another message:

(def message-1 {:note "i'm #2!" :i-am-hungry true})

;; Of course our old schema doesn't know about the :i-am-hungry property, so we
;; need a new version of our schema:

(def schema-v2 (avro/parse-schema {:type :record :name :simple_message
                                   :fields [{:name :note :type :string}
                                            {:name :i-am-hungry :type :boolean :default false}]}))

;; Now we write the new message:

(enqueue-encoded-message! schema-v2 message-1)

;; And let's verify that we can consume our entire queue using this new schema:

(assert (= [{:note "consume me" :i-am-hungry false}
            {:note "i'm #2!" :i-am-hungry true}]
           (consume-tail schema-v2 2)))

;; Look what we just did there. This is pretty cool. We consume our old message types
;; using our new schema. This means our new schema is backward compatible with the old
;; schema.

;; Interestingly, we are also *forward* compatible, as well. Our old consumers out in
;; there in the world can read all of our messages, even the new one, using our original
;; shchema:

(assert (= [{:note "consume me"}
            {:note "i'm #2!"}]
           (consume-tail schema-v1 2)))

;; So cool! This means we can replay our queues at any time, even using old
;; consumers, without having to re-deploy them!
;;
;; Why would anyone *ever* make  a non-backwards or non-forwards compatible schema
;; change? You might be thinking to yourself, "What if I want to add a new required
;; field?" The answer is simple: don't ever do that.

;; We can also ask Avro to tell us if our schemas are fully compatible:

(defn fully-compatible? [schema1 schema2]
  (let [validator (.. (SchemaValidatorBuilder.) (mutualReadStrategy) (validateAll))]
    (try
      (.validate validator schema1 [schema2]) true
      (catch SchemaValidationException e
        false))))

(assert (fully-compatible? schema-v1 schema-v2))

;; == Heterogenous Messages ==

;; So far we have looked at a schema evolution for one "subject" type.

;; What if we would like a queue of heterogenous subjects? That is, each message
;; in the queue can be written using schemas that don't relate to each other and
;; are not compatible in any way.

;; Let's make this concrete. So far we have only been writing "simple messages"
;; that contain a note and, after we revised our schema a little, a boolean indicating
;; hunger level. Now suppose that we want to write messages of an entirely different
;; entity type, ones that represent people:

(def person-message-0 {:fname "bob" :lname "smith"})

(def person-schema-v1 (avro/parse-schema {:type :record :name :person
                                          :fields [{:name :fname :type :string}
                                                   {:name :lname :type :string}]}))

(enqueue-encoded-message! person-schema-v1 person-message-0)

;; Now we have a problem. We now have *heterogenous* subjects on our queue
;; and unless we do something to differentiate them, we'll blow up when we
;; try to consume them.

;; We will need to upgrade our decoder to be subject-aware. There are a many
;; many ways to achieve this, but for simplicity we are going to assume that all
;; of the messages in our queue are avro record types whose record type names are
;; stable over time -- and we'll use these names to indicate our subject.

;; When we decode our messages we'll have to provide more than just one desired
;; reader schema; we'll need a map of desired schemas per subject. Here are our
;; new decode-queue-message and consume-tail functions:

(defn decode-queue-message [reader-schema-map {:keys [:schema-id :message-bytes]}]
  (let [writer-schema (get-schema-by-id schema-id)
        schema-subject (keyword (.getName writer-schema))]
    (avro/decode (avro/datum-reader writer-schema (schema-subject reader-schema-map))
                 message-bytes)))

(defn consume-tail [reader-schema-map n]
  (map (partial decode-queue-message reader-schema-map)
       (reverse (take n @message-queue))))

;; And, voila, we can replay the whole queue again:

(assert (= [{:note "consume me"}
            {:note "i'm #2!"}
            {:fname "bob" :lname "smith"}]
           (consume-tail {:simple_message schema-v1 :person person-schema-v1} 3)))


;; Next time, we'll look at full forward- and backward- compatibility for complex schema
;; changes, including field and type renames via Avro aliases. Hint: most of this works out
;; of the box although we need a little hack to get field renames to be forward- compatible
;; with older consumers.
