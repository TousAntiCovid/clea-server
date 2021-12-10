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

    Instant startTime;

    Duration validity;

    public boolean containsProvidedScanTime(final Instant scanTime) {
        final var validityEndTime = startTime.plus(validity);
        return (scanTime.isAfter(startTime) || scanTime.equals(startTime))
                && (scanTime.isBefore(validityEndTime) || scanTime.equals(validityEndTime));
    }
}
