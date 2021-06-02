package fr.gouv.clea.ws.service.impl;

import fr.gouv.clea.ws.configuration.CleaKafkaProperties;
import fr.gouv.clea.ws.model.DecodedVisit;
import fr.gouv.clea.ws.model.ReportStat;
import fr.gouv.clea.ws.service.IProducerService;
import fr.gouv.clea.ws.utils.KafkaVisitDeserializer;
import fr.inria.clea.lsp.EncryptedLocationSpecificPart;
import fr.inria.clea.lsp.utils.TimeUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EmbeddedKafka(partitions = 1, brokerProperties = {"listeners=PLAINTEXT://localhost:9092", "port=9092"})
class ProducerServiceTest {

    @Autowired
    private IProducerService producerService;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private CleaKafkaProperties cleaKafkaProperties;

    private static DecodedVisit createSerializableDecodedVisit(Instant qrCodeScanTime, boolean isBackward, UUID locationTemporaryPublicId, byte[] encryptedLocationMessage) {
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
    void testProduceVisits() {
        final Map<String, Object> configs = KafkaTestUtils.consumerProps("visitConsumer", "false", embeddedKafkaBroker);
        final Consumer<String, DecodedVisit> visitConsumer = new DefaultKafkaConsumerFactory<>(
                configs,
                new StringDeserializer(),
                new KafkaVisitDeserializer()
        ).createConsumer();
        visitConsumer.subscribe(Collections.singleton(cleaKafkaProperties.getQrCodesTopic()));

        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        UUID uuid3 = UUID.randomUUID();

        byte[] encryptedLocationMessage1 = RandomUtils.nextBytes(21);
        byte[] encryptedLocationMessage2 = RandomUtils.nextBytes(22);
        byte[] encryptedLocationMessage3 = RandomUtils.nextBytes(23);

        boolean isBackward1 = RandomUtils.nextBoolean();
        boolean isBackward2 = RandomUtils.nextBoolean();
        boolean isBackward3 = RandomUtils.nextBoolean();

        Instant qrCodeScanTime1 = newRandomInstant();
        Instant qrCodeScanTime2 = newRandomInstant();
        Instant qrCodeScanTime3 = newRandomInstant();

        List<DecodedVisit> decoded = List.of(
                createSerializableDecodedVisit(qrCodeScanTime1, isBackward1, uuid1, encryptedLocationMessage1),
                createSerializableDecodedVisit(qrCodeScanTime2, isBackward2, uuid2, encryptedLocationMessage2),
                createSerializableDecodedVisit(qrCodeScanTime3, isBackward3, uuid3, encryptedLocationMessage3)
        );

        producerService.produceVisits(decoded);

        ConsumerRecords<String, DecodedVisit> records = KafkaTestUtils.getRecords(visitConsumer);
        assertThat(records.count()).isEqualTo(3);

        List<DecodedVisit> extracted = StreamSupport
                .stream(records.spliterator(), true)
                .map(ConsumerRecord::value)
                .collect(Collectors.toList());
        assertThat(extracted.size()).isEqualTo(3);

        DecodedVisit visit1 = extracted.stream().filter(it -> it.getLocationTemporaryPublicId().equals(uuid1)).findFirst().orElse(null);
        assertThat(visit1).isNotNull();
        assertThat(visit1.getLocationTemporaryPublicId()).isEqualTo(uuid1);
        assertThat(visit1.getEncryptedLocationSpecificPart().getEncryptedLocationMessage()).isEqualTo(encryptedLocationMessage1);
        assertThat(visit1.getQrCodeScanTime()).isEqualTo(qrCodeScanTime1);
        assertThat(visit1.isBackward()).isEqualTo(isBackward1);

        DecodedVisit visit2 = extracted.stream().filter(it -> it.getLocationTemporaryPublicId().equals(uuid2)).findFirst().orElse(null);
        assertThat(visit2).isNotNull();
        assertThat(visit2.getLocationTemporaryPublicId()).isEqualTo(uuid2);
        assertThat(visit2.getEncryptedLocationSpecificPart().getEncryptedLocationMessage()).isEqualTo(encryptedLocationMessage2);
        assertThat(visit2.getQrCodeScanTime()).isEqualTo(qrCodeScanTime2);
        assertThat(visit2.isBackward()).isEqualTo(isBackward2);

        DecodedVisit visit3 = extracted.stream().filter(it -> it.getLocationTemporaryPublicId().equals(uuid3)).findFirst().orElse(null);
        assertThat(visit3).isNotNull();
        assertThat(visit3.getLocationTemporaryPublicId()).isEqualTo(uuid3);
        assertThat(visit3.getEncryptedLocationSpecificPart().getEncryptedLocationMessage()).isEqualTo(encryptedLocationMessage3);
        assertThat(visit3.getQrCodeScanTime()).isEqualTo(qrCodeScanTime3);
        assertThat(visit3.isBackward()).isEqualTo(isBackward3);
    }

    @Test
    @DisplayName("test that produceStat can send a stat to kafka and that we can read it back")
    void testProduceStat() {
        final Map<String, Object> configs = KafkaTestUtils.consumerProps("statConsumer", "false", embeddedKafkaBroker);
        final Consumer<String, ReportStat> statConsumer = new DefaultKafkaConsumerFactory<>(
                configs,
                new StringDeserializer(),
                new JsonDeserializer<>(ReportStat.class)
        ).createConsumer();
        statConsumer.subscribe(Collections.singleton(cleaKafkaProperties.getStatsTopic()));

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

        ConsumerRecords<String, ReportStat> records = KafkaTestUtils.getRecords(statConsumer);
        assertThat(records.count()).isEqualTo(1);

        List<ReportStat> extracted = StreamSupport
                .stream(records.spliterator(), true)
                .map(ConsumerRecord::value)
                .collect(Collectors.toList());
        assertThat(extracted.size()).isEqualTo(1);

        var response = extracted.stream().findFirst().get();
        assertThat(response.getReported()).isEqualTo(10);
        assertThat(response.getRejected()).isEqualTo(2);
        assertThat(response.getBackwards()).isEqualTo(5);
        assertThat(response.getForwards()).isEqualTo(3);
        assertThat(response.getClose()).isEqualTo(4);
        assertThat(response.getTimestamp()).isEqualTo(timestamp);
    }

    private Instant newRandomInstant() {
        return Instant.ofEpochSecond(RandomUtils.nextLong(0, Instant.now().getEpochSecond()));
    }
}
