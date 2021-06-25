package fr.gouv.clea.integrationtests.model;

import lombok.*;

@Value
@With
@Builder
@AllArgsConstructor
public class Visit {

    String deepLinkExtractedInformation;

    Long scanTime;
}
