package fr.gouv.clea.integrationtests.cucumber.steps;

import fr.gouv.clea.integrationtests.cucumber.ScenarioContext;
import fr.inria.clea.lsp.exception.CleaCryptoException;
import io.cucumber.java.en.Given;
import lombok.AllArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@AllArgsConstructor
public class CleaQrCodeSteps {

    private final ScenarioContext scenarioContext;

    // Dynamic Location
    @Given("{string} created a dynamic QRCode at {instant} with VType as {int}, with VCategory1 as {int}, with VCategory2 as {int}, with a renewal time of \"{int} {word}\" and with a periodDuration of \"{int} hours\"")
    public void dynamic_location_with_a_periodDuration_and_renewalTime(String locationName, Instant periodStartTime,
            Integer venueType, Integer venueCategory1, Integer venueCategory2, Integer qrCodeRenewalInterval,
            String qrCodeRenewalIntervalUnit, Integer periodDuration) throws CleaCryptoException {
        final var qrCodeRenewalIntervalDuration = Duration
                .of(qrCodeRenewalInterval, ChronoUnit.valueOf(qrCodeRenewalIntervalUnit.toUpperCase()));
        this.scenarioContext.getOrCreateDynamicLocation(
                locationName, periodStartTime, venueType, venueCategory1, venueCategory2,
                qrCodeRenewalIntervalDuration,
                periodDuration
        );
        // TODO: add QR id
    }

    @Given("{string} created a dynamic QRCode at {instant} with VType as {int} and with VCategory1 as {int} and with VCategory2 as {int} and with and with a renewal time of \"{int} {word}\"")
    public void dynamic_location_without_periodDuration_with_renewalTime(String locationName, Instant periodStartTime,
            Integer venueType, Integer venueCategory1, Integer venueCategory2, Integer qrCodeRenewalInterval,
            String qrCodeRenewalIntervalUnit) throws CleaCryptoException {
        final var qrCodeRenewalIntervalDuration = Duration
                .of(qrCodeRenewalInterval, ChronoUnit.valueOf(qrCodeRenewalIntervalUnit.toUpperCase()));
        this.scenarioContext.getOrCreateDynamicLocation(
                locationName, periodStartTime, venueType, venueCategory1, venueCategory2,
                qrCodeRenewalIntervalDuration
        );
        // TODO: add QR id
    }

    @Given("{string} created a static QRCode at {instant} with VType as {int}, with VCategory1 as {int}, with VCategory2 as {int} and with a periodDuration of \"{int} hours\"")
    public void static_location_without_renewalTime_with_periodDuration(String locationName, Instant periodStartTime,
            Integer venueType, Integer venueCategory1, Integer venueCategory2, Integer periodDuration)
            throws CleaCryptoException {
        this.scenarioContext.getOrCreateStaticLocation(
                locationName, periodStartTime, venueType, venueCategory1, venueCategory2,
                periodDuration
        );
        // TODO: add QR id
    }

    @Given("{string} created a static QRCode at {instant} with VType as {string} and VCategory1 as {int} and with VCategory2 as {int}")
    public void static_location_with_default_periodDuration(String locationName, Instant periodStartTime,
            Integer venueType, Integer venueCategory1, Integer venueCategory2) throws CleaCryptoException {
        this.scenarioContext.getOrCreateStaticLocationWithUnlimitedDuration(
                locationName, periodStartTime, venueType, venueCategory1, venueCategory2
        );
        // TODO: add QR id
    }
}
