package fr.gouv.clea.consumer.test;

import fr.gouv.clea.consumer.model.DecodedVisit;
import fr.gouv.clea.consumer.model.Visit;
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

public class ReferenceData {

    // server authority public key
    private static final String PK_SA = "02c3a58bf668fa3fe2fc206152abd6d8d55102adfee68c8b227676d1fe763f5a06";

    // manual contact tracing authority public key
    private static final String PK_MCTA = "02c3a58bf668fa3fe2fc206152abd6d8d55102adfee68c8b227676d1fe763f5a06";

    public static Location LOCATION_1;

    public static URL LOCATION_1_URL;

    public static String LOCATION_1_LOCATION_SPECIFIC_PART_DECODED_BASE64;

    public static UUID LOCATION_1_LOCATION_TEMPORARY_SPECIFIC_ID;

    static {
        final var instant = Instant.now()
                .minus(365, DAYS)
                .truncatedTo(HOURS);
        LOCATION_1 = createRandomLocation(instant);
        try {
            LOCATION_1_URL = new URL(LOCATION_1.newDeepLink());
            LOCATION_1_LOCATION_SPECIFIC_PART_DECODED_BASE64 = Base64.getEncoder().encodeToString(
                    new LocationSpecificPartDecoder()
                            .decodeHeader(Base64.getUrlDecoder().decode(LOCATION_1_URL.getRef()))
                            .getEncryptedLocationMessage()
            );
            LOCATION_1_LOCATION_TEMPORARY_SPECIFIC_ID = LOCATION_1.getLocationSpecificPart()
                    .getLocationTemporaryPublicId();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static DecodedVisit givenBackwardDecodedVisitAt(Instant qrCodeScanTime) {
        return givenDecodedVisitAt(qrCodeScanTime, true);
    }

    private static DecodedVisit givenDecodedVisitAt(Instant qrCodeScanTime, boolean isBackward) {

        try {

            final var instant = Instant.now()
                    .minus(365, DAYS)
                    .truncatedTo(HOURS);

            final var location = createRandomLocation(instant);
            final var locationUrl = new URL(location.newDeepLink(qrCodeScanTime.minus(2, HOURS)));
            final var binaryLocationSpecificPart = Base64.getUrlDecoder().decode(locationUrl.getRef());

            return DecodedVisit.builder()
                    .qrCodeScanTime(qrCodeScanTime)
                    .encryptedLocationSpecificPart(
                            new LocationSpecificPartDecoder().decodeHeader(binaryLocationSpecificPart)
                    )
                    .isBackward(isBackward)
                    .build();

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
                .venueCategory1(2)
                .venueCategory2(3)
                .build();
    }

    private static String generatePermanentLocationSecretKey() {
        final var permanentLocationSecretKey = new byte[LocationSpecificPart.LOCATION_TEMPORARY_SECRET_KEY_SIZE];
        new Random().nextBytes(permanentLocationSecretKey);
        return Hex.toHexString(permanentLocationSecretKey);
    }

    public static Visit.VisitBuilder<?, ?> defaultVisit() {
        final var lsp = LOCATION_1.getLocationSpecificPart();
        final var now = Instant.now();
        return Visit.builder()
                .version(0)
                .type(0)
                .staff(true)
                .locationTemporaryPublicId(lsp.getLocationTemporaryPublicId())
                .qrCodeRenewalIntervalExponentCompact(lsp.getQrCodeRenewalIntervalExponentCompact())
                .venueType(lsp.getVenueType())
                .venueCategory1(lsp.getVenueCategory1())
                .venueCategory2(lsp.getVenueCategory2())
                .periodDuration(lsp.getPeriodDuration())
                .compressedPeriodStartTime(lsp.getCompressedPeriodStartTime())
                .qrCodeValidityStartTime(lsp.getPeriodStartTime())
                .qrCodeScanTime(now)
                .locationTemporarySecretKey(lsp.getLocationTemporarySecretKey())
                .encryptedLocationContactMessage(lsp.getEncryptedLocationContactMessage())
                .isBackward(true);
    }
}
