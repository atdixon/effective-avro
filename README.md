An isolated environment and demos using Kafka, Avro and a Schema Registry.

# Setup/IDE 

Import this as a deps.edn project. 

# Kafka and Schema Registry Sandbox

### Setup/Services

To produce and consume messages you can set up an isolated zk + kafka +
schema-registry + schema-registry-ui:

    1. Download latest apache/kafka
            From here: https://kafka.apache.org/quickstart
    2. Download latest confluent-community
            https://www.confluent.io/download/
                    (Search for "Download Confluent Community")
    3. Clone Landoop schema-registry-ui project
            https://github.com/Landoop/schema-registry-ui

*** I put these in a subfolder off of this project (./services/) which is 
already in the .gitignore for this project; my subfolder looks like this:

    services/
        confluent-5.3.0/
        kafka_2.12-2.2.0/
        schema-registry-ui/

Before starting the services you'll need to configure the schema registry:

    services/confluent-5.3.0/etc/schema-registry.properties

         - kafkastore.connection.url=localhost:2181
         + kafkastore.bootstrap.servers=PLAINTEXT://localhost:9092

         + access.control.allow.methods=GET,POST,PUT,OPTIONS
         + access.control.allow.origin=*
         
### Optional

This will add support to Kafka to allow for displaying Avro-serialized objects
using Kafka CLI tools (like ./bin/kafka-console-consumer.sh):

    $ cd deserializer
    $ mvn clean package
    $ cp target/schema-registry-deserializer-0.1-SNAPSHOT.jar ../services/kafka_2.12-2.2.0
    
(See below how to use this.)

### Start the Services

Then start each service in a separate terminal window:

    kafka_2.12-2.2.0/$ bin/zookeeper-server-start.sh ./config/zookeeper.properties

    kafka_2.12-2.2.0/$ bin/kafka-server-start.sh config/server.properties

    confluent-5.3.0/$ bin/schema-registry-start ./etc/schema-registry/schema-registry.properties

    schema-registry-ui/$ npm install && npm start

Now you have a clean, isolated environment to sandbox kafka, schema registry, etc.
You should be able to see the Schema Registry UI here:

    http://localhost:8080/#/
    
And the Schema Registry JSON API here:

    http://localhost:8081

(Example curls for the Schema Registry are here:
    https://github.com/confluentinc/schema-registry)
    
### Kafka CLI Tools

Run these from kafka dir:

    $ cd services/kafka_2.12-2.2.0
    

List topics:

    ./bin/kafka-topics.sh --list --bootstrap-server localhost:9092
    
Create a topic, "my-topic"

    bin/kafka-topics.sh --create --bootstrap-server localhost:9092 --replication-factor 1 --partitions 1 --topic my-topic

Show topic messages from the very beginning:

    ./bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic my-topic --from-beginning
     
*** ^^ This will only be able to show string messages. To show the Avro-encoded messages you have to install the
   optional SchemaRegistryDeserializer (see above); then you can:
     
    ./bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic my-topic --property value.deserializer=SchemaRegistryDeserializer

*** ^^ This will show the messages using the original writer's schema for EACH message. To use an explicit reader schema:

    ./bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic my-topic
        \ --property value.deserializer=AppleDeserializer --property value.deserializer.reader-schema=demo.person/2
        
*** ^^ The exact format of the reader-schema here is <full-subject-name>/<version>

Note: `kafka-console-consumer.sh` follows the log, so it will sit waiting for messages and display them
as they come; if you don't use "--from-beginning" it will wait for new messages only.

To produce Avro-encoded messages, see:

# Producing Messages

Start a REPL... then:

```.clojure
    (load "effective_avro/demo2/a_schemas")
    
    (in-ns 'effective-avro.demo2.a-schemas)

    (register-schema! person-schema-v1)
    (register-schema! person-schema-v2)
```
    
At this point you should see a new subject in your schema registry with two
versions.

Now you can produce some messages:

```.clojure
    (load "b_producer")
    
    (in-ns 'effective-avro.demo2.b-producer)

    (send-message "my-topic" "demo.person" 1 person-alpha)
    (send-message "my-topic" "demo.person" 2 person-beta)
```

The kafka-consumer should show these messages.
