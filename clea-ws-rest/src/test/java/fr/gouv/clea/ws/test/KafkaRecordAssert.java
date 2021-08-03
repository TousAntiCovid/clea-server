package fr.gouv.clea.ws.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.jsonpath.JsonPath;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.HamcrestCondition;
import org.hamcrest.Matcher;
import org.hamcrest.core.IsEqual;

public class KafkaRecordAssert extends AbstractAssert<KafkaRecordAssert, ConsumerRecord<? extends Object, JsonNode>> {

    private final ConsumerRecord<? extends Object, JsonNode> actualRecord;

    private KafkaRecordAssert(ConsumerRecord<? extends Object, JsonNode> actual) {
        super(actual, KafkaRecordAssert.class);
        this.actualRecord = actual;
    }

    public static KafkaRecordAssert assertThat(ConsumerRecord<? extends Object, JsonNode> consumerRecord) {
        return new KafkaRecordAssert(consumerRecord);
    }

    public <T> KafkaRecordAssert hasJsonValue(String jsonPath, Matcher<T> matcher) {
        final var jsonValue = actualRecord.value().toPrettyString();
        final T jsonPathValue = JsonPath.compile(jsonPath).read(jsonValue);
        Assertions.assertThat(jsonPathValue)
                .satisfies(new HamcrestCondition<>(matcher));
        return this;
    }

    public <T> KafkaRecordAssert hasJsonValue(String jsonPath, T expectedValue) {
        return hasJsonValue(jsonPath, new IsEqual<>(expectedValue));
    }

    public KafkaRecordAssert hasNoHeader(String headerName) {
        Assertions.assertThat(actualRecord.headers().headers(headerName))
                .extracting(Header::value)
                .extracting(String::new)
                .as("Kafka record shouldn't have a '%s' header", headerName)
                .isEmpty();
        return this;
    }

    public KafkaRecordAssert hasNoKey() {
        Assertions.assertThat(actualRecord.key())
                .as("Kafka record shouldn't have a key")
                .isNull();
        return this;
    }
}
