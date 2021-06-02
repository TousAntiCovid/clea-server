package fr.gouv.clea.consumer.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext
@TestPropertySource("classpath:application.yml")
class VenueConsumerPropertiesIntegrationTest {

    @Autowired
    private VenueConsumerProperties properties;

    @Test
    void should_get_expected_values() {
        assertThat(properties.getDurationUnitInSeconds()).isEqualTo(1800);
        assertThat(properties.getDriftBetweenDeviceAndOfficialTimeInSecs()).isEqualTo(300);
        assertThat(properties.getCleaClockDriftInSecs()).isEqualTo(300);
        assertThat(properties.getRetentionDurationInDays()).isEqualTo(14);
        assertThat(properties.getSecurity().getCrypto().getServerAuthoritySecretKey())
                .isNotNull()
                .isNotBlank()
                .isNotEmpty();
    }
}
