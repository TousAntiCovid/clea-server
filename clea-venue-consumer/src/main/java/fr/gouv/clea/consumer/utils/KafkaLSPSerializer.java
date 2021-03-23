package fr.gouv.clea.consumer.utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import fr.gouv.clea.consumer.model.DecodedVisit;
import fr.inria.clea.lsp.EncryptedLocationSpecificPart;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

import java.io.IOException;

public class KafkaLSPSerializer implements Serializer<DecodedVisit> {

    @Override
    public byte[] serialize(String topic, DecodedVisit data) {
        if (data == null)
            return null;
        try {
            return new ObjectMapper()
                    .registerModule(new SimpleModule().addSerializer(DecodedVisit.class, new JacksonLSPSerializer()))
                    .writeValueAsBytes(data);
        } catch (JsonProcessingException e) {
            throw new SerializationException("Error serializing JSON message", e);
        }
    }
}

class JacksonLSPSerializer extends StdSerializer<DecodedVisit> {

    public JacksonLSPSerializer() {
        this(null);
    }

    public JacksonLSPSerializer(Class<DecodedVisit> t) {
        super(t);
    }

    @Override
    public void serialize(DecodedVisit value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        Long getQrCodeScanTime = value.getQrCodeScanTime();
        boolean isBackward = value.isBackward();
        EncryptedLocationSpecificPart enc = value.getEncryptedLocationSpecificPart();
        gen.writeStartObject();
        gen.writeNumberField("qrCodeScanTime", getQrCodeScanTime);
        gen.writeBooleanField("isBackward", isBackward);
        gen.writeNumberField("version", enc.getVersion());
        gen.writeNumberField("type", enc.getType());
        gen.writeStringField("locationTemporaryPublicId", enc.getLocationTemporaryPublicId().toString());
        gen.writeBinaryField("encryptedLocationMessage", enc.getEncryptedLocationMessage());
        gen.writeEndObject();
    }
}
