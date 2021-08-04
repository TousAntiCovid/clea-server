package fr.gouv.clea.ws.service.impl;

import fr.gouv.clea.ws.model.DecodedVisit;
import fr.gouv.clea.ws.model.ReportStat;
import fr.gouv.clea.ws.service.IProducerService;
import fr.gouv.clea.ws.test.IntegrationTest;
import fr.gouv.clea.ws.test.KafkaManager;
import fr.inria.clea.lsp.EncryptedLocationSpecificPart;
import fr.inria.clea.lsp.utils.TimeUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static fr.gouv.clea.ws.test.KafkaManager.assertThatKafkaRecordInTopic;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.assertj.core.api.Assertions.tuple;

@IntegrationTest
class ProducerServiceTest {

    @Autowired
    private IProducerService producerService;

    private static DecodedVisit createSerializableDecodedVisit(Instant qrCodeScanTime, boolean isBackward,
            UUID locationTemporaryPublicId, byte[] encryptedLocationMessage) {
        return new DecodedVisit(
                qrCodeScanTime,
                EncryptedLocationSpecificPart.builder()
                        .version(RandomUtils.nextInt())
                        .type(RandomUtils.nextInt())
                        .locationTemporaryPublicId(locationTemporaryPublicId)
                        .encryptedLocationMessage(encryptedLocationMessage)
                        .build(),
                isBackward
        );
    }

    @Test
    @DisplayName("test that produceVisits can send decoded lsps to kafka and that we can read them back")
    void can_send_decrypted_lsps_to_kafka() {

        UUID uuid1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID uuid2 = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID uuid3 = UUID.fromString("33333333-3333-3333-3333-333333333333");

        byte[] encryptedLocationMessage1 = RandomUtils.nextBytes(21);
        byte[] encryptedLocationMessage2 = RandomUtils.nextBytes(22);
        byte[] encryptedLocationMessage3 = RandomUtils.nextBytes(23);

        Instant qrCodeScanTime1 = Instant.now().minus(1, DAYS);
        Instant qrCodeScanTime2 = Instant.now().minus(2, DAYS);
        Instant qrCodeScanTime3 = Instant.now().minus(3, DAYS);

        List<DecodedVisit> decoded = List.of(
                createSerializableDecodedVisit(qrCodeScanTime1, true, uuid1, encryptedLocationMessage1),
                createSerializableDecodedVisit(qrCodeScanTime2, true, uuid2, encryptedLocationMessage2),
                createSerializableDecodedVisit(qrCodeScanTime3, false, uuid3, encryptedLocationMessage3)
        );

        producerService.produceVisits(decoded);

        final var records = KafkaManager.getRecords(3, "dev.clea.fct.visit-scan");
        Assertions.assertThat(records)
                .extracting(ConsumerRecord::value)
                .extracting(
                        value -> tuple(
                                value.get("locationTemporaryPublicId").asText(),
                                value.get("encryptedLocationMessage").asText(),
                                value.get("qrCodeScanTime").asLong() / 1000,
                                value.get("isBackward").asBoolean()
                        )
                )
                .containsExactly(
                        tuple(
                                "11111111-1111-1111-1111-111111111111",
                                Base64.getEncoder().encodeToString(encryptedLocationMessage1),
                                qrCodeScanTime1.getEpochSecond(), true
                        ),
                        tuple(
                                "22222222-2222-2222-2222-222222222222",
                                Base64.getEncoder().encodeToString(encryptedLocationMessage2),
                                qrCodeScanTime2.getEpochSecond(), true
                        ),
                        tuple(
                                "33333333-3333-3333-3333-333333333333",
                                Base64.getEncoder().encodeToString(encryptedLocationMessage3),
                                qrCodeScanTime3.getEpochSecond(), false
                        )
                );
    }

    @Test
    @DisplayName("test that produceStat can send a stat to kafka and that we can read it back")
    void can_send_report_stat_to_kafka() {
        long timestamp = TimeUtils.currentNtpTime();

        ReportStat reportStat = ReportStat.builder()
                .reported(10)
                .rejected(2)
                .backwards(5)
                .forwards(3)
                .close(4)
                .timestamp(timestamp)
                .build();

        producerService.produceStat(reportStat);

        assertThatKafkaRecordInTopic("dev.clea.fct.report-stats")
                .hasNoKey()
                .hasNoHeader("__TypeId__")
                .hasJsonValue("reported", 10)
                .hasJsonValue("rejected", 2)
                .hasJsonValue("backwards", 5)
                .hasJsonValue("forwards", 3)
                .hasJsonValue("close", 4)
                .hasJsonValue("timestamp", timestamp);
    }
}
