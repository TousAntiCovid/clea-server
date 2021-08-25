package fr.gouv.clea.integrationtests.repository.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.inria.clea.lsp.utils.TimeUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

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

    public boolean affects(final Instant instant) {
        final var clusterStartTime = TimeUtils.instantFromTimestamp(startTimeAsNtpTimestamp);
        final var clusterEndTime = clusterStartTime.plusSeconds(durationInSeconds);
        return instant.equals(clusterStartTime)
                || instant.isAfter(clusterStartTime) && instant.isBefore(clusterEndTime)
                || instant.equals(clusterEndTime);
    }
}
