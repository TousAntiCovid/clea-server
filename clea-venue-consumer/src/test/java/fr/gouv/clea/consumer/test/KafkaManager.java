package fr.gouv.clea.consumer.test;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.clea.consumer.model.ReportStat;
import lombok.SneakyThrows;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.connect.json.JsonDeserializer;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * A {@link TestExecutionListener} to start a Kafka container to be used as a
 * dependency for SpringBootTests.
 * <p>
 * It starts a Kafka container statically and export required system properties
 * to override Spring application context configuration.
 * <p>
 * It starts / closes a consumer before / after each test method.
 * <p>
 * Static methods {@link KafkaManager#assertThatNextRecordInTopic(String)} can
 * be used to verify messages from Kafka.
 */
public class KafkaManager implements TestExecutionListener {

    private static final KafkaContainer KAFKA = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:5.4.4")
    );

    private static final Admin KAFKA_ADMIN;

    private static final Map<String, KafkaTopicListener> TOPICS;

    static {
        KAFKA.start();
        System.setProperty("spring.kafka.bootstrap-servers", KAFKA.getBootstrapServers());

        KAFKA_ADMIN = Admin.create(
                Map.of(
                        AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers()
                )
        );

        TOPICS = Map.of(
                "test.clea.fct.qr-code-scan", new KafkaTopicListener("test.clea.fct.qr-code-scan"),
                "test.clea.fct.report-stat", new KafkaTopicListener("test.clea.fct.report-stat"),
                "test.clea.fct.clea-error-visit", new KafkaTopicListener("test.clea.fct.clea-error-visit")
        );
    }

    public static String getBootstrapServers() {
        return KAFKA.getBootstrapServers();
    }

    @Override
    public void beforeTestMethod(TestContext testContext) {
        // flush all kafka templates then fetch records to clear topics
        final var kafkaTemplates = testContext.getApplicationContext()
                .getBeansOfType(KafkaTemplate.class)
                .values();
        kafkaTemplates.forEach(KafkaTemplate::flush);

        TOPICS.values()
                .forEach(KafkaTopicListener::start);
    }

    @Override
    public void afterTestMethod(TestContext testContext) {
        TOPICS.values()
                .forEach(KafkaTopicListener::stop);
    }

    public static KafkaRecordAssert assertThatNextRecordInTopic(String topicName) {
        return TOPICS.get(topicName)
                .getRecords(1)
                .stream()
                .findFirst()
                .map(KafkaRecordAssert::assertThat)
                .orElse(null);
    }

    public static class KafkaTopicListener {

        private final LinkedBlockingQueue<ConsumerRecord<String, JsonNode>> records = new LinkedBlockingQueue<>();

        private final KafkaMessageListenerContainer<String, JsonNode> container;

        @SneakyThrows
        public KafkaTopicListener(String topicName) {
            // create topic
            KAFKA_ADMIN.createTopics(
                    List.of(
                            new NewTopic(topicName, 1, (short) 1)
                    )
            ).all().get();

            // setup message listener to fill in records queue
            final var consumerConfig = KafkaTestUtils
                    .consumerProps(KAFKA.getBootstrapServers(), "kafka-manager", "false");
            // using a low heartbeat interval accelerates partitions reassignment, and so
            // the cleanup between tests
            consumerConfig.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 100);
            final var consumerFactory = new DefaultKafkaConsumerFactory<>(
                    consumerConfig, new StringDeserializer(),
                    new JsonDeserializer()
            );
            final var containerProperties = new ContainerProperties(topicName);
            container = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
            container.setupMessageListener((MessageListener<String, JsonNode>) records::add);
        }

        public void start() {
            container.start();
            ContainerTestUtils.waitForAssignment(container, 1);
        }

        public void stop() {
            container.stop();
            records.clear();
        }

        public List<ConsumerRecord<String, JsonNode>> getRecords(int expectedRecords) {
            return IntStream.rangeClosed(1, expectedRecords)
                    .mapToObj(i -> {
                        try {
                            return records.poll(10, SECONDS);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .collect(Collectors.toList());
        }
    }

    public static void sendReportStat(ReportStat reportStat) {
        Map<String, Object> configs = KafkaTestUtils.producerProps(KafkaManager.getBootstrapServers());
        Producer<String, ReportStat> producer = new DefaultKafkaProducerFactory<>(
                configs,
                new StringSerializer(),
                new JsonSerializer<ReportStat>()
        ).createProducer();
        producer.send(new ProducerRecord<>("test.clea.fct.report-stat", reportStat));
        producer.flush();
        producer.close();

    }
}
