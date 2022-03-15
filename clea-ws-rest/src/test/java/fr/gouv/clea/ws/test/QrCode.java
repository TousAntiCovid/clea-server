package fr.gouv.clea.ws.test;

import fr.inria.clea.lsp.Location;
import fr.inria.clea.lsp.LocationContact;
import fr.inria.clea.lsp.LocationSpecificPart;
import fr.inria.clea.lsp.LocationSpecificPartDecoder;
import org.bouncycastle.util.encoders.Hex;

import java.net.URL;
import java.time.Instant;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;

public class QrCode {

    // server authority public key
    private static final String PK_SA = "02c3a58bf668fa3fe2fc206152abd6d8d55102adfee68c8b227676d1fe763f5a06";

    // manual contact tracing authority public key
    private static final String PK_MCTA = "02c3a58bf668fa3fe2fc206152abd6d8d55102adfee68c8b227676d1fe763f5a06";

    public static final Location LOCATION_1;

    public static final URL LOCATION_1_URL;

    public static final String LOCATION_1_LOCATION_SPECIFIC_PART_DECODED_BASE64;

    public static final UUID LOCATION_1_LOCATION_TEMPORARY_SPECIFIC_ID;

    public static final Location LOCATION_2;

    public static final URL LOCATION_2_URL;

    static {
        final var instant = Instant.now()
                .minus(365, DAYS)
                .truncatedTo(HOURS);
        LOCATION_1 = createRandomLocation(instant);
        LOCATION_2 = createRandomLocation(instant);
        try {
            LOCATION_1_URL = new URL(LOCATION_1.newDeepLink());
            LOCATION_1_LOCATION_SPECIFIC_PART_DECODED_BASE64 = Base64.getEncoder().encodeToString(
                    new LocationSpecificPartDecoder()
                            .decodeHeader(Base64.getUrlDecoder().decode(LOCATION_1_URL.getRef()))
                            .getEncryptedLocationMessage()
            );
            LOCATION_1_LOCATION_TEMPORARY_SPECIFIC_ID = LOCATION_1.getLocationSpecificPart()
                    .getLocationTemporaryPublicId();
            LOCATION_2_URL = new URL(LOCATION_2.newDeepLink());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Location createRandomLocation(Instant instant) {
        return Location.builder()
                .manualContactTracingAuthorityPublicKey(PK_MCTA)
                .serverAuthorityPublicKey(PK_SA)
                .permanentLocationSecretKey(generatePermanentLocationSecretKey())
                .locationSpecificPart(createRandomLocationSpecificPart())
                .contact(
                        LocationContact.builder()
                                .locationPhone("01000000")
                                .locationRegion(0)
                                .locationPin("123456")
                                .periodStartTime(instant)
                                .build()
                )
                .build();
    }

    private static LocationSpecificPart createRandomLocationSpecificPart() {
        return LocationSpecificPart.builder()
                .staff(false)
                .periodDuration(255 /* unlimited */)
                .qrCodeRenewalIntervalExponentCompact(0x1F /* no renewal */)
                .venueType(1)
                .venueCategory1(1)
                .venueCategory2(1)
                .build();
    }

    private static String generatePermanentLocationSecretKey() {
        final var permanentLocationSecretKey = new byte[LocationSpecificPart.LOCATION_TEMPORARY_SECRET_KEY_SIZE];
        new Random().nextBytes(permanentLocationSecretKey);
        return Hex.toHexString(permanentLocationSecretKey);
    }

}
