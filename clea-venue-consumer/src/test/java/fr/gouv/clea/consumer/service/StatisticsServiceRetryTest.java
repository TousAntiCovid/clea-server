package fr.gouv.clea.consumer.service;

import fr.gouv.clea.consumer.test.IntegrationTest;
import fr.gouv.clea.consumer.test.QrCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;

import static fr.gouv.clea.consumer.test.ElasticManager.givenElasticIndexIsFrozen;
import static fr.gouv.clea.consumer.test.KafkaManager.assertThatNextRecordInTopic;

// The purpose of this test is to observe the behavior of spring's @Retryable
// annotation
// to be sure that conflicting operations sent to elastic (concurrent
// creation/update) are retried then recovered.
@IntegrationTest
class StatisticsServiceRetryTest {

    @Autowired
    private StatisticsService statisticsService;

    @Test
    void should_send_to_kafka_after_some_failed_create_attempts() {
        // given index "health-clealocations-2019.07.22" is not writable
        givenElasticIndexIsFrozen("health-clealocations-2019.07.22");

        // when a visit is sent
        final var visit = QrCode.defaultVisit()
                .qrCodeScanTime(Instant.parse("2019-07-22T08:13:00Z"))
                .venueType(4)
                .venueCategory1(1)
                .venueCategory2(2)
                .build();
        statisticsService.logStats(visit);

        // then kafka contains the following record
        assertThatNextRecordInTopic("test.clea.fct.clea-error-visit")
                .hasJsonValue("periodStart", "2019-07-22T08:00:00Z")
                .hasJsonValue("venueType", 4)
                .hasJsonValue("venueCategory1", 1)
                .hasJsonValue("venueCategory2", 2)
                .hasJsonValue("backward", true);
    }

    @Test
    void should_send_to_kafka_after_some_failed_update_attempts() {
        // given a visit has been written in elastic for period "2019-07-22T08:00:00Z"
        final var visit = QrCode.defaultVisit()
                .qrCodeScanTime(Instant.parse("2019-07-22T08:13:00Z"))
                .venueType(4)
                .venueCategory1(1)
                .venueCategory2(2)
                .build();
        statisticsService.logStats(visit);

        // given index "health-clealocations-2019.07.22" is not writable
        givenElasticIndexIsFrozen("health-clealocations-2019.07.22");

        // when another stat is sent for the same period
        statisticsService.logStats(visit);

        // then kafka contains the following record
        assertThatNextRecordInTopic("test.clea.fct.clea-error-visit")
                .hasJsonValue("periodStart", "2019-07-22T08:00:00Z")
                .hasJsonValue("venueType", 4)
                .hasJsonValue("venueCategory1", 1)
                .hasJsonValue("venueCategory2", 2)
                .hasJsonValue("backward", true);
    }
}
