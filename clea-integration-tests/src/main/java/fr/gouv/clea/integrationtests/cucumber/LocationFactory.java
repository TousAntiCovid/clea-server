package fr.gouv.clea.integrationtests.cucumber;

import fr.gouv.clea.integrationtests.config.ApplicationProperties;
import fr.inria.clea.lsp.Location;
import fr.inria.clea.lsp.LocationSpecificPart;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

import static java.util.UUID.randomUUID;
import static org.bouncycastle.util.encoders.Hex.toHexString;

@Component
@RequiredArgsConstructor
public class LocationFactory {

    private final ApplicationProperties applicationProperties;

    private final int STATIC_LOCATION_DEEPLINK_RENEWAL_INTERVAL = 0x1F;

    private final int INFINITE_PERIOD_DURATION = 255;

    public Location createStaticLocation(final int venueType,
            final int venueCategory1,
            final int venueCategory2) {
        return createLocation(
                venueType,
                venueCategory1,
                venueCategory2,
                STATIC_LOCATION_DEEPLINK_RENEWAL_INTERVAL,
                INFINITE_PERIOD_DURATION,
                false
        );
    }

    public Location createDynamicLocation(final int venueType,
            final int venueCategory1,
            final int venueCategory2,
            final Duration deepLinkRenewalInterval,
            final int periodDurationHours) {
        final int formattedDeepLinkRenewalInterval = getFormattedDeeplinkRenewalInterval(deepLinkRenewalInterval);
        return createLocation(
                venueType,
                venueCategory1,
                venueCategory2,
                formattedDeepLinkRenewalInterval,
                periodDurationHours,
                false
        );
    }

    private int getFormattedDeeplinkRenewalInterval(Duration deepLinkRenewalInterval) {
        return (int) (Math.log(deepLinkRenewalInterval.getSeconds()) / Math.log(2));
    }

    public Location createStaticStaffLocation(final int venueType,
            final int venueCategory1,
            final int venueCategory2) {
        return createLocation(
                venueType,
                venueCategory1,
                venueCategory2,
                STATIC_LOCATION_DEEPLINK_RENEWAL_INTERVAL,
                INFINITE_PERIOD_DURATION,
                true
        );
    }

    public Location createDynamicStaffLocation(final int venueType,
            final int venueCategory1,
            final int venueCategory2,
            final Duration deepLinkRenewalInterval,
            final int periodDurationHours) {
        final int formattedDeepLinkRenewalInterval = getFormattedDeeplinkRenewalInterval(deepLinkRenewalInterval);
        return createLocation(
                venueType,
                venueCategory1,
                venueCategory2,
                formattedDeepLinkRenewalInterval,
                periodDurationHours,
                true
        );
    }

    private Location createLocation(final int venueType,
            final int venueCategory1,
            final int venueCategory2,
            final int deepLinkRenewalInterval,
            final int periodDurationHours,
            final boolean staff) {
        final var permanentLocationSecretKey = toHexString(randomUUID().toString().getBytes());

        return Location.builder()
                .locationSpecificPart(
                        LocationSpecificPart.builder()
                                .staff(staff)
                                .periodDuration(periodDurationHours)
                                .qrCodeRenewalIntervalExponentCompact(deepLinkRenewalInterval)
                                .venueType(venueType)
                                .venueCategory1(venueCategory1)
                                .venueCategory2(venueCategory2)
                                .build()
                )
                .manualContactTracingAuthorityPublicKey(
                        applicationProperties.getManualContactTracingAuthorityPublicKey()
                )
                .serverAuthorityPublicKey(applicationProperties.getServerAuthorityPublicKey())
                .permanentLocationSecretKey(permanentLocationSecretKey)
                .contact(null)
                .build();
    }
}
