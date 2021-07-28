package fr.gouv.clea.consumer.it;

import fr.gouv.clea.consumer.configuration.StatistiquesElasticsearchContainer;
import fr.gouv.clea.consumer.configuration.VenueConsumerProperties;
import fr.gouv.clea.consumer.model.LocationStat;
import fr.gouv.clea.consumer.model.ReportStat;
import fr.gouv.clea.consumer.model.ReportStatEntity;
import fr.gouv.clea.consumer.model.Visit;
import fr.gouv.clea.consumer.repository.statistiques.ReportStatIndex;
import fr.gouv.clea.consumer.repository.statistiques.StatLocationIndex;
import fr.gouv.clea.consumer.service.impl.StatService;
import fr.inria.clea.lsp.utils.TimeUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class StatistiquesServiceIT {

    private static final UUID _UUID = UUID.randomUUID();

    private static final byte[] LOCATION_TEMPORARY_SECRET_KEY = RandomUtils.nextBytes(20);

    private static final byte[] ENCRYPTED_LOCATION_CONTACT_MESSAGE = RandomUtils.nextBytes(20);

    private static final Instant TODAY_AT_MIDNIGHT = Instant.now().truncatedTo(ChronoUnit.DAYS);

    private static final Instant TODAY_AT_8AM = TODAY_AT_MIDNIGHT.plus(8, ChronoUnit.HOURS);

    private static final long TODAY_AT_MIDNIGHT_AS_NTP = TimeUtils.ntpTimestampFromInstant(TODAY_AT_MIDNIGHT);

    @Container
    private static ElasticsearchContainer elasticsearchContainer = new StatistiquesElasticsearchContainer();

    @Autowired
    private VenueConsumerProperties properties;

    @Autowired
    private StatService service;

    @Autowired
    private ReportStatIndex reportStatIndex;

    @Autowired
    private StatLocationIndex statLocationIndex;

    @Autowired
    private ElasticsearchOperations template;

    @BeforeAll
    static void setUp() {
        elasticsearchContainer.start();
    }

    @AfterAll
    static void destroy() {
        elasticsearchContainer.stop();
    }

    static Visit defaultVisit() {
        return Visit.builder()
                .version(0)
                .type(0)
                .staff(true)
                .locationTemporaryPublicId(_UUID)
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

    @BeforeEach
    void testIsContainerRunning() {
        Assertions.assertTrue(elasticsearchContainer.isRunning());
        this.recreateIndexes();
    }

    @Test
    void should_create_a_new_stat_in_DB_when_visit_has_no_existing_context() {
        Visit visit = defaultVisit().toBuilder()
                .qrCodeScanTime(TODAY_AT_8AM.plus(15, ChronoUnit.MINUTES))
                .venueType(4)
                .venueCategory1(1)
                .venueCategory2(2)
                .build();

        service.logStats(visit);

        List<LocationStat> stats = new ArrayList<>();
        statLocationIndex.findAll().forEach((stats::add));

        assertThat(stats.size()).isEqualTo(1L);
        LocationStat locationStat = stats.get(0);
        assertThat(locationStat.getPeriodStart()).isEqualTo(TODAY_AT_8AM);
        assertThat(locationStat.getVenueType()).isEqualTo(4);
        assertThat(locationStat.getVenueCategory1()).isEqualTo(1);
        assertThat(locationStat.getVenueCategory2()).isEqualTo(2);
        assertThat(locationStat.getBackwardVisits()).isEqualTo(1L);
        assertThat(locationStat.getForwardVisits()).isZero();

    }

    @Test
    void should_update_an_existing_stat_in_DB_when_visit_has_existing_context() throws InterruptedException {
        /*
         * if: periodStartTime = today at 00:00:00 qrCodeScanTime = today at 08:15:00
         * durationUnit = 1800 seconds then: => scanTimeSlot = 8*2 = 16 => stat duration
         * = periodStartTime + (slot * durationUnit) = today at 08:00:00
         */
        Visit visit1 = defaultVisit().toBuilder()
                .qrCodeScanTime(TODAY_AT_8AM.plus(15, ChronoUnit.MINUTES))
                .build();
        service.logStats(visit1);
        long before = statLocationIndex.count();

        Visit visit2 = defaultVisit().toBuilder()
                .qrCodeScanTime(TODAY_AT_8AM.plus(15, ChronoUnit.MINUTES))
                .build();
        service.logStats(visit2);
        long after = statLocationIndex.count();

        assertThat(before).isEqualTo(after);

        template.indexOps(LocationStat.class).refresh();

        List<LocationStat> stats = new ArrayList<>();
        statLocationIndex.findAll().forEach((stats::add));

        assertThat(stats.size()).isEqualTo(1L);
        LocationStat locationStat = stats.get(0);
        assertThat(locationStat.getPeriodStart()).isEqualTo(TODAY_AT_8AM);
        assertThat(locationStat.getVenueType()).isEqualTo(4);
        assertThat(locationStat.getVenueCategory1()).isEqualTo(1);
        assertThat(locationStat.getVenueCategory2()).isEqualTo(2);
        assertThat(locationStat.getBackwardVisits()).isEqualTo(2L);
        assertThat(locationStat.getForwardVisits()).isZero();
    }

    @Test
    void should_get_new_context_when_different_venue_type() {
        Visit visit1 = defaultVisit().toBuilder().venueType(1).build(),
                visit2 = defaultVisit().toBuilder().venueType(2).build();

        service.logStats(visit1);
        service.logStats(visit2);

        assertThat(statLocationIndex.count()).isEqualTo(2);
    }

    @Test
    void should_get_new_context_when_different_venue_category1() {
        Visit visit1 = defaultVisit().toBuilder().venueCategory1(1).build(),
                visit2 = defaultVisit().toBuilder().venueCategory1(2).build();

        service.logStats(visit1);
        service.logStats(visit2);

        assertThat(statLocationIndex.count()).isEqualTo(2);
    }

    @Test
    @Transactional
    void should_get_same_stat_period_when_visits_scantimes_are_in_same_stat_slot() throws InterruptedException {
        Visit visit1 = defaultVisit().toBuilder()
                .qrCodeScanTime(TODAY_AT_8AM)
                .isBackward(true)
                .build();
        Visit visit2 = defaultVisit().toBuilder()
                .qrCodeScanTime(TODAY_AT_8AM.plus(15, ChronoUnit.MINUTES)) // same stat slot
                .isBackward(true)
                .build();
        Visit visit3 = defaultVisit().toBuilder()
                .qrCodeScanTime(TODAY_AT_8AM.plus(28, ChronoUnit.MINUTES)) // same stat slot
                .isBackward(false)
                .build();

        Visit visit4 = defaultVisit().toBuilder()
                .qrCodeScanTime(TODAY_AT_8AM.plus(29, ChronoUnit.MINUTES)) // same stat slot
                .isBackward(true)
                .build();

        service.logStats(visit1);
        service.logStats(visit2);
        service.logStats(visit3);
        service.logStats(visit4);

        assertThat(statLocationIndex.count()).isEqualTo(1);

        final var statLocation = statLocationIndex
                .findByVenueTypeAndVenueCategory1AndVenueCategory2AndPeriodStart(
                        visit1.getVenueType(),
                        visit1.getVenueCategory1(),
                        visit1.getVenueCategory2(),
                        visit1.getQrCodeScanTime()
                )
                .get();
        assertThat(statLocation.getBackwardVisits()).as("backward visits").isEqualTo(3l);
        assertThat(statLocation.getForwardVisits()).as("forward visits").isEqualTo(1l);

    }

    @Test
    void should_get_new_period_when_scantimes_are_in_different_stat_slot() throws InterruptedException {
        Visit visit1 = defaultVisit().toBuilder()
                .qrCodeScanTime(TODAY_AT_8AM)
                .build();
        Visit visit2 = defaultVisit().toBuilder()
                .qrCodeScanTime(TODAY_AT_8AM.plus(31, ChronoUnit.MINUTES)) // different stat slot
                .build();

        service.logStats(visit1);
        service.logStats(visit2);

        assertThat(statLocationIndex.count()).isEqualTo(2);
    }

    @Test
    void should_get_new_context_when_different_venue_category2() {
        Visit visit1 = defaultVisit().toBuilder().venueCategory2(1).build(),
                visit2 = defaultVisit().toBuilder().venueCategory2(2).build();

        service.logStats(visit1);
        service.logStats(visit2);

        List<LocationStat> stats = new ArrayList<>();
        statLocationIndex.findAll().forEach(stats::add);

        assertThat(stats.size()).isEqualTo(2);
    }

    @Test
    void should_add_a_new_entity_in_db() {
        long timestamp = TimeUtils.currentNtpTime();
        ReportStat reportStat = ReportStat.builder()
                .reported(10)
                .rejected(2)
                .backwards(5)
                .forwards(3)
                .close(4)
                .timestamp(timestamp)
                .build();

        service.logStats(reportStat);

        assertThat(reportStatIndex.count()).isEqualTo(1);

        final var stat = reportStatIndex.findAll().iterator().next();

        assertThat(stat.getId()).isInstanceOf(String.class);
        assertThat(stat.getId()).isNotBlank();
        assertThat(stat.getReported()).isEqualTo(10);
        assertThat(stat.getRejected()).isEqualTo(2);
        assertThat(stat.getBackwards()).isEqualTo(5);
        assertThat(stat.getForwards()).isEqualTo(3);
        assertThat(stat.getClose()).isEqualTo(4);
        assertThat(stat.getTimestamp().truncatedTo(ChronoUnit.SECONDS))
                .isEqualTo(TimeUtils.instantFromTimestamp(timestamp).truncatedTo(ChronoUnit.SECONDS));
    }

    @Test
    void should_throw_OptimisticLockingFailureException_on_concurrent_access_for_stat_location() {
        Visit visit1 = defaultVisit().toBuilder().build();

        service.logStats(visit1);
        assertThat(reportStatIndex.count()).isEqualTo(1);

        Optional<LocationStat> statLocation = statLocationIndex
                .findByVenueTypeAndVenueCategory1AndVenueCategory2AndPeriodStart(4, 1, 2, this.getStatPeriod(visit1));

        statLocation.get().setForwardVisits(1);

        statLocationIndex.save(statLocation.get());

        statLocation.get().setForwardVisits(2);

        Assertions.assertThrows(OptimisticLockingFailureException.class, () -> {
            statLocationIndex.save(statLocation.get());
        });

    }

    @AfterEach
    void purge() {
        if (template.indexOps(LocationStat.class).exists()) {
            template.indexOps(LocationStat.class).delete();
        }
        if (template.indexOps(ReportStatEntity.class).exists()) {
            template.indexOps(ReportStatEntity.class).delete();
        }
    }

    private void recreateIndexes() {
        if (template.indexOps(LocationStat.class).exists()) {
            template.indexOps(LocationStat.class).delete();
        }
        if (template.indexOps(ReportStatEntity.class).exists()) {
            template.indexOps(ReportStatEntity.class).delete();
        }
        template.indexOps(ReportStatEntity.class).create();
        template.indexOps(LocationStat.class).create();
    }

    private LocationStat newStatLocation(Visit visit) {
        return LocationStat.builder()
                .periodStart(this.getStatPeriod(visit))
                .venueType(visit.getVenueType())
                .venueCategory1(visit.getVenueCategory1())
                .venueCategory2(visit.getVenueCategory2())
                .backwardVisits(visit.isBackward() ? 1 : 0)
                .forwardVisits(visit.isBackward() ? 0 : 1)
                .build();
    }

    private Instant getStatPeriod(Visit visit) {
        long secondsToRemove = visit.getQrCodeScanTime().getEpochSecond() % properties.getStatSlotDurationInSeconds();
        return visit.getQrCodeScanTime().minus(secondsToRemove, ChronoUnit.SECONDS).truncatedTo(ChronoUnit.SECONDS);
    }
}
