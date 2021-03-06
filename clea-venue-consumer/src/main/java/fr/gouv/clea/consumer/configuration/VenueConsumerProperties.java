package fr.gouv.clea.consumer.configuration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import javax.annotation.PostConstruct;
import javax.validation.constraints.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Validated
@Configuration
@ConfigurationProperties(prefix = "clea.conf")
@Slf4j
public class VenueConsumerProperties {

    @Min(value = 600)
    @HourDivisor
    private long durationUnitInSeconds;

    @Min(value = 1800)
    private long statSlotDurationInSeconds;

    @Positive
    private int driftBetweenDeviceAndOfficialTimeInSecs;

    @Positive
    private int cleaClockDriftInSecs;

    @Min(value = 10)
    @Max(value = 30)
    private int retentionDurationInDays;

    @NotNull
    private Security security;

    @PostConstruct
    private void logConfiguration() {
        log.info(this.toString());
    }
}

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Validated
class Security {

    @NotNull
    private Crypto crypto;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Validated
class Crypto {

    @NotBlank
    private String serverAuthoritySecretKey;
}
