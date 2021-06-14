package fr.gouv.clea.integrationtests.feature.context;

import fr.gouv.clea.api.CleaApi;
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

    private final Map<String, Map<String, ConfPair>> venueTypeCategories1 = new HashMap<>();

    private final Map<String, LocationQrCodeGenerator> locations = new HashMap<>(10);
    private final Map<String, LocationQrCodeGenerator> staffLocations = new HashMap<>(10);

    private String serverAuthorityPublicKey;
    private String manualContactTracingAuthorityPublicKey;

    private final CleaApi cleaApi;
    private final ApplicationProperties appConfig;
    private final CleaS3Service s3service;

    public ScenarioContext(final ApplicationProperties appConfig, final CleaApi cleaApi, CleaS3Service s3service) throws Exception {
        this.cleaApi = cleaApi;
        this.appConfig = appConfig;
        this.s3service = s3service;
        this.initializeKeys(appConfig);
        Map<String, ConfPair> currConf = new HashMap<>();
        venueTypeCategories1.put("restauration", currConf);
        currConf.put("restaurant rapide", new ConfPair(1, 1));
        currConf = new HashMap<>();
        venueTypeCategories1.put("etablissements sportifs", currConf);
        currConf.put("sport indoor", new ConfPair(4, 2));
        currConf.put("salle de sport", new ConfPair(4, 1));
        log.debug("{}", venueTypeCategories1);
    }

    public void initializeKeys(final ApplicationProperties appConfig) throws Exception {
        this.serverAuthorityPublicKey = appConfig.getServerAuthorityPublicKey();
        this.manualContactTracingAuthorityPublicKey = appConfig.getManualContactTracingAuthorityPublicKey();
        if (Objects.isNull(serverAuthorityPublicKey) || serverAuthorityPublicKey.isBlank()) {
            this.generateKeys();
        }
    }

    public void generateKeys() throws Exception {
        CleaEciesEncoder cleaEciesEncoder = new CleaEciesEncoder();
        String[] serverAuthorityKeyPair = cleaEciesEncoder.genKeysPair(true);
        this.serverAuthorityPublicKey = serverAuthorityKeyPair[1];
        log.info("Server Authority Private Key: {}", serverAuthorityKeyPair[0]);
        log.info("Server Authority Public Key : {}", this.serverAuthorityPublicKey);

        String[] manualContactTracingAuthorityKeyPair = cleaEciesEncoder.genKeysPair(true);
        this.manualContactTracingAuthorityPublicKey = manualContactTracingAuthorityKeyPair[1];
        log.info("Manual Contact Tracing Authority Private Key: {}", serverAuthorityKeyPair[0]);
        log.info("Manual Contact Tracing Authority Public Key : {}", this.manualContactTracingAuthorityPublicKey);
    }

    public Visitor getOrCreateUser(String name) {
        return visitors.computeIfAbsent(name, this::createVisitor);
    }

    private Visitor createVisitor(String name) {
        return new Visitor(name, cleaApi, s3service, appConfig);
    }

    public Visitor getVisitor(String visitorName) {
        return visitors.get(visitorName);
    }

    public void updateOrCreateRiskConfig(String vtype, String vcategory1, Integer vcategory2, Integer backwardThreshold, Integer backwardExposureTime, Float backwardRisk, Integer forwardThreshold, Integer forwardExposureTime, Float forwardRisk) {
        //TODO: Create new ConfPair or just check if it exists?
    }

    private LocationQrCodeGenerator createDynamicLocation(String locationName, Instant periodStartTime, String venueType,
                                                          String venueCategory1, Integer venueCategory2, Duration qrCodeRenewalInterval, Integer periodDuration) throws CleaCryptoException {
        long qrCodeRenewalIntervalLong = qrCodeRenewalInterval.getSeconds();
        int qrCodeRenewalIntervalExponentCompact = (int) (Math.log(qrCodeRenewalIntervalLong) / Math.log(2));
        return this.createLocation(locationName, periodStartTime, venueType, venueCategory1, venueCategory2, qrCodeRenewalIntervalExponentCompact, periodDuration);
    }

    private LocationQrCodeGenerator createStaticLocation(String locationName, Instant periodStartTime, String venueType,
                                                         String venueCategory1, Integer venueCategory2, Integer periodDuration) throws CleaCryptoException {
        int qrCodeRenewalIntervalExponentCompact = 0x1F;
        return this.createLocation(locationName, periodStartTime, venueType, venueCategory1, venueCategory2, qrCodeRenewalIntervalExponentCompact, periodDuration);
    }


    private LocationQrCodeGenerator createLocation(String locationName, Instant periodStartTime, String venueType,
                                                   String venueCategory1, Integer venueCategory2, Integer qrCodeRenewalIntervalExponentCompact, Integer periodDuration) throws CleaCryptoException {
        final String permanentLocationSecretKey = Hex.toHexString(UUID.randomUUID().toString().getBytes());
        ConfPair conf;
        if (venueTypeCategories1.containsKey(venueType) && venueTypeCategories1.get(venueType).containsKey(venueCategory1)) {
            conf = venueTypeCategories1.get(venueType).get(venueCategory1);
        } else {
            conf = new ConfPair(9, 9);
        }
        LocationQrCodeGenerator location = LocationQrCodeGenerator.builder()
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
        LocationQrCodeGenerator staffLocation = LocationQrCodeGenerator.builder()
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

    public LocationQrCodeGenerator getOrCreateDynamicLocation(String locationName, Instant periodStartTime, String venueType,
                                                              String venueCategory1, Integer venueCategory2, Duration qrCodeRenewalInterval) throws CleaCryptoException {
        return this.getOrCreateDynamicLocation(locationName, periodStartTime, venueType, venueCategory1, venueCategory2,
                qrCodeRenewalInterval, 24);
    }

    public LocationQrCodeGenerator getOrCreateDynamicLocation(String locationName, Instant periodStartTime,
                                                              String venueType, String venueCategory1, Integer venueCategory2, Duration qrCodeRenewalInterval,
                                                              Integer periodDuration) throws CleaCryptoException {
        return locations.containsKey(locationName) ? locations.get(locationName)
                : this.createDynamicLocation(locationName, periodStartTime, venueType, venueCategory1, venueCategory2,
                qrCodeRenewalInterval, periodDuration);
    }

    public LocationQrCodeGenerator getOrCreateStaticLocation(String locationName, Instant periodStartTime, String venueType,
                                                             String venueCategory1, Integer venueCategory2, Integer periodDuration) throws CleaCryptoException {
        return locations.containsKey(locationName) ? locations.get(locationName)
                : this.createStaticLocation(locationName, periodStartTime, venueType, venueCategory1, venueCategory2, periodDuration);
    }

    public LocationQrCodeGenerator getOrCreateStaticLocation(String locationName, Instant periodStartTime, String venueType,
                                                             String venueCategory1, Integer venueCategory2) throws CleaCryptoException {
        return this.getOrCreateStaticLocation(locationName, periodStartTime, venueType, venueCategory1, venueCategory2, 24);
    }

    public LocationQrCodeGenerator getLocation(String locationName) {
        return locations.get(locationName);
    }

    public LocationQrCodeGenerator getStaffLocation(String locationName) {
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
