package fr.gouv.clea.integrationtests.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;

import static fr.inria.clea.lsp.utils.TimeUtils.SECONDS_FROM_01_01_1900_TO_01_01_1970;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClusterExposition {

    @JsonProperty("s")
    private long startTimeAsNtpTimestamp;

    @JsonProperty("d")
    private int durationInSeconds;

    @JsonProperty("r")
    private float risk;

    public boolean isInExposition(final Instant instant) {
        Instant startTime = Instant.ofEpochSecond(startTimeAsNtpTimestamp - SECONDS_FROM_01_01_1900_TO_01_01_1970);
        long delta = Duration.between(startTime, instant).toSeconds();
        return (delta >= 0 && delta <= durationInSeconds);
    }
}
