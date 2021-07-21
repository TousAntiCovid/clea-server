package fr.gouv.clea.consumer.it;

import fr.gouv.clea.consumer.configuration.StatistiquesElasticsearchContainer;
import fr.gouv.clea.consumer.model.ReportStat;
import fr.gouv.clea.consumer.model.ReportStatEntity;
import fr.gouv.clea.consumer.model.StatLocation;
import fr.gouv.clea.consumer.model.Visit;
import fr.gouv.clea.consumer.repository.statistiques.IReportStatRepository;
import fr.gouv.clea.consumer.repository.statistiques.IStatLocationRepository;
import fr.gouv.clea.consumer.service.impl.StatService;
import fr.inria.clea.lsp.utils.TimeUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
import java.util.concurrent.TimeUnit;

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
    private StatService service;

    @Autowired
    private IReportStatRepository reportStatRepository;

    @Autowired
    private IStatLocationRepository statLocationRepository;

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

        List<StatLocation> stats = new ArrayList<>();
        statLocationRepository.findAll().forEach((stats::add));

        assertThat(stats.size()).isEqualTo(1L);
        StatLocation statLocation = stats.get(0);
        assertThat(statLocation.getPeriod()).isEqualTo(TODAY_AT_8AM);
        assertThat(statLocation.getVenueType()).isEqualTo(4);
        assertThat(statLocation.getVenueCategory1()).isEqualTo(1);
        assertThat(statLocation.getVenueCategory2()).isEqualTo(2);
        assertThat(statLocation.getBackwardVisits()).isEqualTo(1L);
        assertThat(statLocation.getForwardVisits()).isZero();

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
        long before = statLocationRepository.count();

        Visit visit2 = defaultVisit().toBuilder()
                .qrCodeScanTime(TODAY_AT_8AM.plus(15, ChronoUnit.MINUTES))
                .build();
        service.logStats(visit2);
        long after = statLocationRepository.count();

        assertThat(before).isEqualTo(after);

        // FIXME: find a way to flush or making sure that all is persisted before
        // retrieving item
        TimeUnit.SECONDS.sleep(1);

        List<StatLocation> stats = new ArrayList<>();
        statLocationRepository.findAll().forEach((stats::add));

        assertThat(stats.size()).isEqualTo(1L);
        StatLocation statLocation = stats.get(0);
        assertThat(statLocation.getPeriod()).isEqualTo(TODAY_AT_8AM);
        assertThat(statLocation.getVenueType()).isEqualTo(4);
        assertThat(statLocation.getVenueCategory1()).isEqualTo(1);
        assertThat(statLocation.getVenueCategory2()).isEqualTo(2);
        assertThat(statLocation.getBackwardVisits()).isEqualTo(2L);
        assertThat(statLocation.getForwardVisits()).isZero();
    }

    @Test
    void should_get_new_context_when_different_venue_type() {
        Visit visit1 = defaultVisit().toBuilder().venueType(1).build(),
                visit2 = defaultVisit().toBuilder().venueType(2).build();

        service.logStats(visit1);
        service.logStats(visit2);

        assertThat(statLocationRepository.count()).isEqualTo(2);
    }

    @Test
    void should_get_new_context_when_different_venue_category1() {
        Visit visit1 = defaultVisit().toBuilder().venueCategory1(1).build(),
                visit2 = defaultVisit().toBuilder().venueCategory1(2).build();

        service.logStats(visit1);
        service.logStats(visit2);

        assertThat(statLocationRepository.count()).isEqualTo(2);
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

        assertThat(statLocationRepository.count()).isEqualTo(1);

        // FIXME: find a way to flush or making sure that all is persisted before
        // retrieving item
        TimeUnit.SECONDS.sleep(1);

        Optional<StatLocation> statLocation = statLocationRepository
                .findByVenueTypeAndVenueCategory1AndVenueCategory2AndPeriod(
                        visit1.getVenueType(),
                        visit1.getVenueCategory1(),
                        visit1.getVenueCategory2(),
                        visit1.getQrCodeScanTime()
                );
        if (statLocation.isPresent()) {
            assertThat(statLocation.get().getBackwardVisits()).as("back visits").isEqualTo(3l);
            assertThat(statLocation.get().getForwardVisits()).as("forward visits").isEqualTo(1l);
        }

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

        assertThat(statLocationRepository.count()).isEqualTo(2);
    }

    @Test
    void should_get_new_context_when_different_venue_category2() {
        Visit visit1 = defaultVisit().toBuilder().venueCategory2(1).build(),
                visit2 = defaultVisit().toBuilder().venueCategory2(2).build();

        service.logStats(visit1);
        service.logStats(visit2);

        List<StatLocation> stats = new ArrayList<>();
        statLocationRepository.findAll().forEach(stats::add);

        assertThat(stats.size()).isEqualTo(2);
    }

    @Test
    @DisplayName("logStats should add a new entity to DB")
    void testLogStats() {
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

        assertThat(reportStatRepository.count()).isEqualTo(1);

        List<ReportStatEntity> stats = new ArrayList<>();
        reportStatRepository.findAll().forEach(stats::add);

        assertThat(stats.get(0).getId()).isInstanceOf(String.class);
        assertThat(stats.get(0).getId()).isNotNull();
        assertThat(stats.get(0).getId()).isNotBlank();
        assertThat(stats.get(0).getId()).isNotEmpty();
        assertThat(stats.get(0).getReported()).isEqualTo(10);
        assertThat(stats.get(0).getRejected()).isEqualTo(2);
        assertThat(stats.get(0).getBackwards()).isEqualTo(5);
        assertThat(stats.get(0).getForwards()).isEqualTo(3);
        assertThat(stats.get(0).getClose()).isEqualTo(4);
        assertThat(stats.get(0).getTimestamp().truncatedTo(ChronoUnit.SECONDS))
                .isEqualTo(TimeUtils.instantFromTimestamp(timestamp).truncatedTo(ChronoUnit.SECONDS));
    }

    @AfterEach
    void purge() {
        if (template.indexExists(StatLocation.class)) {
            template.deleteIndex(StatLocation.class);
        }
        if (template.indexExists(ReportStatEntity.class)) {
            template.deleteIndex(ReportStatEntity.class);
        }
    }

    private void recreateIndexes() {
        if (template.indexOps(StatLocation.class).exists()) {
            template.indexOps(StatLocation.class).delete();
        }
        if (template.indexOps(ReportStatEntity.class).exists()) {
            template.indexOps(ReportStatEntity.class).delete();
        }
        template.indexOps(ReportStatEntity.class).create();
        template.indexOps(StatLocation.class).create();
    }
}
