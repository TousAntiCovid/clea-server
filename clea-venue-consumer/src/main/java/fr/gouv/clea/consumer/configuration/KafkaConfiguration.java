package fr.gouv.clea.consumer.configuration;

import fr.gouv.clea.consumer.model.DecodedVisit;
import fr.gouv.clea.consumer.model.ReportStat;
import fr.gouv.clea.consumer.utils.KafkaVisitDeserializer;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
@RequiredArgsConstructor
public class KafkaConfiguration {

    private static final String OFFSET_CONFIG = "earliest";
    private final CleaKafkaProperties cleaKafkaProperties;

    @Bean
    public ConsumerFactory<String, DecodedVisit> visitConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, cleaKafkaProperties.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, cleaKafkaProperties.getQrCodesConsumer());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, OFFSET_CONFIG);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaVisitDeserializer.class);
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
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, cleaKafkaProperties.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, cleaKafkaProperties.getStatsConsumer());
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
