package fr.gouv.clea.consumer.service.impl;

import fr.gouv.clea.consumer.model.ReportStat;
import fr.gouv.clea.consumer.model.StatLocation;
import fr.gouv.clea.consumer.model.StatLocationKey;
import fr.gouv.clea.consumer.model.Visit;
import fr.gouv.clea.consumer.repository.IReportStatRepository;
import fr.gouv.clea.consumer.repository.IStatLocationJpaRepository;
import fr.inria.clea.lsp.utils.TimeUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class StatServiceTest {

    private static final UUID _UUID = UUID.randomUUID();

    private static final byte[] LOCATION_TEMPORARY_SECRET_KEY = RandomUtils.nextBytes(20);

    private static final byte[] ENCRYPTED_LOCATION_CONTACT_MESSAGE = RandomUtils.nextBytes(20);

    private static final Instant TODAY_AT_MIDNIGHT = Instant.now().truncatedTo(ChronoUnit.DAYS);

    private static final Instant TODAY_AT_8AM = TODAY_AT_MIDNIGHT.plus(8, ChronoUnit.HOURS);

    private static final long TODAY_AT_MIDNIGHT_AS_NTP = TimeUtils.ntpTimestampFromInstant(TODAY_AT_MIDNIGHT);

    @Autowired
    private IStatLocationJpaRepository statLocationRepository;

    @Autowired
    private IReportStatRepository reportStatRepository;

    @Autowired
    private StatService service;

    @PersistenceContext
    EntityManager entityManager;

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

    @AfterEach
    void clean() {
        statLocationRepository.deleteAll();
    }

    @Test
    void should_create_a_new_stat_in_DB_when_visit_has_no_existing_context() {
        /*
         * if: periodStartTime = today at 00:00:00 qrCodeScanTime = today at 08:15:00
         * durationUnit = 1800 seconds then: => scanTimeSlot = 8*2 = 16 => stat duration
         * = periodStartTime + (slot * durationUnit) = today at 08:00:00
         */

        Visit visit = defaultVisit().toBuilder()
                .qrCodeScanTime(TODAY_AT_8AM.plus(15, ChronoUnit.MINUTES))
                .venueType(4)
                .venueCategory1(1)
                .venueCategory2(2)
                .build();

        service.logStats(visit);

        List<StatLocation> stats = statLocationRepository.findAll();
        assertThat(stats.size()).isEqualTo(1L);
        StatLocation statLocation = stats.get(0);
        assertThat(statLocation.getStatLocationKey().getPeriod()).isEqualTo(TODAY_AT_8AM);
        assertThat(statLocation.getStatLocationKey().getVenueType()).isEqualTo(4);
        assertThat(statLocation.getStatLocationKey().getVenueCategory1()).isEqualTo(1);
        assertThat(statLocation.getStatLocationKey().getVenueCategory2()).isEqualTo(2);
        assertThat(statLocation.getBackwardVisits()).isEqualTo(1L);
        assertThat(statLocation.getForwardVisits()).isZero();
    }

    @Test
    void should_update_an_existing_stat_in_DB_when_visit_has_existing_context() {
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

        List<StatLocation> stats = statLocationRepository.findAll();
        assertThat(stats.size()).isEqualTo(1L);
        StatLocation statLocation = stats.get(0);
        assertThat(statLocation.getStatLocationKey().getPeriod()).isEqualTo(TODAY_AT_8AM);
        assertThat(statLocation.getStatLocationKey().getVenueType()).isEqualTo(4);
        assertThat(statLocation.getStatLocationKey().getVenueCategory1()).isEqualTo(1);
        assertThat(statLocation.getStatLocationKey().getVenueCategory2()).isEqualTo(2);
        assertThat(statLocation.getBackwardVisits()).isEqualTo(2L);
        assertThat(statLocation.getForwardVisits()).isZero();
    }

    @Test
    @Transactional
    void should_get_same_stat_period_when_visits_scantimes_are_in_same_stat_slot() {
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

        entityManager.flush();
        entityManager.clear();
        assertThat(statLocationRepository.count()).isEqualTo(1);
        StatLocationKey key = service.buildKey(visit1);
        StatLocation statLocation = statLocationRepository.getOne(key);
        assertThat(statLocation.getBackwardVisits()).as("back visits").isEqualTo(3l);
        assertThat(statLocation.getForwardVisits()).as("forward visits").isEqualTo(1l);
    }

    @Test
    void should_get_new_period_when_scantimes_are_in_different_stat_slot() {
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
    void should_get_new_context_when_different_venue_category2() {
        Visit visit1 = defaultVisit().toBuilder().venueCategory2(1).build(),
                visit2 = defaultVisit().toBuilder().venueCategory2(2).build();

        service.logStats(visit1);
        service.logStats(visit2);

        assertThat(statLocationRepository.count()).isEqualTo(2);
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
        var response = reportStatRepository.findAll().stream().findFirst().get();
        assertThat(response.getId()).isInstanceOf(String.class);
        assertThat(response.getId()).isNotNull();
        assertThat(response.getId()).isNotBlank();
        assertThat(response.getId()).isNotEmpty();
        assertThat(response.getReported()).isEqualTo(10);
        assertThat(response.getRejected()).isEqualTo(2);
        assertThat(response.getBackwards()).isEqualTo(5);
        assertThat(response.getForwards()).isEqualTo(3);
        assertThat(response.getClose()).isEqualTo(4);
        assertThat(response.getTimestamp().truncatedTo(ChronoUnit.SECONDS))
                .isEqualTo(TimeUtils.instantFromTimestamp(timestamp).truncatedTo(ChronoUnit.SECONDS));
    }
}
