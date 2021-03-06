package fr.gouv.clea.integrationtests.cucumber.steps;

import fr.gouv.clea.integrationtests.config.ApplicationProperties;
import fr.gouv.clea.integrationtests.cucumber.ScenarioContext;
import fr.gouv.clea.integrationtests.service.qrcodesimulator.LocationSpec;
import io.cucumber.java.en.Given;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.time.Instant;

@RequiredArgsConstructor
public class CleaPlacesSteps {

    private final ApplicationProperties applicationProperties;

    private final ScenarioContext scenarioContext;

    @Given("{string} manager configured qrcode generators at {naturalTime} with venue type {int}, venue category 1 {int}, venue category 2 {int},"
            + " deepLink renewal duration of {duration}, and a periodDuration of {int} hours")
    public void create_dynamic_place_with_vType_vCat1_vCat2_deepLinkRenewalDuration_and_periodDuration(
            final String locationName,
            final Instant dynamicDeepLinkStartTime,
            final Integer venueType,
            final Integer venueCategory1,
            final Integer venueCategory2,
            final Duration deepLinkRenewalDuration,
            final Integer periodDuration) {
        scenarioContext.createPlace(
                locationName, LocationSpec.builder(applicationProperties)
                        .startTime(dynamicDeepLinkStartTime)
                        .venueConfig(venueType, venueCategory1, venueCategory2)
                        .periodDurationHours(periodDuration)
                        .renewalIntervalSeconds((int) deepLinkRenewalDuration.toSeconds())
        );
    }

    @Given("{string} manager generated qrcodes at {naturalTime} has configuration: venue type {int}, venue category 1 {int}, venue category 2 {int}")
    public void create_static_place_with_vType_vCat1_vCat2_and_periodDuration(
            final String locationName,
            final Instant deepLinkStartTime,
            final Integer venueType,
            final Integer venueCategory1,
            final Integer venueCategory2) {
        scenarioContext.createPlace(
                locationName, LocationSpec.builder(applicationProperties)
                        .startTime(deepLinkStartTime)
                        .venueConfig(venueType, venueCategory1, venueCategory2)
        );
    }
}
