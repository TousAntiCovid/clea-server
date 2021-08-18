package fr.gouv.clea.consumer.service;

import fr.gouv.clea.consumer.configuration.CleaKafkaProperties;
import fr.gouv.clea.consumer.model.DecodedVisit;
import fr.gouv.clea.consumer.model.ReportStat;
import fr.gouv.clea.consumer.test.IntegrationTest;
import fr.gouv.clea.consumer.test.KafkaManager;
import fr.gouv.clea.consumer.test.QrCode;
import fr.gouv.clea.consumer.utils.KafkaVisitSerializer;
import fr.inria.clea.lsp.LocationSpecificPartDecoder;
import fr.inria.clea.lsp.exception.CleaEncodingException;
import fr.inria.clea.lsp.utils.TimeUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.time.Instant;
import java.util.Base64;
import java.util.Map;

import static fr.gouv.clea.consumer.test.ElasticManager.assertThatAllDocumentsFromElastic;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

@IntegrationTest
class ConsumerServiceTest {

    @Autowired
    private CleaKafkaProperties cleaKafkaProperties;

    @Test
    @DisplayName("test that consumeVisit listener triggers when something is sent to visit queue")
    void testConsumeVisit() throws CleaEncodingException {
        Map<String, Object> configs = KafkaTestUtils.producerProps(KafkaManager.getBootstrapServers());
        Producer<String, DecodedVisit> producer = new DefaultKafkaProducerFactory<>(
                configs,
                new StringSerializer(),
                new KafkaVisitSerializer()
        ).createProducer();

        final var binaryLocationSpecificPart = Base64.getUrlDecoder().decode(QrCode.LOCATION_1_URL.getRef());
        DecodedVisit decodedVisit = new DecodedVisit(
                Instant.now(),
                new LocationSpecificPartDecoder().decodeHeader(binaryLocationSpecificPart),
                RandomUtils.nextBoolean()
        );

        producer.send(new ProducerRecord<>(cleaKafkaProperties.getQrCodesTopic(), decodedVisit));
        producer.flush();
        producer.close();

        // kafka listener has been called when something appears in elastic
        await().atMost(10, SECONDS)
                .untilAsserted(
                        () -> assertThatAllDocumentsFromElastic().hasSize(1)
                );
    }

    @Test
    @DisplayName("test that consumeStat listener triggers when something is sent to stat queue")
    void testConsumeStat() {
        Map<String, Object> configs = KafkaTestUtils.producerProps(KafkaManager.getBootstrapServers());
        Producer<String, ReportStat> producer = new DefaultKafkaProducerFactory<>(
                configs,
                new StringSerializer(),
                new JsonSerializer<ReportStat>()
        ).createProducer();

        long timestamp = TimeUtils.currentNtpTime();
        ReportStat reportStat = ReportStat.builder()
                .reported(10)
                .rejected(2)
                .backwards(5)
                .forwards(3)
                .close(4)
                .timestamp(timestamp)
                .build();

        producer.send(new ProducerRecord<>(cleaKafkaProperties.getReportStatsTopic(), reportStat));
        producer.flush();
        producer.close();

        // kafka listener has been called when something appears in elastic
        await().atMost(10, SECONDS)
                .untilAsserted(
                        () -> assertThatAllDocumentsFromElastic().hasSize(1)
                );
    }
}
