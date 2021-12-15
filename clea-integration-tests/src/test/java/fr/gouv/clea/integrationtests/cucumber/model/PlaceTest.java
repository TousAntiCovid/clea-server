package fr.gouv.clea.integrationtests.cucumber.model;

import fr.gouv.clea.integrationtests.cucumber.LocationFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PlaceTest {

    Place staticPlace;

    Place dynamicPlace;

    @BeforeEach
    void setup() {
        final var publicKey = "02c3a58bf668fa3fe2fc206152abd6d8d55102adfee68c8b227676d1fe763f5a06";
        final var startTime = Instant.parse("2019-12-01T12:00:00Z");
        final var staticLocation = LocationFactory.builder(publicKey)
                .startTime(startTime)
                .venueConfig(1, 1, 1);
        staticPlace = new Place(staticLocation);

        final var dynamicLocation = LocationFactory.builder(publicKey)
                .startTime(startTime)
                .periodDurationHours(1)
                .renewalIntervalSeconds(512)
                .venueConfig(1, 1, 1);
        dynamicPlace = new Place(dynamicLocation);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "2019-12-01T12:00:00Z",
            "2019-12-02T17:13:52Z",
            "2022-07-15T21:41:03Z",
            "2021-03-01T02:36:22Z"
    })
    void a_static_place_always_return_the_same_deeplink(final String datetime) {
        final var firstDeeplink = staticPlace.getDeepLinkAt(Instant.parse("2019-12-01T12:00:00Z"));
        final var someDeeplink = staticPlace.getDeepLinkAt(Instant.parse(datetime));
        assertThat(someDeeplink).isEqualTo(firstDeeplink);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "2019-12-01T12:00:00Z",
            "2019-12-01T12:00:01Z",
            "2019-12-01T12:08:22Z",
            "2019-12-01T12:08:32Z"
    })
    void a_dynamic_place_always_return_the_same_deeplink_for_the_same_period(final String samePeriodDatetime) {
        final var firstDeeplink = dynamicPlace.getDeepLinkAt(Instant.parse("2019-12-01T12:00:00Z"));
        final var someDeeplink = dynamicPlace.getDeepLinkAt(Instant.parse(samePeriodDatetime));
        assertThat(someDeeplink).isEqualTo(firstDeeplink);
    }

    @ParameterizedTest
    @CsvSource({
            "2019-12-01T12:00:00Z,2020-12-01T12:00:00Z",
            "2020-01-01T03:44:55Z,2020-01-01T05:44:55Z",
            "2020-06-20T14:00:00Z,2020-06-20T14:08:33Z"
    })
    void a_dynamic_place_returns_a_new_deeplink_for_two_different_periods(String firstTime, String secondTime) {
        final var firstDeeplink = dynamicPlace.getDeepLinkAt(Instant.parse(firstTime));
        final var someDeeplink = dynamicPlace.getDeepLinkAt(Instant.parse(secondTime));
        assertThat(someDeeplink).isNotEqualTo(firstDeeplink);
    }
}
