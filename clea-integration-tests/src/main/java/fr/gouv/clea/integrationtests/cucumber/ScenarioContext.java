package fr.gouv.clea.integrationtests.cucumber;

import fr.gouv.clea.integrationtests.config.ApplicationProperties;
import fr.gouv.clea.integrationtests.service.ClusterExpositionService;
import fr.gouv.clea.integrationtests.utils.DeepLinkGenerator;
import fr.gouv.clea.integrationtests.service.visitorsimulator.Visitor;
import fr.gouv.clea.integrationtests.utils.LocationBuilder;
import fr.gouv.clea.qr.LocationQrCodeGenerator;
import fr.inria.clea.lsp.Location;
import fr.inria.clea.lsp.exception.CleaCryptoException;
import io.cucumber.spring.ScenarioScope;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

@Component
@ScenarioScope
public class ScenarioContext {

    private final Map<String, Visitor> visitors = new HashMap<>();

    private final Map<String, DeepLinkGenerator> locations = new HashMap<>();

    private final ApplicationProperties applicationProperties;

    private final ClusterExpositionService clusterExpositionService;

    public ScenarioContext(final ApplicationProperties applicationProperties,
            final ClusterExpositionService clusterExpositionService) {
        this.applicationProperties = applicationProperties;
        this.clusterExpositionService = clusterExpositionService;
    }

    public Visitor getOrCreateUser(final String name) {
        return visitors.computeIfAbsent(name, this::createVisitor);
    }

    private Visitor createVisitor(final String name) {
        return new Visitor(name, clusterExpositionService);
    }

    public Visitor getVisitor(final String visitorName) {
        return visitors.get(visitorName);
    }

    public void registerLocation(String locationName, Location location) {
        locations.put(locationName, new DeepLinkGenerator(location));
    }

    public DeepLinkGenerator getLocation(String locationName) {
       return locations.get(locationName);
    }
}
