import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaMetadata;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.kafka.common.serialization.Deserializer;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;

public final class SchemaRegistryDeserializer implements Deserializer<String> {

    private CachedSchemaRegistryClient registry;
    private Schema readerSchema;

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        String schemaRef = (String) configs.get("reader-schema");

        this.registry = new CachedSchemaRegistryClient("http://localhost:8081/", 1000);

        if (schemaRef != null) {
            String[] parts = schemaRef.split("/");
            String subject = parts[0];
            int version = Integer.parseInt(parts[1]);
            try {
                SchemaMetadata md = this.registry.getSchemaMetadata(subject, version);
                readerSchema = this.registry.getById(md.getId());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public String deserialize(String topic, byte[] data) {
        int id = -1;
        try {
            if (data == null)
                return null;

            // -- read header --
            ByteBuffer wrapped = ByteBuffer.wrap(
                Arrays.copyOfRange(data, 0, 4)); // big-endian by default
            id = wrapped.getInt();

            Schema schema = registry.getById(id);
            // -- read rest --
            GenericDatumReader<Object> reader =
                readerSchema == null ? new GenericDatumReader<>(schema)
                    : new GenericDatumReader<>(schema, readerSchema);
            Decoder decoder = DecoderFactory.get().binaryDecoder(
                data, 4, data.length - 4, null);
            Object result = reader.read(null, decoder);
            return result.toString(); // json.
        } catch (Exception e) {
            throw new RuntimeException("id: " + id, e);
        }
    }

}
