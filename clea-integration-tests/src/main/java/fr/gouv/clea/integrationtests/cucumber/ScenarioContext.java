package fr.gouv.clea.integrationtests.cucumber;

import fr.gouv.clea.integrationtests.config.ApplicationProperties;
import fr.gouv.clea.integrationtests.service.ClusterExpositionService;
import fr.gouv.clea.integrationtests.service.visitorsimulator.Visitor;
import fr.gouv.clea.qr.LocationQrCodeGenerator;
import fr.inria.clea.lsp.exception.CleaCryptoException;
import io.cucumber.spring.ScenarioScope;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Predicate;

@Slf4j
@Component
@ScenarioScope
public class ScenarioContext {

    private final Map<String, Visitor> visitors = new HashMap<>();

    private final Map<String, LocationQrCodeGenerator> locations = new HashMap<>();

    private final Map<String, LocationQrCodeGenerator> staffLocations = new HashMap<>();

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
        return new Visitor(name, clusterExpositionService, applicationProperties);
    }

    public Visitor getVisitor(final String visitorName) {
        return visitors.get(visitorName);
    }

    private LocationQrCodeGenerator createDynamicLocation(final String locationName, final Instant periodStartTime,
            final Integer venueType, final Integer venueCategory1, final Integer venueCategory2,
            final Duration qrCodeRenewalInterval, final Integer periodDuration) throws CleaCryptoException {
        final var qrCodeRenewalIntervalLong = qrCodeRenewalInterval.getSeconds();
        final var qrCodeRenewalIntervalExponentCompact = (int) (Math.log(qrCodeRenewalIntervalLong) / Math.log(2));
        return this.createLocation(
                locationName, periodStartTime, venueType, venueCategory1, venueCategory2,
                qrCodeRenewalIntervalExponentCompact, periodDuration
        );
    }

    private LocationQrCodeGenerator createStaticLocation(final String locationName, final Instant periodStartTime,
            final Integer venueType, final Integer venueCategory1, final Integer venueCategory2,
            final Integer periodDuration) throws CleaCryptoException {
        final var qrCodeRenewalIntervalExponentCompact = 0x1F;
        return this.createLocation(
                locationName, periodStartTime, venueType, venueCategory1, venueCategory2,
                qrCodeRenewalIntervalExponentCompact, periodDuration
        );
    }

    private LocationQrCodeGenerator createLocation(final String locationName, final Instant periodStartTime,
            final Integer venueType, final Integer venueCategory1, final Integer venueCategory2,
            final Integer qrCodeRenewalIntervalExponentCompact, final Integer periodDuration)
            throws CleaCryptoException {
        final var permanentLocationSecretKey = Hex.toHexString(UUID.randomUUID().toString().getBytes());
        final var location = LocationQrCodeGenerator.builder()
                .countryCode(250) // France Country Code
                .staff(false)
                .venueType(venueType)
                .venueCategory1(venueCategory1)
                .venueCategory2(venueCategory2)
                .periodDuration(periodDuration)
                .periodStartTime(periodStartTime)
                .qrCodeRenewalIntervalExponentCompact(qrCodeRenewalIntervalExponentCompact)
                .manualContactTracingAuthorityPublicKey(
                        applicationProperties.getManualContactTracingAuthorityPublicKey()
                )
                .serverAuthorityPublicKey(applicationProperties.getServerAuthorityPublicKey())
                .permanentLocationSecretKey(permanentLocationSecretKey)
                .build();
        final var staffLocation = LocationQrCodeGenerator.builder()
                .countryCode(250) // France Country Code
                .staff(true)
                .venueType(venueType)
                .venueCategory1(venueCategory1)
                .venueCategory2(venueCategory2)
                .periodDuration(periodDuration)
                .periodStartTime(periodStartTime)
                .qrCodeRenewalIntervalExponentCompact(qrCodeRenewalIntervalExponentCompact)
                .manualContactTracingAuthorityPublicKey(
                        applicationProperties.getManualContactTracingAuthorityPublicKey()
                )
                .serverAuthorityPublicKey(applicationProperties.getServerAuthorityPublicKey())
                .permanentLocationSecretKey(permanentLocationSecretKey)
                .build();
        staffLocations.put(locationName, staffLocation);
        locations.put(locationName, location);
        return location;
    }

    private Predicate<VenueConfiguration> matchingConfigurationExists(Integer venueType, Integer venueCategory1,
            Integer venueCategory2) {
        return config -> config.getVenueType() == venueType &&
                config.getVenueCategory1() == venueCategory1 &&
                config.getVenueCategory2() == venueCategory2;
    }

    public LocationQrCodeGenerator getOrCreateDynamicLocation(final String locationName, final Instant periodStartTime,
            final Integer venueType, final Integer venueCategory1, final Integer venueCategory2,
            final Duration qrCodeRenewalInterval) throws CleaCryptoException {
        return this.getOrCreateDynamicLocation(
                locationName, periodStartTime, venueType, venueCategory1, venueCategory2,
                qrCodeRenewalInterval, 24
        );
    }

    public LocationQrCodeGenerator getOrCreateDynamicLocation(final String locationName, final Instant periodStartTime,
            final Integer venueType, final Integer venueCategory1, final Integer venueCategory2,
            final Duration qrCodeRenewalInterval, final Integer periodDuration) throws CleaCryptoException {
        return locations.containsKey(locationName) ? locations.get(locationName)
                : this.createDynamicLocation(
                        locationName, periodStartTime, venueType, venueCategory1, venueCategory2,
                        qrCodeRenewalInterval, periodDuration
                );
    }

    public LocationQrCodeGenerator getOrCreateStaticLocation(final String locationName, final Instant periodStartTime,
            final Integer venueType, final Integer venueCategory1, final Integer venueCategory2,
            final Integer periodDuration) throws CleaCryptoException {
        return locations.containsKey(locationName) ? locations.get(locationName)
                : this.createStaticLocation(
                        locationName, periodStartTime, venueType, venueCategory1, venueCategory2, periodDuration
                );
    }

    public LocationQrCodeGenerator getOrCreateStaticLocationWithUnlimitedDuration(final String locationName,
            final Instant periodStartTime, final Integer venueType, final Integer venueCategory1,
            final Integer venueCategory2) throws CleaCryptoException {
        return this.getOrCreateStaticLocation(
                locationName, periodStartTime, venueType, venueCategory1, venueCategory2, 24
        );
    }

    public LocationQrCodeGenerator getLocation(final String locationName) {
        return locations.get(locationName);
    }

    public LocationQrCodeGenerator getStaffLocation(final String locationName) {
        return staffLocations.get(locationName);
    }
}
