package fr.gouv.clea.integrationtests.feature.context;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class VenueConfiguration {

    int venueType;

    int venueCategory1;

    int venueCategory2;

    static VenueConfiguration DEFAULT = new VenueConfiguration(9, 9, 0);
}
