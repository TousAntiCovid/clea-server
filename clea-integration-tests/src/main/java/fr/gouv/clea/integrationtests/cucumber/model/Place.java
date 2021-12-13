package fr.gouv.clea.integrationtests.cucumber.model;

import fr.inria.clea.lsp.Location;
import fr.inria.clea.lsp.LocationSpecificPart;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Comparator.comparing;

@Value
@Slf4j
@RequiredArgsConstructor
public class Place {

    Location location;

    Location staffLocation;

    /**
     * Maps are ordered by start time so that the first deeplink is the one
     * providing the initial start time
     *
     * @see TreeMap#TreeMap(java.util.Comparator)
     */
    TreeMap<Period, List<DeepLink>> locationDeepLinks = new TreeMap<>(comparing(Period::getStartTime));

    TreeMap<Period, List<DeepLink>> staffLocationDeepLinks = new TreeMap<>(comparing(Period::getStartTime));

    private static final Duration MAX_DURATION = Duration.ofSeconds(
            Long.MAX_VALUE,
            999999999L
    );

    public DeepLink getDeepLinkAt(final Instant scanTime) {
        if (this.location.getLocationSpecificPart().getQrCodeRenewalInterval() == 0) {
            return locationDeepLinks.values().stream()
                    .flatMap(List::stream)
                    .filter(deepLink -> deepLink.getValidity() == MAX_DURATION)
                    .findFirst()
                    .orElseThrow();
        } else {
            return getDeepLink(scanTime, location, locationDeepLinks);
        }
    }

    public DeepLink getStaffDeepLinkAt(final Instant scanTime) {
        if (this.location.getLocationSpecificPart().getQrCodeRenewalInterval() == 0) {
            return staffLocationDeepLinks.values().stream()
                    .flatMap(List::stream)
                    .filter(deepLink -> deepLink.getValidity() == MAX_DURATION)
                    .findFirst()
                    .orElseThrow();
        } else {
            return getDeepLink(scanTime, staffLocation, staffLocationDeepLinks);
        }
    }

    @SneakyThrows
    public void createStaticDeepLink(final Instant periodStartTime) {
        locationDeepLinks.put(
                new Period(periodStartTime, MAX_DURATION),
                List.of(
                        DeepLink.builder()
                                .url(new URL(location.newDeepLink(periodStartTime)))
                                .startTime(periodStartTime)
                                .validity(MAX_DURATION)
                                .build()
                )
        );
    }

    @SneakyThrows
    public void createStaticStaffDeepLink(final Instant periodStartTime) {
        staffLocationDeepLinks.put(
                new Period(periodStartTime, MAX_DURATION),
                List.of(
                        DeepLink.builder()
                                .url(new URL(staffLocation.newDeepLink(periodStartTime)))
                                .startTime(periodStartTime)
                                .validity(MAX_DURATION)
                                .build()
                )
        );
    }

    private DeepLink getDeepLink(final Instant deepLinkScanTime,
            final Location location,
            final TreeMap<Period, List<DeepLink>> deepLinks) {
        final var locationSpecificPart = location.getLocationSpecificPart();
        final Period concernedPeriod = getDeepLinkPeriod(deepLinkScanTime, deepLinks, locationSpecificPart);

        deepLinks.computeIfAbsent(concernedPeriod, (p) -> new ArrayList<>());

        return deepLinks.get(concernedPeriod).stream()
                .filter(it -> it.containsProvidedScanTime(deepLinkScanTime))
                .findFirst()
                .orElseGet(() -> {
                    final var deepLink = createDeepLinkFrom(
                            deepLinkScanTime,
                            concernedPeriod,
                            locationSpecificPart.getQrCodeRenewalInterval()
                    );
                    deepLinks.get(concernedPeriod).add(deepLink);
                    return deepLink;
                }
                );
    }

    private Period getDeepLinkPeriod(final Instant deepLinkScanTime,
            final TreeMap<Period, List<DeepLink>> deepLinks,
            final LocationSpecificPart locationSpecificPart) {
        final var deepLinkInitialStartTime = locationSpecificPart.getQrCodeValidityStartTime();
        final var periodDurationInSeconds = locationSpecificPart.getPeriodDuration() * 3600L;
        final var secondsBetweenScanTimeAndInitialPeriodStartTime = Duration
                .between(deepLinkScanTime, deepLinkInitialStartTime)
                .abs().getSeconds();
        final var timeBetweenPeriodStartTimeAndScanTime = secondsBetweenScanTimeAndInitialPeriodStartTime
                % periodDurationInSeconds;
        final var instantPeriodStart = deepLinkScanTime.minus(
                timeBetweenPeriodStartTimeAndScanTime,
                ChronoUnit.SECONDS
        );

        return deepLinks.keySet()
                .stream()
                .filter(p -> p.contains(deepLinkScanTime))
                .findFirst()
                .orElseGet(() -> createNewPeriod(instantPeriodStart, locationSpecificPart.getPeriodDuration()));
    }

    private DeepLink createDeepLinkFrom(final Instant scanTime,
            final Period concernedPeriod,
            final int deepLinkRenewalInterval) {
        final var computedValidityStartTime = getValidityStartTime(
                scanTime, concernedPeriod, deepLinkRenewalInterval
        );
        return createDynamicDeepLink(
                concernedPeriod.getStartTime(),
                computedValidityStartTime,
                Duration.of(deepLinkRenewalInterval, SECONDS)
        );
    }

    private Instant getValidityStartTime(final Instant deepLinkScanTime,
            final Period concernedPeriod,
            final int deepLinkRenewalInterval) {
        final var secondsBetweenPeriodStartAndScanTime = Duration
                .between(deepLinkScanTime, concernedPeriod.getStartTime()).abs().getSeconds();
        final var timeBetweenLastRenewalIntervalAndScanTime = secondsBetweenPeriodStartAndScanTime
                % deepLinkRenewalInterval;
        return deepLinkScanTime.minus(
                timeBetweenLastRenewalIntervalAndScanTime,
                SECONDS
        );
    }

    private Period createNewPeriod(final Instant instantPeriodStart,
            final int periodDuration) {
        return new Period(
                instantPeriodStart, Duration.of(periodDuration, HOURS)
        );
    }

    @SneakyThrows
    private DeepLink createDynamicDeepLink(final Instant deepLinkPeriodStartTime,
            final Instant deepLinkValidityStartTime,
            final Duration deepLinkRenewalIntervalDuration) {
        return DeepLink.builder()
                .url(new URL(location.newDeepLink(deepLinkPeriodStartTime, deepLinkValidityStartTime)))
                .startTime(deepLinkPeriodStartTime)
                .validity(deepLinkRenewalIntervalDuration)
                .build();
    }
}
