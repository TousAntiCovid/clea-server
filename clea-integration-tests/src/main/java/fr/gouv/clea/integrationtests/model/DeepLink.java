package fr.gouv.clea.integrationtests.model;

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
}
