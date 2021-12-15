package fr.gouv.clea.integrationtests.cucumber;

import fr.gouv.clea.integrationtests.config.ApplicationProperties;
import fr.gouv.clea.integrationtests.cucumber.model.Place;
import fr.gouv.clea.integrationtests.service.ClusterExpositionService;
import fr.gouv.clea.integrationtests.service.visitorsimulator.Visitor;
import io.cucumber.spring.ScenarioScope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@ScenarioScope
@RequiredArgsConstructor
public class ScenarioContext {

    private final Map<String, Visitor> visitors = new HashMap<>();

    private final Map<String, Place> places = new HashMap<>();

    private final ApplicationProperties applicationProperties;

    private final ClusterExpositionService clusterExpositionService;

    public Visitor getOrCreateUser(final String name) {
        return visitors.computeIfAbsent(name, this::createVisitor);
    }

    private Visitor createVisitor(final String name) {
        return new Visitor(name, clusterExpositionService, applicationProperties);
    }

    public Visitor getVisitor(final String visitorName) {
        return visitors.get(visitorName);
    }

    public void createPlace(String placeName, LocationFactory locationFactory) {
        places.put(placeName, new Place(locationFactory));
    }

    public Place getPlace(final String placeName) {
        return places.get(placeName);
    }
}
