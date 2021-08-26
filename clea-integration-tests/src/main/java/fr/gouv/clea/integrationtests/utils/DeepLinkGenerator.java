package fr.gouv.clea.integrationtests.utils;

import fr.inria.clea.lsp.Location;
import lombok.*;

import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static lombok.AccessLevel.PRIVATE;

@RequiredArgsConstructor
public class DeepLinkGenerator {

    private final Map<Period, URL> generatedDeepLinks = new HashMap<>();

    private final Location location;

    public URL getDeepLink(Instant instant) {
        final var periodDuration = Duration.ofHours(location.getLocationSpecificPart().getPeriodDuration());
        final var renewalInterval = Duration.ofSeconds(location.getLocationSpecificPart().getQrCodeRenewalInterval());
        final var period = Period.ofInstant(instant, renewalInterval);
        return generatedDeepLinks.computeIfAbsent(period, this::generatedDeepLink);
    }

    @SneakyThrows
    private URL generatedDeepLink(Period period) {
        final var locationStartTime = location.getContact().getPeriodStartTime();
        final var deepLink = location.newDeepLink(period.startTime, locationStartTime);
        return new URL(deepLink);
    }

    @Value
    @AllArgsConstructor(access = PRIVATE)
    private static class Period {

        Instant startTime;

        Duration duration;

        public static Period ofInstant(Instant instant, Duration renewalInterval) {
            // Assumption : All Periods are contiguous, making auto period-creation easier.
            // This is not the real-world case. Could emulate how real-world will be done
            // using a period duration such as periodDuration%24 == 0.
            final var secondsToRemove = instant.getEpochSecond() % renewalInterval.getSeconds();
            final var periodStart = instant.minusSeconds(secondsToRemove);
            return new Period(periodStart, renewalInterval);
        }

        public boolean contains(Instant instant) {
            return !(instant.isBefore(startTime) || instant.isAfter(startTime.plus(duration)));
        }
    }
}
