package fr.gouv.clea.integrationtests.feature.context;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class VenueConfiguration {

    VenueType venueType;

    int venueCategory1;

    int venueCategory2;

    static VenueConfiguration DEFAULT = new VenueConfiguration(VenueType.DEFAULT, 9, 0);
}
