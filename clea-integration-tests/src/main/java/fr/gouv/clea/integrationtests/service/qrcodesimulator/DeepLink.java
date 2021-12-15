package fr.gouv.clea.integrationtests.service.qrcodesimulator;

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

    Duration duration;

    public boolean contains(final Instant scanTime) {
        final var validityEndTime = startTime.plus(duration);
        return (scanTime.isAfter(startTime) || scanTime.equals(startTime))
                && (scanTime.isBefore(validityEndTime) || scanTime.equals(validityEndTime));
    }
}
