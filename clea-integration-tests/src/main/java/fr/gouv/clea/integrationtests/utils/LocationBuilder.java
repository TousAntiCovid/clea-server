package fr.gouv.clea.integrationtests.utils;

import fr.inria.clea.lsp.Location;
import fr.inria.clea.lsp.LocationContact;
import fr.inria.clea.lsp.LocationSpecificPart;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.With;
import org.bouncycastle.util.encoders.Hex;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Random;

import static fr.inria.clea.lsp.LocationSpecificPart.LOCATION_TEMPORARY_SECRET_KEY_SIZE;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.YEARS;
import static java.util.stream.Collectors.joining;
import static lombok.AccessLevel.PRIVATE;

@RequiredArgsConstructor
@AllArgsConstructor(access = PRIVATE)
public class LocationBuilder {

    private final Random random = new Random();

    private final String serverAuthorityPublicKey;

    private final String manualContactTracingAuthorityPublicKey;

    private final byte[] permanentLocationSecretKey = new byte[LOCATION_TEMPORARY_SECRET_KEY_SIZE];

    @With
    private Instant periodStartTime = Instant.now().minus(365, DAYS);

    @With
    private double renewalIntervalInSeconds = Math.pow(2, 10);

    @With
    private int periodDurationInHours = 1;

    private Integer venueType = 1;

    private Integer venueCategory1 = 1;

    private Integer venueCategory2 = 1;

    public LocationBuilder withRandomPermanentLocationSecretKey() {
        random.nextBytes(permanentLocationSecretKey);
        return this;
    }

    public LocationBuilder withVenueParameters(int venueType, int venueCategory1, int venueCategory2) {
        this.venueType = venueType;
        this.venueCategory1 = venueCategory1;
        this.venueCategory2 = venueCategory2;
        return this;
    }

    public Location buildPublic() {
        return build(false);
    }

    public Location buildStaff() {
        return build(true);
    }

    private Location build(boolean staff) {
        return Location.builder()
                .serverAuthorityPublicKey(serverAuthorityPublicKey)
                .manualContactTracingAuthorityPublicKey(manualContactTracingAuthorityPublicKey)
                .permanentLocationSecretKey(Hex.toHexString(permanentLocationSecretKey))
                .locationSpecificPart(
                        unlimitedRandomLocationSpecificPart()
                                .staff(staff)
                                .build()
                )
                .contact(
                        randomContact()
                                .periodStartTime(periodStartTime)
                                .build()
                )
                .build();
    }

    private LocationSpecificPart.LocationSpecificPartBuilder<?, ?> unlimitedRandomLocationSpecificPart() {
        final var renewalIntervalExponentCompact = (int) (Math.log(renewalIntervalInSeconds) / Math.log(2));

        return LocationSpecificPart.builder()
                .periodDuration(periodDurationInHours)
                .qrCodeRenewalIntervalExponentCompact(renewalIntervalExponentCompact)
                .venueType(venueType)
                .venueCategory1(venueCategory1)
                .venueCategory2(venueCategory2);
    }

    private LocationContact.LocationContactBuilder randomContact() {
        final var randomPhoneNumber = random.ints(10, 0, 9)
                .mapToObj(String::valueOf)
                .collect(joining());
        final var randomPinNumber = random.ints(6, 0, 9)
                .mapToObj(String::valueOf)
                .collect(joining());
        return LocationContact.builder()
                .locationPhone(randomPhoneNumber)
                .locationRegion(0)
                .locationPin(randomPinNumber)
                .periodStartTime(Instant.now());
    }
}
