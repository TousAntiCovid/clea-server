package fr.gouv.clea.integrationtests.cucumber.steps;

import fr.gouv.clea.integrationtests.cucumber.ScenarioContext;
import io.cucumber.java.en.Given;
import lombok.RequiredArgsConstructor;

import java.time.Duration;

@RequiredArgsConstructor
public class CleaPlacesSteps {

    private final ScenarioContext scenarioContext;

    @Given("Place named {string} with venue type {int}, venue category 1 {int}, venue category 2 {int}," +
            " qr code renewal duration of {duration}, and a periodDuration of {int} hours")
    public void create_dynamic_place_with_vType_vCat1_vCat2_qrCodeRenewalDuration_and_periodDuration(
            final String locationName,
            final Integer venueType,
            final Integer venueCategory1,
            final Integer venueCategory2,
            final Duration qrCodeRenewalDuration,
            final Integer periodDuration) {
        scenarioContext.createDynamicPlace(
                locationName,
                venueType,
                venueCategory1,
                venueCategory2,
                qrCodeRenewalDuration,
                periodDuration
        );
    }

    @Given("Place named {string} has configuration: venue type {int}, venue category 1 {int}, venue category 2 {int}, and a periodDuration of {int} hours")
    public void create_static_place_with_vType_vCat1_vCat2_and_periodDuration(
            final String locationName,
            final Integer venueType,
            final Integer venueCategory1,
            final Integer venueCategory2,
            final Integer periodDuration) {
        scenarioContext.createStaticPlace(
                locationName,
                venueType,
                venueCategory1,
                venueCategory2,
                periodDuration
        );
    }
}
