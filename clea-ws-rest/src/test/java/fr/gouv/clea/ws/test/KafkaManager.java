package fr.gouv.clea.ws.test;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.connect.json.JsonDeserializer;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toUnmodifiableMap;

/**
 * A {@link TestExecutionListener} to start a Kafka container to be used as a
 * dependency for SpringBootTests.
 * <p>
 * It starts a Kafka container statically and export required system properties
 * to override Spring application context configuration.
 * <p>
 * It starts / closes a consumer before / after each test method.
 * <p>
 * Static method {@link KafkaManager#getRecords(int, String)} can be used to
 * fetch messages from Kafka.
 */
@Slf4j
public class KafkaManager implements TestExecutionListener {

    private static final KafkaContainer KAFKA = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:5.4.4")
    );

    private static final List<String> TOPICS = List.of(
            "dev.clea.fct.visit-scan",
            "dev.clea.fct.report-stats"
    );

    private static final Map<String, Consumer<String, JsonNode>> CONSUMERS;

    static {
        KAFKA.start();
        System.setProperty("spring.kafka.bootstrap-servers", KAFKA.getBootstrapServers());

        // open a consumer to fetch records for each topic
        final var consumerConfig = KafkaTestUtils.consumerProps(KAFKA.getBootstrapServers(), "test-consumer", "false");
        CONSUMERS = TOPICS.stream()
                .collect(
                        toUnmodifiableMap(
                                topic -> topic,
                                topic -> new DefaultKafkaConsumerFactory<>(
                                        consumerConfig, new StringDeserializer(),
                                        new JsonDeserializer()
                                )
                                        .createConsumer()
                        )
                );
        CONSUMERS.forEach((topic, consumer) -> consumer.subscribe(List.of(topic)));
    }

    @Override
    public void beforeTestMethod(TestContext testContext) {
        // flush all kafka templates then fetch records to clear topics
        final var kafkaTemplates = testContext.getApplicationContext()
                .getBeansOfType(KafkaTemplate.class)
                .values();
        kafkaTemplates.forEach(KafkaTemplate::flush);
        CONSUMERS.values()
                .forEach(consumer -> KafkaTestUtils.getRecords(consumer, 100));
    }

    public static ConsumerRecords<String, JsonNode> getRecords(int expectedRecords, String topic) {
        final var consumer = CONSUMERS.get(topic);
        return KafkaTestUtils.getRecords(consumer, 5000, expectedRecords);
    }

    public static KafkaRecordAssert assertThatKafkaRecordInTopic(String topic) {
        final var consumer = CONSUMERS.get(topic);
        final var record = KafkaTestUtils.getSingleRecord(consumer, topic);
        return KafkaRecordAssert.assertThat(record);
    }
}
