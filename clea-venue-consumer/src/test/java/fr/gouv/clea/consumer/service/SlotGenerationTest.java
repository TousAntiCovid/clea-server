package fr.gouv.clea.consumer.service;

import fr.gouv.clea.consumer.configuration.VenueConsumerProperties;
import fr.gouv.clea.consumer.model.ExposedVisitEntity;
import fr.gouv.clea.consumer.model.Visit;
import fr.gouv.clea.consumer.repository.visits.ExposedVisitRepository;
import fr.gouv.clea.scoring.configuration.ScoringRule;
import fr.gouv.clea.scoring.configuration.exposure.ExposureTimeConfiguration;
import fr.gouv.clea.scoring.configuration.exposure.ExposureTimeRule;
import fr.inria.clea.lsp.utils.TimeUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SlotGenerationTest {

    private static final Instant TODAY_AT_MIDNIGHT = Instant.now().truncatedTo(DAYS);

    private static final Instant TODAY_AT_8AM = TODAY_AT_MIDNIGHT.plus(8, HOURS);

    private final VenueConsumerProperties properties = VenueConsumerProperties.builder().build();

    private final ExposureTimeConfiguration exposureTimeConfig = new ExposureTimeConfiguration();

    @Mock
    private ExposedVisitRepository repository;

    @Mock
    private StatisticsService statisticsService;

    @Captor
    private ArgumentCaptor<List<ExposedVisitEntity>> exposedVisitEntitiesCaptor;

    private VisitExpositionAggregatorService service;

    @BeforeEach
    void init() {
        exposureTimeConfig.setRules(
                List.of(
                        ExposureTimeRule.builder()
                                .venueType(ScoringRule.WILDCARD_VALUE)
                                .venueCategory1(ScoringRule.WILDCARD_VALUE)
                                .venueCategory2(ScoringRule.WILDCARD_VALUE)
                                .exposureTimeBackward(3)
                                .exposureTimeForward(3)
                                .exposureTimeStaffBackward(3)
                                .exposureTimeStaffForward(3)
                                .build()
                )
        );
        properties.setDurationUnitInSeconds(Duration.ofMinutes(30).toSeconds());
        service = new VisitExpositionAggregatorService(
                repository,
                exposureTimeConfig, properties
        );
    }

    @Test
    void a_period_duration_of_24_hours_generates_5_slots() {
        Visit visit = defaultVisit().toBuilder()
                .periodDuration(24)
                .compressedPeriodStartTime(getCompressedPeriodStartTime(TODAY_AT_MIDNIGHT))
                .qrCodeValidityStartTime(TODAY_AT_MIDNIGHT)
                .qrCodeScanTime(TODAY_AT_8AM)
                .build();

        service.updateExposureCount(visit, false);

        // => scanTimeSlot = 8*2 = 16
        // => slots to generate = 3 before + scanTimeSlot + 3 after = 7
        // => firstExposedSlot = 16-3 = 13
        // => lastExposedSlot = 16+3 = 19
        verify(repository).saveAll(exposedVisitEntitiesCaptor.capture());
        assertThat(exposedVisitEntitiesCaptor.getValue())
                .extracting(ExposedVisitEntity::getTimeSlot)
                .containsExactly(13, 14, 15, 16, 17, 18, 19);
    }

    @Test
    void a_period_duration_of_1_hour_generates_2_slots() {
        Visit visit = defaultVisit().toBuilder()
                .periodDuration(1)
                .compressedPeriodStartTime(getCompressedPeriodStartTime(TODAY_AT_8AM))
                .qrCodeValidityStartTime(TODAY_AT_8AM)
                .qrCodeScanTime(TODAY_AT_8AM)
                .build();

        service.updateExposureCount(visit, false);

        // => scanTimeSlot = 0
        // => slots to generate = scanTimeSlot + 1 after = 2
        // => firstExposedSlot = 0
        // => lastExposedSlot = 0+1 = 1
        verify(repository).saveAll(exposedVisitEntitiesCaptor.capture());
        assertThat(exposedVisitEntitiesCaptor.getValue())
                .extracting(ExposedVisitEntity::getTimeSlot)
                .containsExactly(0, 1);
    }

    @Test
    void a_visit_at_first_slot_with_an_unlimited_period_duration_generates_3_slots() {
        Visit visit = defaultVisit().toBuilder()
                .periodDuration(255)
                .compressedPeriodStartTime(getCompressedPeriodStartTime(TODAY_AT_8AM))
                .qrCodeValidityStartTime(TODAY_AT_8AM)
                .qrCodeScanTime(TODAY_AT_8AM)
                .build();

        service.updateExposureCount(visit, false);

        // => scanTimeSlot = 0
        // => slots to generate = scanTimeSlot + 3 after = 4
        // => firstExposedSlot = 0
        // => lastExposedSlot = 0+4-1 = 3
        verify(repository).saveAll(exposedVisitEntitiesCaptor.capture());
        assertThat(exposedVisitEntitiesCaptor.getValue())
                .extracting(ExposedVisitEntity::getTimeSlot)
                .containsExactly(0, 1, 2, 3);
    }

    @Test
    void a_qrScanTime_after_period_validity_doesnt_generate_slots() {
        Visit visit = defaultVisit().toBuilder()
                .compressedPeriodStartTime(getCompressedPeriodStartTime(TODAY_AT_MIDNIGHT))
                .periodDuration(6)
                .qrCodeValidityStartTime(TODAY_AT_MIDNIGHT)
                .qrCodeScanTime(TODAY_AT_8AM)
                .build();

        service.updateExposureCount(visit, false);

        verify(repository, never()).saveAll(exposedVisitEntitiesCaptor.capture());
    }

    @Test
    void a_visit_at_first_slot_when_qrScanTime_is_after_qr_validity_generates_5_slots() {
        // This case can happen with authorized drift
        properties.setDurationUnitInSeconds(Duration.ofHours(1).toSeconds());
        Visit visit = defaultVisit().toBuilder()
                .compressedPeriodStartTime(getCompressedPeriodStartTime(TODAY_AT_MIDNIGHT))
                .periodDuration(24)
                .qrCodeValidityStartTime(TODAY_AT_MIDNIGHT)
                .qrCodeRenewalIntervalExponentCompact(14) // 2^14 seconds = 4.55 hours
                .qrCodeScanTime(TODAY_AT_8AM)
                .build();

        service.updateExposureCount(visit, false);

        // => scanTimeSlot = 8
        // => slots to generate = scanTimeSlot + 3 after + 3 before
        // => firstExposedSlot = 11
        // => lastExposedSlot = 5
        verify(repository).saveAll(exposedVisitEntitiesCaptor.capture());
        assertThat(exposedVisitEntitiesCaptor.getValue())
                .extracting(ExposedVisitEntity::getTimeSlot)
                .containsExactly(5, 6, 7, 8, 9, 10, 11);
    }

    protected Visit defaultVisit() {
        return Visit.builder()
                .version(0)
                .type(0)
                .staff(true)
                .locationTemporaryPublicId(UUID.randomUUID())
                .qrCodeRenewalIntervalExponentCompact(2)
                .venueType(4)
                .venueCategory1(1)
                .venueCategory2(1)
                .periodDuration(24)
                .compressedPeriodStartTime(getCompressedPeriodStartTime(TODAY_AT_MIDNIGHT))
                .qrCodeValidityStartTime(Instant.now())
                .locationTemporarySecretKey(RandomUtils.nextBytes(20))
                .encryptedLocationContactMessage(RandomUtils.nextBytes(20))
                .qrCodeScanTime(TODAY_AT_8AM)
                .isBackward(true)
                .build();
    }

    protected int getCompressedPeriodStartTime(Instant instant) {
        return (int) (TimeUtils.ntpTimestampFromInstant(instant) / 3600);
    }

}
