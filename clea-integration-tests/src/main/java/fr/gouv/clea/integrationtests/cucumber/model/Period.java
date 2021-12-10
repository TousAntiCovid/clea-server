package fr.gouv.clea.integrationtests.cucumber.model;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.time.Duration;
import java.time.Instant;

@Value
@AllArgsConstructor
public class Period {

    Instant startTime;

    Duration duration;

    public boolean contains(final Instant instant) {
        return !(instant.isBefore(startTime) || instant.isAfter(startTime.plus(duration)));
    }
}
