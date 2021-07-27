package fr.gouv.clea.integrationtests.feature.context;

import fr.gouv.clea.integrationtests.config.ApplicationProperties;
import fr.gouv.clea.integrationtests.service.CleaS3Service;
import fr.gouv.clea.qr.LocationQrCodeGenerator;
import fr.inria.clea.lsp.CleaEciesEncoder;
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

    private String serverAuthorityPublicKey;

    private String manualContactTracingAuthorityPublicKey;

    private final ApplicationProperties appConfig;

    private final CleaS3Service s3service;

    public ScenarioContext(final ApplicationProperties appConfig, final CleaS3Service s3service) throws Exception {
        this.appConfig = appConfig;
        this.s3service = s3service;
        this.initializeKeys(appConfig);
    }

    public void initializeKeys(final ApplicationProperties appConfig) throws Exception {
        this.serverAuthorityPublicKey = appConfig.getServerAuthorityPublicKey();
        this.manualContactTracingAuthorityPublicKey = appConfig.getManualContactTracingAuthorityPublicKey();
        if (Objects.isNull(serverAuthorityPublicKey) || serverAuthorityPublicKey.isBlank()) {
            this.generateKeys();
        }
    }

    public void generateKeys() throws Exception {
        final var cleaEciesEncoder = new CleaEciesEncoder();
        final String[] serverAuthorityKeyPair = cleaEciesEncoder.genKeysPair(true);
        this.serverAuthorityPublicKey = serverAuthorityKeyPair[1];
        log.info("Server Authority Private Key: {}", serverAuthorityKeyPair[0]);
        log.info("Server Authority Public Key : {}", this.serverAuthorityPublicKey);

        String[] manualContactTracingAuthorityKeyPair = cleaEciesEncoder.genKeysPair(true);
        this.manualContactTracingAuthorityPublicKey = manualContactTracingAuthorityKeyPair[1];
        log.info("Manual Contact Tracing Authority Private Key: {}", serverAuthorityKeyPair[0]);
        log.info("Manual Contact Tracing Authority Public Key : {}", this.manualContactTracingAuthorityPublicKey);
    }

    public Visitor getOrCreateUser(final String name) {
        return visitors.computeIfAbsent(name, this::createVisitor);
    }

    private Visitor createVisitor(final String name) {
        return new Visitor(name, s3service, appConfig);
    }

    public Visitor getVisitor(final String visitorName) {
        return visitors.get(visitorName);
    }

    // TODO remonter le venueType au niveau maximum
    public void updateOrCreateRiskConfig(final String vtype, final Integer vcategory1, final Integer vcategory2) {
        venueConfigurations.add(new VenueConfiguration(VenueType.valueFromName(vtype), vcategory1, vcategory2));
    }

    private LocationQrCodeGenerator createDynamicLocation(final String locationName, final Instant periodStartTime,
            final VenueType venueType, final Integer venueCategory1, final Integer venueCategory2,
            final Duration qrCodeRenewalInterval, final Integer periodDuration) throws CleaCryptoException {
        final var qrCodeRenewalIntervalLong = qrCodeRenewalInterval.getSeconds();
        final var qrCodeRenewalIntervalExponentCompact = (int) (Math.log(qrCodeRenewalIntervalLong) / Math.log(2));
        return this.createLocation(
                locationName, periodStartTime, venueType, venueCategory1, venueCategory2,
                qrCodeRenewalIntervalExponentCompact, periodDuration
        );
    }

    private LocationQrCodeGenerator createStaticLocation(final String locationName, final Instant periodStartTime,
            final VenueType venueType, final Integer venueCategory1, final Integer venueCategory2,
            final Integer periodDuration) throws CleaCryptoException {
        final var qrCodeRenewalIntervalExponentCompact = 0x1F;
        return this.createLocation(
                locationName, periodStartTime, venueType, venueCategory1, venueCategory2,
                qrCodeRenewalIntervalExponentCompact, periodDuration
        );
    }

    private LocationQrCodeGenerator createLocation(final String locationName, final Instant periodStartTime,
            final VenueType venueType, final Integer venueCategory1, final Integer venueCategory2,
            final Integer qrCodeRenewalIntervalExponentCompact, final Integer periodDuration)
            throws CleaCryptoException {
        final var permanentLocationSecretKey = Hex.toHexString(UUID.randomUUID().toString().getBytes());
        final VenueConfiguration venueConfiguration = findOrCreate(venueType, venueCategory1, venueCategory2);
        final var location = LocationQrCodeGenerator.builder()
                .countryCode(250) // France Country Code
                .staff(false)
                .venueType(venueConfiguration.getVenueType().getValue())
                .venueCategory1(venueConfiguration.getVenueCategory1())
                .venueCategory2(venueConfiguration.getVenueCategory2())
                .periodDuration(periodDuration)
                .periodStartTime(periodStartTime)
                .qrCodeRenewalIntervalExponentCompact(qrCodeRenewalIntervalExponentCompact)
                .manualContactTracingAuthorityPublicKey(manualContactTracingAuthorityPublicKey)
                .serverAuthorityPublicKey(serverAuthorityPublicKey)
                .permanentLocationSecretKey(permanentLocationSecretKey)
                .build();
        final var staffLocation = LocationQrCodeGenerator.builder()
                .countryCode(250) // France Country Code
                .staff(true)
                .venueType(venueConfiguration.getVenueType().getValue())
                .venueCategory1(venueConfiguration.getVenueCategory1())
                .venueCategory2(venueConfiguration.getVenueCategory2())
                .periodDuration(periodDuration)
                .periodStartTime(periodStartTime)
                .qrCodeRenewalIntervalExponentCompact(qrCodeRenewalIntervalExponentCompact)
                .manualContactTracingAuthorityPublicKey(manualContactTracingAuthorityPublicKey)
                .serverAuthorityPublicKey(serverAuthorityPublicKey)
                .permanentLocationSecretKey(permanentLocationSecretKey)
                .build();
        staffLocations.put(locationName, staffLocation);
        locations.put(locationName, location);
        return location;
    }

    private VenueConfiguration findOrCreate(VenueType venueType, Integer venueCategory1, Integer venueCategory2) {
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

    private Predicate<VenueConfiguration> matchingConfigurationExists(VenueType venueType, Integer venueCategory1,
            Integer venueCategory2) {
        return config -> config.getVenueType().equals(venueType) &&
                config.getVenueCategory1() == venueCategory1 &&
                config.getVenueCategory2() == venueCategory2;
    }

    public LocationQrCodeGenerator getOrCreateDynamicLocation(final String locationName, final Instant periodStartTime,
            final VenueType venueType, final Integer venueCategory1, final Integer venueCategory2,
            final Duration qrCodeRenewalInterval) throws CleaCryptoException {
        return this.getOrCreateDynamicLocation(
                locationName, periodStartTime, venueType, venueCategory1, venueCategory2,
                qrCodeRenewalInterval, 24
        );
    }

    public LocationQrCodeGenerator getOrCreateDynamicLocation(final String locationName, final Instant periodStartTime,
            final VenueType venueType, final Integer venueCategory1, final Integer venueCategory2,
            final Duration qrCodeRenewalInterval, final Integer periodDuration) throws CleaCryptoException {
        return locations.containsKey(locationName) ? locations.get(locationName)
                : this.createDynamicLocation(
                        locationName, periodStartTime, venueType, venueCategory1, venueCategory2,
                        qrCodeRenewalInterval, periodDuration
                );
    }

    public LocationQrCodeGenerator getOrCreateStaticLocation(final String locationName, final Instant periodStartTime,
            final VenueType venueType, final Integer venueCategory1, final Integer venueCategory2,
            final Integer periodDuration) throws CleaCryptoException {
        return locations.containsKey(locationName) ? locations.get(locationName)
                : this.createStaticLocation(
                        locationName, periodStartTime, venueType, venueCategory1, venueCategory2, periodDuration
                );
    }

    public LocationQrCodeGenerator getOrCreateStaticLocationWithUnlimitedDuration(final String locationName,
            final Instant periodStartTime, final VenueType venueType, final Integer venueCategory1,
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
