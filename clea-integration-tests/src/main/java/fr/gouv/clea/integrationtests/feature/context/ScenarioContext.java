package fr.gouv.clea.integrationtests.feature.context;

import fr.gouv.clea.integrationtests.config.ApplicationProperties;
import fr.gouv.clea.integrationtests.service.CleaS3Service;
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

import static fr.gouv.clea.integrationtests.feature.context.VenueConfiguration.DEFAULT;
import static java.util.Collections.singleton;

@Slf4j
@Component
@ScenarioScope
public class ScenarioContext {

    private final Map<String, Visitor> visitors = new HashMap<>(10);

    private final Set<VenueConfiguration> venueConfigurations = new HashSet<>(singleton(DEFAULT));

    private final Map<String, LocationQrCodeGenerator> locations = new HashMap<>(10);

    private final Map<String, LocationQrCodeGenerator> staffLocations = new HashMap<>(10);

    private final ApplicationProperties applicationProperties;

    private final CleaS3Service s3service;

    public ScenarioContext(final ApplicationProperties applicationProperties, final CleaS3Service s3service)
            throws Exception {
        this.applicationProperties = applicationProperties;
        this.s3service = s3service;
    }

    public Visitor getOrCreateUser(final String name) {
        return visitors.computeIfAbsent(name, this::createVisitor);
    }

    private Visitor createVisitor(final String name) {
        return new Visitor(name, s3service, applicationProperties);
    }

    public Visitor getVisitor(final String visitorName) {
        return visitors.get(visitorName);
    }

    // TODO remonter le venueType au niveau maximum
    public void updateOrCreateRiskConfig(final Integer venueType, final Integer venueCategory1,
            final Integer venueCategory2) {
        venueConfigurations.add(new VenueConfiguration(venueType, venueCategory1, venueCategory2));
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
        final VenueConfiguration venueConfiguration = findOrCreate(venueType, venueCategory1, venueCategory2);
        final var location = LocationQrCodeGenerator.builder()
                .countryCode(250) // France Country Code
                .staff(false)
                .venueType(venueConfiguration.getVenueType())
                .venueCategory1(venueConfiguration.getVenueCategory1())
                .venueCategory2(venueConfiguration.getVenueCategory2())
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
                .venueType(venueConfiguration.getVenueType())
                .venueCategory1(venueConfiguration.getVenueCategory1())
                .venueCategory2(venueConfiguration.getVenueCategory2())
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

    private VenueConfiguration findOrCreate(Integer venueType, Integer venueCategory1, Integer venueCategory2) {
        return venueConfigurations.stream()
                .filter(matchingConfigurationExists(venueType, venueCategory1, venueCategory2))
                .findFirst()
                .orElseGet(() -> {
                    VenueConfiguration newConfiguration = new VenueConfiguration(
                            venueType, venueCategory1, venueCategory2
                    );
                    venueConfigurations.add(newConfiguration);
                    return newConfiguration;
                });
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
