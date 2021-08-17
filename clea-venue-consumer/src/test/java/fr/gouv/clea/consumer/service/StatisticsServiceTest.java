package fr.gouv.clea.consumer.service;

import fr.gouv.clea.consumer.model.ReportStat;
import fr.gouv.clea.consumer.model.Visit;
import fr.gouv.clea.consumer.test.IntegrationTest;
import fr.inria.clea.lsp.utils.TimeUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.annotations.DateFormat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static fr.gouv.clea.consumer.test.ElasticManager.assertThatAllDocumentsFromElastic;
import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class StatisticsServiceTest {

    private static final String TODAY_YYYYMMDD = LocalDate.now().toString();

    private static final byte[] LOCATION_TEMPORARY_SECRET_KEY = RandomUtils.nextBytes(20);

    private static final byte[] ENCRYPTED_LOCATION_CONTACT_MESSAGE = RandomUtils.nextBytes(20);

    private static final Instant TODAY_AT_MIDNIGHT = Instant.now().truncatedTo(ChronoUnit.DAYS);

    private static final Instant TODAY_AT_8AM = TODAY_AT_MIDNIGHT.plus(8, ChronoUnit.HOURS);

    private static final long TODAY_AT_MIDNIGHT_AS_NTP = TimeUtils.ntpTimestampFromInstant(TODAY_AT_MIDNIGHT);

    @Autowired
    private StatisticsService statisticsService;

    static Visit defaultVisit() {
        return Visit.builder()
                .version(0)
                .type(0)
                .staff(true)
                .locationTemporaryPublicId(UUID.fromString("000000000000-0000-0000-0000-00000000"))
                .qrCodeRenewalIntervalExponentCompact(2)
                .venueType(4)
                .venueCategory1(1)
                .venueCategory2(2)
                .periodDuration(24)
                .compressedPeriodStartTime((int) (TODAY_AT_MIDNIGHT_AS_NTP / 3600))
                .qrCodeValidityStartTime(Instant.now())
                .qrCodeScanTime(Instant.now())
                .locationTemporarySecretKey(LOCATION_TEMPORARY_SECRET_KEY)
                .encryptedLocationContactMessage(ENCRYPTED_LOCATION_CONTACT_MESSAGE)
                .isBackward(true)
                .build();
    }

    @Test
    void should_create_a_new_stat_in_DB_when_visit_has_no_existing_context() {
        final var visit = defaultVisit().toBuilder()
                .qrCodeScanTime(TODAY_AT_8AM.plus(15, MINUTES))
                .venueType(4)
                .venueCategory1(1)
                .venueCategory2(2)
                .build();

        statisticsService.logStats(visit);

        assertThatAllDocumentsFromElastic()
                .containsExactlyInAnyOrder(
                        Map.of(
                                "id", TODAY_AT_8AM + "-vt:4-vc1:1-vc2:2",
                                "@timestamp", elasticDefaultStringRepresentation(TODAY_AT_8AM),
                                "venueType", 4,
                                "venueCategory1", 1,
                                "venueCategory2", 2,
                                "forwardVisits", 0,
                                "backwardVisits", 1
                        )
                );
    }

    @Test
    void should_update_an_existing_stat_in_DB_when_visit_has_existing_context() {
        // if:
        // periodStartTime = today at 00:00:00
        // qrCodeScanTime = today at 08:15:00
        // durationUnit = 1800 seconds
        //
        // then:
        // => scanTimeSlot = 8*2 = 16
        // => stat duration = periodStartTime + (slot * durationUnit) = today at
        // 08:00:00

        final var visit = defaultVisit().toBuilder()
                .qrCodeScanTime(TODAY_AT_8AM.plus(15, MINUTES))
                .build();

        statisticsService.logStats(visit);
        statisticsService.logStats(visit);

        assertThatAllDocumentsFromElastic()
                .containsExactlyInAnyOrder(
                        Map.of(
                                "id", TODAY_AT_8AM + "-vt:4-vc1:1-vc2:2",
                                "@timestamp", elasticDefaultStringRepresentation(TODAY_AT_8AM),
                                "venueType", 4,
                                "venueCategory1", 1,
                                "venueCategory2", 2,
                                "forwardVisits", 0,
                                "backwardVisits", 2
                        )
                );
    }

    @Test
    void should_get_new_context_when_different_venue_type() {
        final var visit1 = defaultVisit().toBuilder()
                .qrCodeScanTime(TODAY_AT_8AM)
                .venueType(1)
                .build();
        final var visit2 = defaultVisit().toBuilder()
                .qrCodeScanTime(TODAY_AT_8AM)
                .venueType(2)
                .build();

        statisticsService.logStats(visit1);
        statisticsService.logStats(visit2);

        assertThatAllDocumentsFromElastic()
                .containsExactlyInAnyOrder(
                        Map.of(
                                "id", TODAY_AT_8AM + "-vt:1-vc1:1-vc2:2",
                                "@timestamp", elasticDefaultStringRepresentation(TODAY_AT_8AM),
                                "venueType", 1,
                                "venueCategory1", 1,
                                "venueCategory2", 2,
                                "forwardVisits", 0,
                                "backwardVisits", 1
                        ),
                        Map.of(
                                "id", TODAY_AT_8AM + "-vt:2-vc1:1-vc2:2",
                                "@timestamp", elasticDefaultStringRepresentation(TODAY_AT_8AM),
                                "venueType", 2,
                                "venueCategory1", 1,
                                "venueCategory2", 2,
                                "forwardVisits", 0,
                                "backwardVisits", 1
                        )
                );
    }

    @Test
    void should_get_new_context_when_different_venue_category1() {
        final var visit1 = defaultVisit().toBuilder()
                .qrCodeScanTime(TODAY_AT_8AM)
                .venueCategory1(1)
                .build();
        final var visit2 = defaultVisit().toBuilder()
                .qrCodeScanTime(TODAY_AT_8AM)
                .venueCategory1(2)
                .build();

        statisticsService.logStats(visit1);
        statisticsService.logStats(visit2);

        assertThatAllDocumentsFromElastic()
                .containsExactlyInAnyOrder(
                        Map.of(
                                "id", TODAY_AT_8AM + "-vt:4-vc1:1-vc2:2",
                                "@timestamp", elasticDefaultStringRepresentation(TODAY_AT_8AM),
                                "venueType", 4,
                                "venueCategory1", 1,
                                "venueCategory2", 2,
                                "forwardVisits", 0,
                                "backwardVisits", 1
                        ),
                        Map.of(
                                "id", TODAY_AT_8AM + "-vt:4-vc1:2-vc2:2",
                                "@timestamp", elasticDefaultStringRepresentation(TODAY_AT_8AM),
                                "venueType", 4,
                                "venueCategory1", 2,
                                "venueCategory2", 2,
                                "forwardVisits", 0,
                                "backwardVisits", 1
                        )
                );
    }

    @Test
    void should_get_new_context_when_different_venue_category2() {
        final var visit1 = defaultVisit().toBuilder()
                .qrCodeScanTime(TODAY_AT_8AM)
                .venueCategory2(88)
                .build();
        final var visit2 = defaultVisit().toBuilder()
                .qrCodeScanTime(TODAY_AT_8AM)
                .venueCategory2(99)
                .build();

        statisticsService.logStats(visit1);
        statisticsService.logStats(visit2);

        assertThatAllDocumentsFromElastic()
                .containsExactlyInAnyOrder(
                        Map.of(
                                "id", TODAY_AT_8AM + "-vt:4-vc1:1-vc2:88",
                                "@timestamp", elasticDefaultStringRepresentation(TODAY_AT_8AM),
                                "venueType", 4,
                                "venueCategory1", 1,
                                "venueCategory2", 88,
                                "forwardVisits", 0,
                                "backwardVisits", 1
                        ),
                        Map.of(
                                "id", TODAY_AT_8AM + "-vt:4-vc1:1-vc2:99",
                                "@timestamp", elasticDefaultStringRepresentation(TODAY_AT_8AM),
                                "venueType", 4,
                                "venueCategory1", 1,
                                "venueCategory2", 99,
                                "forwardVisits", 0,
                                "backwardVisits", 1
                        )
                );
    }

    @Test
    void should_get_same_stat_period_when_visits_scantimes_are_in_same_stat_slot() {
        final var visit1 = defaultVisit().toBuilder()
                .qrCodeScanTime(TODAY_AT_8AM)
                .isBackward(true)
                .build();
        final var visit2 = defaultVisit().toBuilder()
                .qrCodeScanTime(TODAY_AT_8AM.plus(15, MINUTES)) // same stat slot
                .isBackward(true)
                .build();
        final var visit3 = defaultVisit().toBuilder()
                .qrCodeScanTime(TODAY_AT_8AM.plus(28, MINUTES)) // same stat slot
                .isBackward(false)
                .build();

        final var visit4 = defaultVisit().toBuilder()
                .qrCodeScanTime(TODAY_AT_8AM.plus(29, MINUTES)) // same stat slot
                .isBackward(true)
                .build();

        statisticsService.logStats(visit1);
        statisticsService.logStats(visit2);
        statisticsService.logStats(visit3);
        statisticsService.logStats(visit4);

        assertThatAllDocumentsFromElastic()
                .containsExactlyInAnyOrder(
                        Map.of(
                                "id", TODAY_AT_8AM + "-vt:4-vc1:1-vc2:2",
                                "@timestamp", elasticDefaultStringRepresentation(TODAY_AT_8AM),
                                "venueType", 4,
                                "venueCategory1", 1,
                                "venueCategory2", 2,
                                "forwardVisits", 1,
                                "backwardVisits", 3
                        )
                );
    }

    @Test
    void should_get_new_period_when_scantimes_are_in_different_stat_slot() {
        final var visit1 = defaultVisit().toBuilder()
                .qrCodeScanTime(TODAY_AT_8AM)
                .build();
        final var visit2 = defaultVisit().toBuilder()
                // according to our statSlotDurationInSeconds configuration,
                // the 31st minute is in a different stat slot
                .qrCodeScanTime(TODAY_AT_8AM.plus(31, MINUTES))
                .build();

        statisticsService.logStats(visit1);
        statisticsService.logStats(visit2);

        assertThatAllDocumentsFromElastic()
                .containsExactlyInAnyOrder(
                        Map.of(
                                "id", TODAY_YYYYMMDD + "T08:00:00Z-vt:4-vc1:1-vc2:2",
                                "@timestamp", TODAY_YYYYMMDD + "T08:00:00.000Z",
                                "venueType", 4,
                                "venueCategory1", 1,
                                "venueCategory2", 2,
                                "forwardVisits", 0,
                                "backwardVisits", 1
                        ),
                        Map.of(
                                "id", TODAY_YYYYMMDD + "T08:30:00Z-vt:4-vc1:1-vc2:2",
                                "@timestamp", TODAY_YYYYMMDD + "T08:30:00.000Z",
                                "venueType", 4,
                                "venueCategory1", 1,
                                "venueCategory2", 2,
                                "forwardVisits", 0,
                                "backwardVisits", 1
                        )
                );
    }

    @Test
    void should_handle_multiple_updates_concurrently() throws InterruptedException {
        // This test aims to observe that we don't loose data when we update the same
        // document.
        // We use a pool of 200 threads to simulate high concurrency,
        // We send 500 statistics about the same location,
        // This cause 500 updates on the same elastic document,
        // Then we expect the right amount of backward/forward visits in the elastic
        // document.

        final var pool = new ForkJoinPool(200);
        IntStream.rangeClosed(1, 500)
                .parallel()
                .mapToObj(
                        i -> defaultVisit().toBuilder()
                                .qrCodeScanTime(TODAY_AT_8AM)
                                // 1/4 of visits are backward: 125
                                // 3/4 of visits are forward: 375
                                .isBackward(i % 4 == 0)
                                .build()
                )
                .map(visit -> (Runnable) () -> statisticsService.logStats(visit))
                .forEach(pool::submit);

        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS))
                .as("pool tasks are all finished")
                .isTrue();

        assertThatAllDocumentsFromElastic()
                .containsExactly(
                        Map.of(
                                "id", TODAY_YYYYMMDD + "T08:00:00Z-vt:4-vc1:1-vc2:2",
                                "@timestamp", TODAY_YYYYMMDD + "T08:00:00.000Z",
                                "venueType", 4,
                                "venueCategory1", 1,
                                "venueCategory2", 2,
                                "forwardVisits", 375,
                                "backwardVisits", 125
                        )
                );
    }

    @Test
    void should_send_report_stats_to_elastic() {
        final var instant = Instant.parse("2019-07-22T09:37:42.251Z");
        final var reportStat = ReportStat.builder()
                .reported(10)
                .rejected(2)
                .backwards(5)
                .forwards(3)
                .close(4)
                .timestamp(TimeUtils.ntpTimestampFromInstant(instant))
                .build();

        statisticsService.logStats(reportStat);

        assertThatAllDocumentsFromElastic()
                .containsExactlyInAnyOrder(
                        Map.of(
                                "@timestamp", "2019-07-22T09:37:42.000Z",
                                "reported", 10,
                                "rejected", 2,
                                "backwards", 5,
                                "forwards", 3,
                                "close", 4
                        )
                );
    }

    private Object elasticDefaultStringRepresentation(Instant instant) {
        return instant.atOffset(UTC)
                .format(DateTimeFormatter.ofPattern(DateFormat.date_time.getPattern()));
    }
}
