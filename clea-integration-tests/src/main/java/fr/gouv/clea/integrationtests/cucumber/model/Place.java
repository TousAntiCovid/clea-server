package fr.gouv.clea.integrationtests.cucumber.model;

import fr.gouv.clea.integrationtests.cucumber.LocationFactory;
import fr.inria.clea.lsp.Location;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Place {

    private static final Duration MAX_DURATION = Duration.ofNanos(Long.MAX_VALUE);

    private final Instant startTime;

    private final DeepLinks visitorDeepLinks;

    private final DeepLinks staffDeepLinks;

    public Place(final LocationFactory locationFactory) {
        this.startTime = locationFactory.getStartTime();
        this.visitorDeepLinks = new DeepLinks(locationFactory.buildVisitor());
        this.staffDeepLinks = new DeepLinks(locationFactory.buildStaff());
    }

    public DeepLink getDeepLinkAt(final Instant scanTime) {
        return visitorDeepLinks.get(scanTime);
    }

    public DeepLink getStaffDeepLinkAt(final Instant scanTime) {
        return staffDeepLinks.get(scanTime);
    }

    @RequiredArgsConstructor
    private class DeepLinks {

        private final List<DeepLink> deepLinks = new ArrayList<>();

        private final Location location;

        public DeepLink get(final Instant scanTime) {
            return deepLinks.stream()
                    .filter(deeplink -> deeplink.contains(scanTime))
                    .findFirst()
                    .orElseGet(() -> {
                        final var deepLink = createDeepLink(scanTime);
                        deepLinks.add(deepLink);
                        return deepLink;
                    });
        }

        @SneakyThrows
        private DeepLink createDeepLink(final Instant scanTime) {
            if (isStaticLocation()) {
                return DeepLink.builder()
                        .url(new URL(location.newDeepLink(startTime)))
                        .startTime(startTime)
                        .duration(MAX_DURATION)
                        .build();
            } else {
                // find out in which period the scan occurs
                // the period defines the ltid included in the deeplink
                final var ltidPeriodDuration = Duration.ofHours(location.getLocationSpecificPart().getPeriodDuration());
                final var ltidPeriodCount = Duration.between(startTime, scanTime).dividedBy(ltidPeriodDuration);
                final var ltidPeriodStart = startTime.plusSeconds(ltidPeriodCount * ltidPeriodDuration.getSeconds());

                // find out in which renewal occurrence the scan occurs in the current period
                // some random data is used to randomize the deeplink every 'renewal' interval
                final var renewalDuration = Duration
                        .ofSeconds(location.getLocationSpecificPart().getQrCodeRenewalInterval());
                final var renewalCount = Duration.between(ltidPeriodStart, scanTime).dividedBy(renewalDuration);
                final var renewalStart = ltidPeriodStart.plusSeconds(renewalCount * renewalDuration.getSeconds());

                return DeepLink.builder()
                        .url(new URL(location.newDeepLink(ltidPeriodStart, renewalStart)))
                        .startTime(renewalStart)
                        .duration(renewalDuration)
                        .build();
            }
        }

        private boolean isStaticLocation() {
            return location.getLocationSpecificPart().getQrCodeRenewalInterval() == 0;
        }
    }
}
