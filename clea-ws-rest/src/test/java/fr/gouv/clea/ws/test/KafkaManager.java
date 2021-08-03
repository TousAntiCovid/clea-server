package fr.gouv.clea.ws.test;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.clea.ws.configuration.CleaKafkaProperties;
import lombok.SneakyThrows;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.connect.json.JsonDeserializer;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

/**
 * A {@link TestExecutionListener} to start a Kafka container to be used as a
 * dependency for SpringBootTests.
 * <p>
 * It starts a Karfka container statically and export required system properties
 * to override Spring application context configuration.
 * <p>
 * It starts / closes a consumer before and after each test method.
 * <p>
 * Static method {@link KafkaManager#getSingleRecord(String)} can be used to
 * fetch messages from Kafka.
 */
public class KafkaManager implements TestExecutionListener {

    private static final KafkaContainer KAFKA = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:5.4.4")
    );

    static {
        KAFKA.start();
        System.setProperty("spring.kafka.bootstrap-servers", KAFKA.getBootstrapServers());
    }

    private static Consumer<String, JsonNode> consumer;

    @Override
    @SneakyThrows
    public void beforeTestMethod(TestContext testContext) {
        final var cleaKafkaProperties = testContext.getApplicationContext().getBean(CleaKafkaProperties.class);
        final var topics = List.of(
                cleaKafkaProperties.getQrCodesTopic(),
                cleaKafkaProperties.getStatsTopic()
        );

        final var config = KafkaTestUtils.consumerProps(KAFKA.getBootstrapServers(), "test-consumer", "false");
        consumer = new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(), new JsonDeserializer())
                .createConsumer();
        consumer.subscribe(topics);
    }

    @Override
    public void afterTestMethod(TestContext testContext) {
        consumer.commitSync();
        consumer.close();
    }

    public static ConsumerRecords<String, JsonNode> getRecords() {
        return KafkaTestUtils.getRecords(consumer);
    }

    public static ConsumerRecord<String, JsonNode> getSingleRecord(String topic) {
        return KafkaTestUtils.getSingleRecord(consumer, topic);
    }
}
