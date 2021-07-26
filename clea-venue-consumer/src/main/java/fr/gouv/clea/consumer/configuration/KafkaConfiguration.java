package fr.gouv.clea.consumer.configuration;

import fr.gouv.clea.consumer.model.DecodedVisit;
import fr.gouv.clea.consumer.model.ReportStat;
import fr.gouv.clea.consumer.model.StatLocation;
import fr.gouv.clea.consumer.utils.KafkaVisitDeserializer;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Configuration
@EnableKafka
@RequiredArgsConstructor
public class KafkaConfiguration {

    private final KafkaProperties kafkaProperties;

    @Bean
    public ConsumerFactory<String, DecodedVisit> visitConsumerFactory() {
        final var props = kafkaProperties.buildConsumerProperties();
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

    @Bean
    public ProducerFactory<String, StatLocation> cleaErrorStatsTopicFactory() {
        final var configProps = kafkaProperties.buildConsumerProperties();
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, StatLocation> cleaErrorStatsTopicKafkaTemplate() {
        return new KafkaTemplate<>(cleaErrorStatsTopicFactory());
    }
}
