package fr.gouv.clea.integrationtests.cucumber.steps;

import fr.gouv.clea.integrationtests.cucumber.ScenarioContext;
import fr.gouv.clea.integrationtests.model.DeepLink;
import fr.gouv.clea.integrationtests.model.Place;
import fr.inria.clea.lsp.exception.CleaCryptoException;
import io.cucumber.java.en.Given;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@AllArgsConstructor
public class CleaQrCodeSteps {

    private final ScenarioContext scenarioContext;

    @Given("{string} created a dynamic deeplink at {naturalTime} with a renewal time of {duration}")
    public void create_qrcode_at_periodStartTime_with_renewalTime(final String locationName,
            final Instant qrCodePeriodStartTime,
            final Duration qrCodeRenewalIntervalDuration) {
        scenarioContext.getPlace(locationName).ifPresent(
                it -> it.addDeepLink(createDynamicDeepLink(it, qrCodePeriodStartTime, qrCodeRenewalIntervalDuration))
        );
    }

    @Given("{string} created a static deeplink at {naturalTime}")
    public void create_deeplink_with_periodStartTime_without_qrCodeRenewalIntervalDuration(final String locationName,
            final Instant periodStartTime) {
        scenarioContext.getPlace(locationName).ifPresent(
                place -> place.addDeepLink(createStaticDeepLink(place, periodStartTime))
        );
    }

    @Given("{string} created a static staff deeplink at {naturalTime}")
    public void create_staff_deeplink_with_periodStartTime_without_qrCodeRenewalIntervalDuration(
            final String locationName,
            final Instant periodStartTime) {
        scenarioContext.getPlace(locationName).ifPresent(
                place -> place.addStaffDeepLink(createStaticDeepLink(place, periodStartTime))
        );
    }

    private DeepLink createDynamicDeepLink(final Place place,
            final Instant periodStartTime,
            final Duration qrCodeRenewalIntervalDuration) {
        try {
            return DeepLink.builder()
                    .url(new URL(place.getLocation().newDeepLink(periodStartTime)))
                    .validityStartTime(periodStartTime)
                    .renewalInterval(qrCodeRenewalIntervalDuration)
                    .build();
        } catch (CleaCryptoException | MalformedURLException e) {
            log.warn(e.getMessage());
        }
        return null;
    }

    private DeepLink createStaticDeepLink(final Place place,
            final Instant periodStartTime) {
        try {
            return DeepLink.builder()
                    .url(new URL(place.getLocation().newDeepLink(periodStartTime)))
                    .validityStartTime(periodStartTime)
                    .renewalInterval(Duration.of(0, ChronoUnit.DAYS))
                    .build();
        } catch (CleaCryptoException | MalformedURLException e) {
            log.warn(e.getMessage());
        }
        return null;
    }
}
