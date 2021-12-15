package fr.gouv.clea.integrationtests.service.qrcodesimulator;

import fr.gouv.clea.integrationtests.config.ApplicationProperties;
import fr.inria.clea.lsp.Location;
import fr.inria.clea.lsp.LocationSpecificPart;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bouncycastle.util.encoders.Hex;

import java.time.Instant;
import java.util.UUID;

import static fr.gouv.clea.integrationtests.service.qrcodesimulator.LocationSpec.LocationType.STAFF;
import static fr.gouv.clea.integrationtests.service.qrcodesimulator.LocationSpec.LocationType.VISITOR;
import static lombok.AccessLevel.PRIVATE;

/**
 * A factory to make it easier to create {@link Location} instances using the
 * CLEA crypto library API.
 * <p>
 * It uses a random <em>permanentLocationSecretKey</em> shared between the
 * regular visitor location and the staff location.
 * <p>
 * Default values help create static {@link Location}s:
 *
 * <pre>
 *
 * final var locationBuilder = LocationFactory.builder(applicationProperties)
 *         .startTime(Instant.parse("2021-12-15T01:54:58Z"))
 *         .venueConfig(4, 2, 2);
 * </pre>
 *
 * A dynamic deeplink can be created using:
 *
 * <pre>
 *
 * final var locationBuilder = LocationFactory.builder(applicationProperties)
 *         .startTime(Instant.parse("2021-12-15T01:54:58Z"))
 *         .venueConfig(4, 2, 2)
 *         .periodDurationHours(1)
 *         .renewalIntervalSeconds(512);
 * </pre>
 *
 * Finally a regular location for visitors and the staff location can be build
 * using:
 *
 * <pre>
 *
 * final Location visitorLocation = locationBuilder.buildVisitor();
 *
 * final Location staffLocation = locationBuilder.buildStaff();
 * </pre>
 */
@RequiredArgsConstructor(access = PRIVATE)
public class LocationSpec {

    private final static int INFINITE_PERIOD_DURATION = 255;

    private final static int STATIC_LOCATION_DEEPLINK_RENEWAL_INTERVAL = 0x1F;

    private final String serverAuthorityPublicKey;

    private final String manualContactTracingPublicKey;

    private final String permanentLocationSecretKey = Hex.toHexString(UUID.randomUUID().toString().getBytes());

    @Getter
    private Instant startTime = Instant.now();

    private Integer periodDurationHours = INFINITE_PERIOD_DURATION;

    private Integer renewalIntervalSeconds = null;

    private int venueType;

    private int venueCategory1;

    private int venueCategory2;

    public static LocationSpec builder(final ApplicationProperties applicationProperties) {
        return new LocationSpec(
                applicationProperties.getServerAuthorityPublicKey(),
                applicationProperties.getManualContactTracingAuthorityPublicKey()
        );
    }

    public LocationSpec startTime(final Instant startTime) {
        this.startTime = startTime;
        return this;
    }

    public LocationSpec periodDurationHours(final int periodDurationHours) {
        this.periodDurationHours = periodDurationHours;
        return this;
    }

    public LocationSpec renewalIntervalSeconds(final int renewalIntervalSeconds) {
        this.renewalIntervalSeconds = renewalIntervalSeconds;
        return this;
    }

    public LocationSpec venueConfig(final int venueType, final int venueCategory1, final int venueCategory2) {
        this.venueType = venueType;
        this.venueCategory1 = venueCategory1;
        this.venueCategory2 = venueCategory2;
        return this;
    }

    public Location buildVisitor() {
        return build(VISITOR);
    }

    public Location buildStaff() {
        return build(STAFF);
    }

    private Location build(final LocationType locationType) {
        return Location.builder()
                .locationSpecificPart(
                        LocationSpecificPart.builder()
                                .staff(locationType.isStaff)
                                .qrCodeValidityStartTime(startTime)
                                .periodDuration(periodDurationHours)
                                .qrCodeRenewalIntervalExponentCompact(getRenewalIntervalAsPowerOf2())
                                .venueType(venueType)
                                .venueCategory1(venueCategory1)
                                .venueCategory2(venueCategory2)
                                .build()
                )
                .manualContactTracingAuthorityPublicKey(manualContactTracingPublicKey)
                .serverAuthorityPublicKey(serverAuthorityPublicKey)
                .permanentLocationSecretKey(permanentLocationSecretKey)
                .contact(null)
                .build();
    }

    private int getRenewalIntervalAsPowerOf2() {
        if (null == renewalIntervalSeconds) {
            return STATIC_LOCATION_DEEPLINK_RENEWAL_INTERVAL;
        } else {
            return (int) (Math.log(renewalIntervalSeconds) / Math.log(2));
        }
    }

    @RequiredArgsConstructor
    public enum LocationType {

        VISITOR(false),
        STAFF(true);

        private final boolean isStaff;
    }
}
