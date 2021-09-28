package fr.gouv.clea.ws.service.impl;

import fr.gouv.clea.ws.model.DecodedVisit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class VisitsInSameCounterTest {

    private static final Duration EXPOSURE_TIME_UNIT = Duration.ofSeconds(1800);

    private static final Instant NOW = Instant.now();

    private VisitsInSameUnitCounter counter;

    @BeforeEach
    void setupCounter() {
        counter = new VisitsInSameUnitCounter(EXPOSURE_TIME_UNIT.getSeconds());
    }

    @Test
    void one_single_visit_always_generates_a_zero_count() {
        // given no visit occurred before

        // when a visit occurs now
        counter.incrementIfScannedInSameTimeUnitThanLastScanTime(decodedVisitWithScanTime(NOW));

        // then the count of visits in same time unit is 0
        assertThat(counter.getCount()).isEqualTo(0);
    }

    @Test
    void can_count_two_successive_visits_in_less_than_exposure_time() {
        // given a visit occurred 1799 seconds ago
        final var lastScanTime = NOW.minus(EXPOSURE_TIME_UNIT.minus(1, SECONDS));
        counter.incrementIfScannedInSameTimeUnitThanLastScanTime(decodedVisitWithScanTime(lastScanTime));

        // when another visit occurs now
        counter.incrementIfScannedInSameTimeUnitThanLastScanTime(decodedVisitWithScanTime(NOW));

        // then the count of visits in same time unit is 1
        assertThat(counter.getCount()).isEqualTo(1);
    }

    @Test
    void ignore_visits_in_future_even_if_scan_times_occured_in_less_than_exposure_time_unit() {
        // given a visit occurred 1799 seconds in the future
        final Instant lastScanTime = Instant.now().minus(EXPOSURE_TIME_UNIT.plus(1, SECONDS));
        counter.incrementIfScannedInSameTimeUnitThanLastScanTime(decodedVisitWithScanTime(lastScanTime));

        // when another visit occurs now
        counter.incrementIfScannedInSameTimeUnitThanLastScanTime(decodedVisitWithScanTime(NOW));

        // then the count of visits in same time unit is 0
        assertThat(counter.getCount()).isEqualTo(0);
    }

    private DecodedVisit decodedVisitWithScanTime(Instant scanTime) {
        return new DecodedVisit(scanTime, null, true);
    }
}
