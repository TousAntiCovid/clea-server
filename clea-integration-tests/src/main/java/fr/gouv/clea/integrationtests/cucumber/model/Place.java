package fr.gouv.clea.integrationtests.cucumber.model;

import fr.inria.clea.lsp.Location;
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

import static java.time.Duration.ZERO;
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

    public DeepLink getDeepLinkAt(final Instant scanTime) {
        if (this.location.getLocationSpecificPart().getQrCodeRenewalInterval() == 0) {
            return getStaticDeepLink();
        } else {
            return getOrCreateDynamicDeepLinkAt(scanTime);
        }
    }

    public DeepLink getStaffDeepLinkAt(final Instant scanTime) {
        if (this.location.getLocationSpecificPart().getQrCodeRenewalInterval() == 0) {
            return getStaticStaffDeepLink();
        } else {
            return getOrCreateStaffDynamicDeepLinkAt(scanTime);
        }
    }

    @SneakyThrows
    public void createStaticDeepLink(final Instant periodStartTime) {
        locationDeepLinks.put(
                new Period(periodStartTime, ZERO),
                List.of(
                        DeepLink.builder()
                                .url(new URL(location.newDeepLink(periodStartTime)))
                                .validityStartTime(periodStartTime)
                                .renewalInterval(ZERO)
                                .build()
                )
        );
    }

    @SneakyThrows
    public void createStaticStaffDeepLink(final Instant periodStartTime) {
        staffLocationDeepLinks.put(
                new Period(periodStartTime, ZERO),
                List.of(
                        DeepLink.builder()
                                .url(new URL(staffLocation.newDeepLink(periodStartTime)))
                                .validityStartTime(periodStartTime)
                                .renewalInterval(ZERO)
                                .build()
                )
        );
    }

    public void createDynamicDeepLinkAt(final Instant periodStartTime) {
        createDeepLinkAtPeriodStartTimeFrom(periodStartTime, location);
    }

    public void createDynamicStaffDeepLinkAt(final Instant periodStartTime) {
        createDeepLinkAtPeriodStartTimeFrom(periodStartTime, staffLocation);
    }

    @SneakyThrows
    private void createDeepLinkAtPeriodStartTimeFrom(final Instant periodStartTime, final Location location) {
        locationDeepLinks.put(
                new Period(periodStartTime, Duration.of(location.getLocationSpecificPart().getPeriodDuration(), HOURS)),
                List.of(
                        DeepLink.builder()
                                .url(new URL(location.newDeepLink(periodStartTime)))
                                .validityStartTime(periodStartTime)
                                .renewalInterval(
                                        Duration.of(
                                                location.getLocationSpecificPart().getQrCodeRenewalInterval(), SECONDS
                                        )
                                )
                                .build()
                )
        );
    }

    private DeepLink getStaticDeepLink() {
        return locationDeepLinks.values().stream()
                .flatMap(List::stream)
                .filter(deepLink -> deepLink.getRenewalInterval() == ZERO)
                .findFirst()
                .orElseThrow();
    }

    private DeepLink getStaticStaffDeepLink() {
        return staffLocationDeepLinks.values().stream()
                .flatMap(List::stream)
                .filter(deepLink -> deepLink.getRenewalInterval() == ZERO)
                .findFirst()
                .orElseThrow();
    }

    private DeepLink getOrCreateDynamicDeepLinkAt(final Instant deepLinkScanTime) {
        return getDeepLink(deepLinkScanTime, location, locationDeepLinks);
    }

    private DeepLink getOrCreateStaffDynamicDeepLinkAt(final Instant deepLinkScanTime) {
        return getDeepLink(deepLinkScanTime, staffLocation, staffLocationDeepLinks);
    }

    private DeepLink getDeepLink(final Instant deepLinkScanTime,
            final Location location,
            final TreeMap<Period, List<DeepLink>> deepLinks) {
        final var locationSpecificPart = location.getLocationSpecificPart();
        final var deepLinkInitialStartTime = getInitialStartTime(deepLinks);
        final var instantPeriodStart = computePeriodStart(
                deepLinkScanTime,
                locationSpecificPart.getPeriodDuration(),
                deepLinkInitialStartTime
        );
        final var concernedPeriod = getCorrespondingPeriod(
                deepLinkScanTime,
                instantPeriodStart,
                locationSpecificPart.getPeriodDuration()
        );

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

    private Instant getInitialStartTime(TreeMap<Period, List<DeepLink>> deepLinks) {
        return deepLinks.get(locationDeepLinks.firstKey()).get(0).getValidityStartTime();
    }

    private Instant computePeriodStart(Instant scanTime, int periodDuration, Instant initialPeriodStartTime) {
        final var periodDurationInSeconds = periodDuration * 3600L;
        final var secondsBetweenScanTimeAndInitialPeriodStartTime = Duration.between(scanTime, initialPeriodStartTime)
                .abs().getSeconds();
        final var timeBetweenPeriodStartTimeAndScanTime = secondsBetweenScanTimeAndInitialPeriodStartTime
                % periodDurationInSeconds;
        return scanTime.minus(
                timeBetweenPeriodStartTimeAndScanTime,
                ChronoUnit.SECONDS
        );
    }

    private Period getCorrespondingPeriod(final Instant scanTime,
            final Instant instantPeriodStart,
            final int periodDuration) {
        return locationDeepLinks.keySet()
                .stream()
                .filter(p -> p.containsInstant(scanTime))
                .findFirst()
                .orElseGet(() -> createNewPeriod(instantPeriodStart, periodDuration));
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
    private DeepLink createDynamicDeepLink(final Instant qrCodePeriodStartTime,
            final Instant qrCodeValidityStartTime,
            final Duration qrCodeRenewalIntervalDuration) {
        return DeepLink.builder()
                .url(new URL(location.newDeepLink(qrCodePeriodStartTime, qrCodeValidityStartTime)))
                .validityStartTime(qrCodePeriodStartTime)
                .renewalInterval(qrCodeRenewalIntervalDuration)
                .build();
    }
}
