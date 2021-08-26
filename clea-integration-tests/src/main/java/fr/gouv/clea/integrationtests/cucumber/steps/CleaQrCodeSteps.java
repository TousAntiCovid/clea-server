package fr.gouv.clea.integrationtests.cucumber.steps;

import fr.gouv.clea.integrationtests.config.ApplicationProperties;
import fr.gouv.clea.integrationtests.cucumber.ScenarioContext;
import fr.gouv.clea.integrationtests.utils.LocationBuilder;
import io.cucumber.java.en.Given;

import java.time.Duration;
import java.time.Instant;

public class CleaQrCodeSteps {

    private final ScenarioContext scenarioContext;

    private final LocationBuilder defaultLocationBuilder;

    public CleaQrCodeSteps(ApplicationProperties applicationProperties, ScenarioContext scenarioContext) {
        this.scenarioContext = scenarioContext;
        defaultLocationBuilder = new LocationBuilder(
                applicationProperties.getServerAuthorityPublicKey(),
                applicationProperties.getManualContactTracingAuthorityPublicKey()
        );
    }

    @Given("{string} created a dynamic QRCode at {naturalTime} with VType as {int}, with VCategory1 as {int}, with VCategory2 as {int}, with a renewal time of {duration} and with a periodDuration of {int} hours")
    public void dynamic_location_with_a_periodDuration_and_renewalTime(String locationName, Instant periodStartTime,
            Integer venueType, Integer venueCategory1, Integer venueCategory2, Duration qrCodeRenewalIntervalDuration,
            Integer periodDurationInHours) {
        final var locationBuilder = defaultLocationBuilder
                .withRandomPermanentLocationSecretKey()
                .withPeriodStartTime(periodStartTime)
                .withRenewalIntervalInSeconds(qrCodeRenewalIntervalDuration.getSeconds())
                .withVenueParameters(venueType, venueCategory1, venueCategory2)
                .withPeriodDurationInHours(periodDurationInHours);
        scenarioContext.registerLocation(locationName, locationBuilder.buildPublic());
        scenarioContext.registerLocation(locationName + " [staff]", locationBuilder.buildStaff());
    }

    @Given("{string} created a dynamic QRCode at {naturalTime} with VType as {int} and with VCategory1 as {int} and with VCategory2 as {int} and with and with a renewal time of {duration}")
    public void dynamic_location_without_periodDuration_with_renewalTime(String locationName, Instant periodStartTime,
            int venueType, int venueCategory1, int venueCategory2, Duration qrCodeRenewalIntervalDuration) {
        final var locationBuilder = defaultLocationBuilder
                .withRandomPermanentLocationSecretKey()
                .withPeriodStartTime(periodStartTime)
                .withRenewalIntervalInSeconds(qrCodeRenewalIntervalDuration.getSeconds())
                .withVenueParameters(venueType, venueCategory1, venueCategory2);
        scenarioContext.registerLocation(locationName, locationBuilder.buildPublic());
        scenarioContext.registerLocation(locationName + " [staff]", locationBuilder.buildStaff());
    }

    @Given("{string} created a static QRCode at {naturalTime} with VType as {int}, with VCategory1 as {int}, with VCategory2 as {int} and with a periodDuration of {int} hours")
    public void static_location_without_renewalTime_with_periodDuration(String locationName, Instant periodStartTime,
            int venueType, int venueCategory1, int venueCategory2, int periodDurationInHours) {
        final var locationBuilder = defaultLocationBuilder
                .withRandomPermanentLocationSecretKey()
                .withPeriodStartTime(periodStartTime)
                .withVenueParameters(venueType, venueCategory1, venueCategory2)
                .withPeriodDurationInHours(periodDurationInHours);
        scenarioContext.registerLocation(locationName, locationBuilder.buildPublic());
        scenarioContext.registerLocation(locationName + " [staff]", locationBuilder.buildStaff());
    }

    @Given("{string} created a static QRCode at {naturalTime} with VType as {string} and VCategory1 as {int} and with VCategory2 as {int}")
    public void static_location_with_default_periodDuration(String locationName, Instant periodStartTime,
            int venueType, int venueCategory1, int venueCategory2) {
        final var locationBuilder = defaultLocationBuilder
                .withRandomPermanentLocationSecretKey()
                .withPeriodStartTime(periodStartTime)
                .withVenueParameters(venueType, venueCategory1, venueCategory2);
        scenarioContext.registerLocation(locationName, locationBuilder.buildPublic());
        scenarioContext.registerLocation(locationName + " [staff]", locationBuilder.buildStaff());
    }
}
