package fr.gouv.clea.integrationtests.cucumber.steps;

import fr.gouv.clea.integrationtests.cucumber.ScenarioContext;
import io.cucumber.java.en.Given;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

@Slf4j
@AllArgsConstructor
public class CleaDeepLinksSteps {

    private final ScenarioContext scenarioContext;

    @Given("{string} created a dynamic deeplink at {naturalTime}")
    public void create_dynamic_deeplink_at_periodStartTime_with_renewalTime(final String locationName,
            final Instant qrCodePeriodStartTime) {
        scenarioContext.getPlace(locationName)
                .orElseThrow()
                .createDynamicDeepLinkAt(qrCodePeriodStartTime);
    }

    @Given("{string} initialized dynamic deeplink at {naturalTime}")
    public void init_dynamic_deeplink_at_instant(final String locationName,
            final Instant dynamicDeepLinkStartTime) {
        final var place = scenarioContext.getPlace(locationName).orElseThrow();
        assert place.getLocationDeepLinks().isEmpty();
        place.createDynamicDeepLinkAt(dynamicDeepLinkStartTime);
    }

    @Given("{string} created a dynamic staff deeplink at {naturalTime}")
    public void create_dynamic_staff_deeplink_at_periodStartTime_with_renewalTime(final String locationName,
            final Instant deepLinkPeriodStartTime) {
        scenarioContext.getPlace(locationName)
                .orElseThrow()
                .createDynamicStaffDeepLinkAt(deepLinkPeriodStartTime);
    }

    @Given("{string} created a static deeplink at {naturalTime}")
    public void create_static_deeplink_with_periodStartTime_without_qrCodeRenewalIntervalDuration(
            final String locationName,
            final Instant periodStartTime) {
        scenarioContext.getPlace(locationName)
                .orElseThrow()
                .createStaticDeepLink(periodStartTime);
    }

    @Given("{string} created a static staff deeplink at {naturalTime}")
    public void create_static_staff_deeplink_with_periodStartTime_without_qrCodeRenewalIntervalDuration(
            final String locationName,
            final Instant periodStartTime) {
        scenarioContext.getPlace(locationName)
                .orElseThrow()
                .createStaticStaffDeepLink(periodStartTime);
    }
}
