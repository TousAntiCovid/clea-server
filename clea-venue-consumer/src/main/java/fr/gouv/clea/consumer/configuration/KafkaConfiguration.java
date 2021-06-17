package fr.gouv.clea.consumer.configuration;

import fr.gouv.clea.consumer.model.DecodedVisit;
import fr.gouv.clea.consumer.model.ReportStat;
import fr.gouv.clea.consumer.utils.KafkaVisitDeserializer;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

@Configuration
@EnableKafka
@RequiredArgsConstructor
public class KafkaConfiguration {

    private static final String OFFSET_CONFIG = "earliest";

    private final KafkaProperties kafkaProperties;

    @Bean
    public ConsumerFactory<String, DecodedVisit> visitConsumerFactory() {
        final var props = kafkaProperties.buildConsumerProperties();
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, OFFSET_CONFIG);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaVisitDeserializer.class);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, DecodedVisit.class);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, DecodedVisit> visitContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, DecodedVisit> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(visitConsumerFactory());
        return factory;
    }

    @Bean
    public ConsumerFactory<String, ReportStat> statConsumerFactory() {
        final var props = kafkaProperties.buildConsumerProperties();
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, OFFSET_CONFIG);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, ReportStat.class);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ReportStat> statContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, ReportStat> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(statConsumerFactory());
        return factory;
    }
}
