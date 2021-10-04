package fr.gouv.clea.integrationtests.cucumber.steps;

import fr.gouv.clea.integrationtests.cucumber.ScenarioContext;
import fr.inria.clea.lsp.exception.CleaCryptoException;
import io.cucumber.java.en.Given;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@RequiredArgsConstructor
public class CleaVisitorUserSteps {

    private final ScenarioContext scenarioContext;

    // TODO Robert registration of the user -> integration tests perimeters to be
    // specified, do we need to test interactions between all apps?
    @Given("{word} registered on TAC")
    public void registered_on_tac(final String username) {
        this.scenarioContext.getOrCreateUser(username);
    }

    // Visitor scan a QR code at given instant
    @Given("{word} recorded a visit to {string} at {naturalTime}")
    public void visitor_scans_qrcode_at_given_instant(String visitorName, String locationName, Instant qrCodeScanTime)
            throws CleaCryptoException {
        final var location = this.scenarioContext.getLocation(locationName);
        final var deepLink = location.getQrCodeAt(qrCodeScanTime);
        this.scenarioContext.getOrCreateUser(visitorName).registerDeepLink(deepLink.getQrCode(), qrCodeScanTime);
    }

    // Visitor scan a staff QR code at given instant
    @Given("{word} recorded a visit as a STAFF to {string} at {naturalTime}")
    public void visitor_scans_staff_qrcode_at_given_instant(String visitorName, String locationName,
            Instant qrCodeScanTime) throws CleaCryptoException {
        final var location = this.scenarioContext.getStaffLocation(locationName);
        final var qr = location.getQrCodeAt(qrCodeScanTime);
        this.scenarioContext.getOrCreateUser(visitorName).registerDeepLink(qr.getQrCode(), qrCodeScanTime);
    }

    // Visitor scan a QR code at a given Instant, but the scanned QR code is valid
    // for another Instant
    @Given("{word} recorded a visit to {string} at {naturalTime} with a QR code valid for {string}")
    public void visitor_scans_qrcode_at_given_instant_but_qr_code_valid_for_another_instant(String visitorName,
            String locationName, Instant qrCodeScanTime, Instant qrCodeValidTime) throws CleaCryptoException {
        final var location = this.scenarioContext.getLocation(locationName);
        final var qr = location.getQrCodeAt(qrCodeValidTime);
        this.scenarioContext.getOrCreateUser(visitorName).registerDeepLink(qr.getQrCode(), qrCodeScanTime);
    }
}
