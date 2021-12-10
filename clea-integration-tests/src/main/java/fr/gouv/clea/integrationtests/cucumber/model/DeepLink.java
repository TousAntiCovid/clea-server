package fr.gouv.clea.integrationtests.cucumber.model;

import lombok.Builder;
import lombok.Value;

import java.net.URL;
import java.time.Duration;
import java.time.Instant;

@Value
@Builder
public class DeepLink {

    URL url;

    Instant validityStartTime;

    Duration renewalInterval;

    public boolean containsProvidedScanTime(final Instant scanTime) {
        if (isStaticDeeplink()) {
            return scanTime.isAfter(validityStartTime) || scanTime.equals(validityStartTime);
        } else {
            final var validityEndTime = validityStartTime.plus(renewalInterval);
            return (scanTime.isAfter(validityStartTime) || scanTime.equals(validityStartTime))
                    && (scanTime.isBefore(validityEndTime) || scanTime.equals(validityEndTime));
        }
    }

    private boolean isStaticDeeplink() {
        return renewalInterval.abs().isZero();
    }
}
