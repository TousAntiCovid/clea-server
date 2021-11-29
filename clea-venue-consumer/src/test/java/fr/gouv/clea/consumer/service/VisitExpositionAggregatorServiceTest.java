package fr.gouv.clea.consumer.service;

import fr.gouv.clea.consumer.model.Visit;
import fr.gouv.clea.consumer.repository.visits.ExposedVisitRepository;
import fr.gouv.clea.consumer.test.IntegrationTest;
import fr.inria.clea.lsp.utils.TimeUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class VisitExpositionAggregatorServiceTest {

    @Autowired
    private ExposedVisitRepository repository;

    @Autowired
    private VisitExpositionAggregatorService service;

    private Instant todayAtMidnight = Instant.now().truncatedTo(ChronoUnit.DAYS);;

    private Instant todayAt8am = todayAtMidnight.plus(8, ChronoUnit.HOURS);

    private UUID uuid = UUID.randomUUID();

    private byte[] locationTemporarySecretKey = RandomUtils.nextBytes(20);;

    private byte[] encryptedLocationContactMessage = RandomUtils.nextBytes(20);

    @Test
    @DisplayName("visits with no existing context should be saved in DB")
    void saveWithNoContext() {
        Visit visit = defaultVisit().toBuilder()
                .locationTemporaryPublicId(uuid)
                .isBackward(true)
                .build();

        service.updateExposureCount(visit, false);

        assertThat(repository.findAll())
                .allMatch(exposedVisit -> exposedVisit.getLocationTemporaryPublicId().equals(uuid), "has uuid" + uuid)
                .allMatch(exposedVisit -> exposedVisit.getBackwardVisits() == 1, "has 1 backward visits");
    }

    @Test
    @DisplayName("visits with existing context should be updated in DB")
    void updateWithExistingContext() {
        Visit visit = defaultVisit().toBuilder()
                .locationTemporaryPublicId(uuid)
                .isBackward(true)
                .build();
        service.updateExposureCount(visit, false);
        long before = repository.count();

        service.updateExposureCount(visit, false);

        long after = repository.count();
        assertThat(before).isEqualTo(after);

        assertThat(repository.findAll())
                .allMatch(exposedVisit -> exposedVisit.getLocationTemporaryPublicId().equals(uuid), "has uuid" + uuid)
                .allMatch(exposedVisit -> exposedVisit.getBackwardVisits() == 2, "has 2 backward visits");
    }

    @Test
    @DisplayName("new visits should be saved while existing be updated in DB")
    void mixedContext() {
        Visit visit = defaultVisit().toBuilder()
                .locationTemporaryPublicId(uuid)
                .isBackward(true)
                .build();
        service.updateExposureCount(visit, false);
        visit.setBackward(false);
        UUID newUUID = UUID.randomUUID();
        Visit visit2 = visit.toBuilder()
                .locationTemporaryPublicId(newUUID)
                .isBackward(true)
                .build();

        service.updateExposureCount(visit, false);
        service.updateExposureCount(visit2, false);

        assertThat(repository.findAll())
                .filteredOn(exposedVisit -> exposedVisit.getLocationTemporaryPublicId().equals(uuid))
                .allMatch(exposedVisit -> exposedVisit.getBackwardVisits() == 1, "has 1 backward visit")
                .allMatch(exposedVisit -> exposedVisit.getForwardVisits() == 1, "has 1 forward visit");

        assertThat(repository.findAll())
                .filteredOn(exposedVisit -> exposedVisit.getLocationTemporaryPublicId().equals(newUUID))
                .allMatch(exposedVisit -> exposedVisit.getBackwardVisits() == 1, "has 1 backward visit")
                .allMatch(exposedVisit -> exposedVisit.getForwardVisits() == 0, "has 0 forward visit");
    }

    @Test
    @DisplayName("manually declared cluster with no existing context should be saved in DB ")
    void saveManuallyDeclaredClusterWithNoContext() {
        Visit visit = defaultVisit().toBuilder()
                .locationTemporaryPublicId(uuid)
                .isBackward(false)
                .build();

        service.updateExposureCount(visit, true);

        assertThat(repository.findAll())
                .allMatch(exposedVisit -> exposedVisit.getLocationTemporaryPublicId().equals(uuid), "has uuid" + uuid)
                .allMatch(exposedVisit -> exposedVisit.getForwardVisits() == 3, "has 3 backward visits");
    }

    @Test
    @DisplayName("stop processing if qrCodeScanTime is before periodStartTime")
    void testWhenQrScanIsBeforePeriodStart() {
        Instant todayAtMidnight = Instant.now().truncatedTo(ChronoUnit.DAYS);
        Instant todayAt8am = todayAtMidnight.plus(8, ChronoUnit.HOURS);
        Visit visit = defaultVisit().toBuilder()
                .periodDuration(24)
                .compressedPeriodStartTime(getCompressedPeriodStartTime(todayAt8am))
                .qrCodeValidityStartTime(todayAt8am)
                .qrCodeScanTime(todayAtMidnight)
                .build();

        service.updateExposureCount(visit, false);

        assertThat(repository.count()).isZero();
    }

    protected Visit defaultVisit() {
        return Visit.builder()
                .version(0)
                .type(0)
                .staff(true)
                .locationTemporaryPublicId(uuid)
                .qrCodeRenewalIntervalExponentCompact(2)
                .venueType(4)
                .venueCategory1(1)
                .venueCategory2(1)
                .periodDuration(24)
                .compressedPeriodStartTime(getCompressedPeriodStartTime(todayAtMidnight))
                .qrCodeValidityStartTime(Instant.now())
                .locationTemporarySecretKey(locationTemporarySecretKey)
                .encryptedLocationContactMessage(encryptedLocationContactMessage)
                .qrCodeScanTime(todayAt8am)
                .isBackward(true)
                .build();
    }

    protected int getCompressedPeriodStartTime(Instant instant) {
        return (int) (TimeUtils.ntpTimestampFromInstant(instant) / 3600);
    }
}
