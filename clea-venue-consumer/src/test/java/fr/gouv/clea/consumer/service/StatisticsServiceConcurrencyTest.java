package fr.gouv.clea.consumer.service;

import fr.gouv.clea.consumer.model.Visit;
import fr.gouv.clea.consumer.test.IntegrationTest;
import fr.inria.clea.lsp.utils.TimeUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static fr.gouv.clea.consumer.test.ElasticManager.assertThatAllDocumentsFromElastic;
import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class StatisticsServiceConcurrencyTest {

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
        assertThat(pool.awaitTermination(60, TimeUnit.SECONDS))
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

}
