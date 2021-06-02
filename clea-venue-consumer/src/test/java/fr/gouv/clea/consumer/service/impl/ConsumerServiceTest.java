package fr.gouv.clea.consumer.service.impl;

import fr.gouv.clea.consumer.configuration.CleaKafkaProperties;
import fr.gouv.clea.consumer.model.DecodedVisit;
import fr.gouv.clea.consumer.model.ReportStat;
import fr.gouv.clea.consumer.service.IDecodedVisitService;
import fr.gouv.clea.consumer.service.IStatService;
import fr.gouv.clea.consumer.utils.KafkaVisitSerializer;
import fr.inria.clea.lsp.EncryptedLocationSpecificPart;
import fr.inria.clea.lsp.utils.TimeUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@DirtiesContext
@EmbeddedKafka(partitions = 1, brokerProperties = {"listeners=PLAINTEXT://localhost:9092", "port=9092"})
class ConsumerServiceTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private CleaKafkaProperties cleaKafkaProperties;

    /*
     * @RefreshScope beans cannot be spied with @SpyBean
     * see https://github.com/spring-cloud/spring-cloud-consumerConfig/issues/944
     */
    @SpyBean
    private IDecodedVisitService decodedVisitService;

    @SpyBean
    private IStatService statService;

    @Test
    @DisplayName("test that consumeVisit listener triggers when something is sent to visit queue")
    void testConsumeVisit() {
        Map<String, Object> configs = KafkaTestUtils.producerProps(embeddedKafkaBroker);
        Producer<String, DecodedVisit> producer = new DefaultKafkaProducerFactory<>(
                configs,
                new StringSerializer(),
                new KafkaVisitSerializer()
        ).createProducer();

        DecodedVisit decodedVisit = new DecodedVisit(
                Instant.now(),
                EncryptedLocationSpecificPart.builder()
                        .version(RandomUtils.nextInt())
                        .type(RandomUtils.nextInt())
                        .locationTemporaryPublicId(UUID.randomUUID())
                        .encryptedLocationMessage(RandomUtils.nextBytes(20))
                        .build(),
                RandomUtils.nextBoolean()
        );

        producer.send(new ProducerRecord<>(cleaKafkaProperties.getQrCodesTopic(), decodedVisit));
        producer.flush();

        await().atMost(60, TimeUnit.SECONDS)
                .untilAsserted(
                        () -> verify(decodedVisitService, times(1))
                                .decryptAndValidate(any(DecodedVisit.class))
                );

        producer.close();
    }

    @Test
    @DisplayName("test that consumeStat listener triggers when something is sent to stat queue")
    void testConsumeStat() {
        Map<String, Object> configs = KafkaTestUtils.producerProps(embeddedKafkaBroker);
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

        producer.send(new ProducerRecord<>(cleaKafkaProperties.getStatsTopic(), reportStat));
        producer.flush();

        await().atMost(60, TimeUnit.SECONDS)
                .untilAsserted(
                        () -> verify(statService, times(1))
                                .logStats(any(ReportStat.class))
                );

        producer.close();
    }
}
