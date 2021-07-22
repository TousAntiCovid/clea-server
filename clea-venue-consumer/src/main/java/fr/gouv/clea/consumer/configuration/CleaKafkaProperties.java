package fr.gouv.clea.consumer.configuration;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@Validated
@Configuration
@ConfigurationProperties(prefix = "clea.kafka")
@Slf4j
public class CleaKafkaProperties {

    @NotBlank
    private String qrCodesTopic;

    @NotBlank
    private String statsTopic;

    @NotBlank
    private String errorLocationStatsTopic;

}
