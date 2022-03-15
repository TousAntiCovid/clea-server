package fr.gouv.clea.ws.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.gouv.clea.ws.model.DecodedVisit;
import fr.inria.clea.lsp.EncryptedLocationSpecificPart;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaVisitSerializerTest {

    @Test
    void testCanSerializeAndDeserializeAVisit() throws IOException {
        final var decodedVisit = DecodedVisit.builder()
                .encryptedLocationSpecificPart(
                        EncryptedLocationSpecificPart.builder()
                                .type(1)
                                .version(0)
                                .locationTemporaryPublicId(UUID.fromString("00000000-0000-0000-0000-000000000000"))
                                .encryptedLocationMessage(new byte[] { 1, 0, 1, 0, 1, 0, 1, 0 })
                                .build()
                )
                .qrCodeScanTime(Instant.parse("2021-08-09T16:40:42.435Z"))
                .isBackward(true)
                .build();

        try (KafkaVisitSerializer serializer = new KafkaVisitSerializer()) {
            final var serializedVisitBytes = serializer.serialize("", decodedVisit);

            final var serializedVisitJson = new ObjectMapper().readTree(new String(serializedVisitBytes));
            assertThat(serializedVisitJson.get("type").asInt()).isEqualTo(1);
            assertThat(serializedVisitJson.get("version").asLong()).isEqualTo(0);
            assertThat(serializedVisitJson.get("qrCodeScanTime").asLong()).isEqualTo(1628527242435L);
            assertThat(serializedVisitJson.get("isBackward").asBoolean()).isEqualTo(true);
            assertThat(serializedVisitJson.get("locationTemporaryPublicId").asText())
                    .isEqualTo("00000000-0000-0000-0000-000000000000");
            assertThat(serializedVisitJson.get("encryptedLocationMessage").asText()).isEqualTo("AQABAAEAAQA=");
            assertThat(serializedVisitJson.get("encryptedLocationMessage").binaryValue())
                    .isEqualTo(new byte[] { 1, 0, 1, 0, 1, 0, 1, 0 });
        }
    }

}
