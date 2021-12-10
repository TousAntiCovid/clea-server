package fr.gouv.clea.integrationtests.cucumber;

import fr.gouv.clea.integrationtests.config.ApplicationProperties;
import fr.gouv.clea.integrationtests.cucumber.model.Place;
import fr.gouv.clea.integrationtests.service.ClusterExpositionService;
import fr.gouv.clea.integrationtests.service.visitorsimulator.Visitor;
import io.cucumber.spring.ScenarioScope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static java.util.UUID.randomUUID;
import static org.bouncycastle.util.encoders.Hex.toHexString;

@Slf4j
@Component
@ScenarioScope
@RequiredArgsConstructor
public class ScenarioContext {

    private final Map<String, Visitor> visitors = new HashMap<>();

    private final Map<String, Place> places = new HashMap<>();

    private final ApplicationProperties applicationProperties;

    private final ClusterExpositionService clusterExpositionService;

    private final LocationFactory locationFactory;

    public Visitor getOrCreateUser(final String name) {
        return visitors.computeIfAbsent(name, this::createVisitor);
    }

    private Visitor createVisitor(final String name) {
        return new Visitor(name, clusterExpositionService, applicationProperties);
    }

    public Visitor getVisitor(final String visitorName) {
        return visitors.get(visitorName);
    }

    public Place createStaticPlace(final String placeName,
            final Instant deepLinkStartTime,
            final int venueType,
            final int venueCategory1,
            final int venueCategory2) {
        final var permanentLocationSecretKey = toHexString(randomUUID().toString().getBytes());
        final var place = new Place(
                locationFactory.createStaticLocation(
                        deepLinkStartTime,
                        venueType,
                        venueCategory1,
                        venueCategory2,
                        permanentLocationSecretKey
                ),
                locationFactory.createStaticStaffLocation(
                        deepLinkStartTime,
                        venueType,
                        venueCategory1,
                        venueCategory2,
                        permanentLocationSecretKey
                )
        );
        places.put(
                placeName, place
        );
        return place;
    }

    public Place createDynamicPlace(final String placeName,
            final Instant dynamicDeepLinkStartTime,
            final int venueType,
            final int venueCategory1,
            final int venueCategory2,
            final Duration deepLinkRenewalInterval,
            final int periodDuration) {
        final var permanentLocationSecretKey = toHexString(randomUUID().toString().getBytes());
        return places.put(
                placeName, new Place(
                        locationFactory.createDynamicLocation(
                                dynamicDeepLinkStartTime,
                                venueType,
                                venueCategory1,
                                venueCategory2,
                                deepLinkRenewalInterval,
                                periodDuration,
                                permanentLocationSecretKey
                        ),
                        locationFactory.createDynamicStaffLocation(
                                dynamicDeepLinkStartTime,
                                venueType,
                                venueCategory1,
                                venueCategory2,
                                deepLinkRenewalInterval,
                                periodDuration,
                                permanentLocationSecretKey
                        )
                )
        );
    }

    public Place getPlace(final String placeName) {
        return places.get(placeName);
    }
}
