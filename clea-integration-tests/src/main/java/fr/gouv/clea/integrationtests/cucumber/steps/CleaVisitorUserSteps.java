package fr.gouv.clea.integrationtests.cucumber.steps;

import fr.gouv.clea.integrationtests.cucumber.ScenarioContext;
import io.cucumber.java.en.Given;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.time.Instant;

@Slf4j
@RequiredArgsConstructor
public class CleaVisitorUserSteps {

    private final ScenarioContext scenarioContext;

    @Given("{word} registered on TAC")
    public void registered_on_tac(final String username) {
        scenarioContext.getOrCreateUser(username);
    }

    // Visitor scan a QR code at given instant
    @Given("{word} recorded a visit to {string} at {naturalTime}")
    public void visitor_scans_qrcode_at_given_instant(final String visitorName,
            final String locationName,
            final Instant qrCodeScanTime) {

        final var place = scenarioContext.getPlace(locationName).orElseThrow();
        scanDeepLink(visitorName, qrCodeScanTime, place.getDeepLinkAt(qrCodeScanTime).getUrl());
    }

    // Visitor scan a staff QR code at given instant
    @Given("{word} recorded a visit as a STAFF to {string} at {naturalTime}")
    public void visitor_scans_staff_qrcode_at_given_instant(final String visitorName,
            final String locationName,
            final Instant qrCodeScanTime) {
        final var place = scenarioContext.getPlace(locationName).orElseThrow();
        scanDeepLink(visitorName, qrCodeScanTime, place.getDeepLinkAt(qrCodeScanTime).getUrl());
    }

    private void scanDeepLink(final String visitorName,
            final Instant qrCodeScanTime,
            final URL locationDeepLink) {
        scenarioContext.getOrCreateUser(visitorName).registerDeepLink(locationDeepLink, qrCodeScanTime);
    }
}
