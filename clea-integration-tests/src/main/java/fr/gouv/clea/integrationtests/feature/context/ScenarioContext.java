package fr.gouv.clea.integrationtests.feature.context;

import fr.gouv.clea.integrationtests.config.ApplicationProperties;
import fr.gouv.clea.integrationtests.service.CleaS3Service;
import fr.gouv.clea.qr.LocationQrCodeGenerator;
import fr.inria.clea.lsp.CleaEciesEncoder;
import fr.inria.clea.lsp.exception.CleaCryptoException;
import io.cucumber.spring.ScenarioScope;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Component
@ScenarioScope
public class ScenarioContext {

    private final Map<String, Visitor> visitors = new HashMap<>(10);

    private final Map<String, Map<String, ConfPair>> venueTypeCategories1 = Map.of(
            "restauration", Map.of("restaurant rapide", new ConfPair(1, 1)),
            "etablissements sportifs", Map.of("sport indoor", new ConfPair(4, 2), "salle de sport", new ConfPair(4, 1))
    );

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

    public void updateOrCreateRiskConfig(final String vtype, final String vcategory1, final Integer vcategory2,
            final Integer backwardThreshold, final Integer backwardExposureTime, final Float backwardRisk,
            final Integer forwardThreshold, final Integer forwardExposureTime, final Float forwardRisk) {
        // TODO: Create new ConfPair or just check if it exists?
    }

    private LocationQrCodeGenerator createDynamicLocation(final String locationName, final Instant periodStartTime,
            final String venueType, final String venueCategory1, final Integer venueCategory2,
            final Duration qrCodeRenewalInterval, final Integer periodDuration) throws CleaCryptoException {
        final var qrCodeRenewalIntervalLong = qrCodeRenewalInterval.getSeconds();
        final var qrCodeRenewalIntervalExponentCompact = (int) (Math.log(qrCodeRenewalIntervalLong) / Math.log(2));
        return this.createLocation(
                locationName, periodStartTime, venueType, venueCategory1, venueCategory2,
                qrCodeRenewalIntervalExponentCompact, periodDuration
        );
    }

    private LocationQrCodeGenerator createStaticLocation(final String locationName, final Instant periodStartTime,
            final String venueType, final String venueCategory1, final Integer venueCategory2,
            final Integer periodDuration) throws CleaCryptoException {
        final var qrCodeRenewalIntervalExponentCompact = 0x1F;
        return this.createLocation(
                locationName, periodStartTime, venueType, venueCategory1, venueCategory2,
                qrCodeRenewalIntervalExponentCompact, periodDuration
        );
    }

    private LocationQrCodeGenerator createLocation(final String locationName, final Instant periodStartTime,
            final String venueType, final String venueCategory1, final Integer venueCategory2,
            final Integer qrCodeRenewalIntervalExponentCompact, final Integer periodDuration)
            throws CleaCryptoException {
        final var permanentLocationSecretKey = Hex.toHexString(UUID.randomUUID().toString().getBytes());
        ConfPair conf;
        if (venueTypeCategories1.containsKey(venueType)
                && venueTypeCategories1.get(venueType).containsKey(venueCategory1)) {
            conf = venueTypeCategories1.get(venueType).get(venueCategory1);
        } else {
            conf = new ConfPair(9, 9);
        }
        final var location = LocationQrCodeGenerator.builder()
                .countryCode(250) // France Country Code
                .staff(false)
                .venueType(conf.getType())
                .venueCategory1(conf.getCategory())
                .venueCategory2(venueCategory2)
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
                .venueType(conf.getType())
                .venueCategory1(conf.getCategory())
                .venueCategory2(venueCategory2)
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

    public LocationQrCodeGenerator getOrCreateDynamicLocation(final String locationName, final Instant periodStartTime,
            final String venueType, final String venueCategory1, final Integer venueCategory2,
            final Duration qrCodeRenewalInterval) throws CleaCryptoException {
        return this.getOrCreateDynamicLocation(
                locationName, periodStartTime, venueType, venueCategory1, venueCategory2,
                qrCodeRenewalInterval, 24
        );
    }

    public LocationQrCodeGenerator getOrCreateDynamicLocation(final String locationName, final Instant periodStartTime,
            final String venueType, final String venueCategory1, final Integer venueCategory2,
            final Duration qrCodeRenewalInterval, final Integer periodDuration) throws CleaCryptoException {
        return locations.containsKey(locationName) ? locations.get(locationName)
                : this.createDynamicLocation(
                        locationName, periodStartTime, venueType, venueCategory1, venueCategory2,
                        qrCodeRenewalInterval, periodDuration
                );
    }

    public LocationQrCodeGenerator getOrCreateStaticLocation(final String locationName, final Instant periodStartTime,
            final String venueType, final String venueCategory1, final Integer venueCategory2,
            final Integer periodDuration) throws CleaCryptoException {
        return locations.containsKey(locationName) ? locations.get(locationName)
                : this.createStaticLocation(
                        locationName, periodStartTime, venueType, venueCategory1, venueCategory2, periodDuration
                );
    }

    public LocationQrCodeGenerator getOrCreateStaticLocationWithUnlimitedDuration(final String locationName,
            final Instant periodStartTime, final String venueType, final String venueCategory1,
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

    @Getter
    private static class ConfPair {

        private final int type;

        private final int category;

        public ConfPair(int type, int category) {
            this.type = type;
            this.category = category;
        }
    }

}
