package fr.gouv.clea.integrationtests.cucumber;

import fr.gouv.clea.integrationtests.config.ApplicationProperties;
import fr.gouv.clea.integrationtests.model.Place;
import fr.gouv.clea.integrationtests.service.ClusterExpositionService;
import fr.gouv.clea.integrationtests.service.visitorsimulator.Visitor;
import fr.inria.clea.lsp.Location;
import io.cucumber.spring.ScenarioScope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.Optional.ofNullable;

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
            final int venueType,
            final int venueCategory1,
            final int venueCategory2,
            final int periodDurationHours) {
        return places.put(
                placeName, new Place(
                        createStaticLocation(
                                venueType,
                                venueCategory1,
                                venueCategory2
                        ),
                        createStaticStaffLocation(
                                venueType,
                                venueCategory1,
                                venueCategory2,
                                periodDurationHours
                        )
                )
        );
    }

    public Place createDynamicPlace(final String placeName,
            final int venueType,
            final int venueCategory1,
            final int venueCategory2,
            final Duration qrCodeRenewalInterval,
            final int periodDuration) {
        return places.put(
                placeName, new Place(
                        createDynamicLocation(
                                venueType,
                                venueCategory1,
                                venueCategory2,
                                qrCodeRenewalInterval,
                                periodDuration
                        ),
                        createDynamicStaffLocation(
                                venueType,
                                venueCategory1,
                                venueCategory2,
                                qrCodeRenewalInterval,
                                periodDuration
                        )
                )
        );
    }

    public Optional<Place> getPlace(final String placeName) {
        return ofNullable(places.get(placeName));
    }

    private Location createDynamicLocation(final int venueType,
            final int venueCategory1,
            final int venueCategory2,
            final Duration qrCodeRenewalInterval,
            final int periodDuration) {
        return locationFactory.createDynamicLocation(
                venueType,
                venueCategory1,
                venueCategory2,
                qrCodeRenewalInterval,
                periodDuration
        );
    }

    private Location createDynamicStaffLocation(final int venueType,
            final int venueCategory1,
            final int venueCategory2,
            final Duration qrCodeRenewalInterval,
            final int periodDuration) {
        return locationFactory.createDynamicStaffLocation(
                venueType,
                venueCategory1,
                venueCategory2,
                qrCodeRenewalInterval,
                periodDuration
        );
    }

    private Location createStaticLocation(final int venueType,
            final int venueCategory1,
            final int venueCategory2) {
        return locationFactory.createStaticLocation(
                venueType,
                venueCategory1,
                venueCategory2
        );
    }

    private Location createStaticStaffLocation(final int venueType,
            final int venueCategory1,
            final int venueCategory2,
            final int periodDuration) {
        return locationFactory.createStaticStaffLocation(
                venueType,
                venueCategory1,
                venueCategory2,
                periodDuration
        );
    }

}
