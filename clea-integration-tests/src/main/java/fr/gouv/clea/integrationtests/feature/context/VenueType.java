package fr.gouv.clea.integrationtests.feature.context;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.stream.Collectors;

import static java.util.Arrays.stream;
import static java.util.Optional.of;

@Slf4j
@Getter
@RequiredArgsConstructor
public enum VenueType {

    RESTAURANT("restaurant", 1),
    NIGHTCLUB("discotheque", 2),
    SPORTS_PLACE("etablissement sportif", 4),
    DEFAULT("défaut", 9);

    private final String name;

    private final int value;

    public static VenueType valueFromName(final String name) {
        return of(
                stream(VenueType.values())
                        .filter(venueType -> venueType.name.equals(name))
                        .collect(Collectors.toList())
        )
                .filter(list -> list.size() == 1)
                .map(list -> list.get(0))
                .orElseGet(() -> {
                    log.warn("multiple venueTypes with same name configured, please double check configuration.");
                    return DEFAULT;
                });
    }
}
