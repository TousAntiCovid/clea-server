package fr.gouv.clea.integrationtests.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Duration;
import java.time.Instant;

@Data
@AllArgsConstructor
public class Period {

    private Instant startTime;

    private Duration duration;

    public boolean containsInstant(final Instant instant) {
        return !(instant.isBefore(startTime) || instant.isAfter(startTime.plus(duration)));
    }
}
