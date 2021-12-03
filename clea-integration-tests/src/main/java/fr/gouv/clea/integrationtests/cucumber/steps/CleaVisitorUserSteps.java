package fr.gouv.clea.integrationtests.cucumber.steps;

import fr.gouv.clea.integrationtests.cucumber.ScenarioContext;
import fr.gouv.clea.integrationtests.model.DeepLink;
import io.cucumber.java.en.Given;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Predicate;

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
        scenarioContext.getPlace(locationName)
                .orElseThrow()
                .getLocationDeepLinks()
                .stream()
                .filter(isDeepLinkThatCovers(qrCodeScanTime))
                .findFirst()
                .ifPresent(deepLink -> scanDeepLink(visitorName, qrCodeScanTime, deepLink.getUrl()));
    }

    // Visitor scan a staff QR code at given instant
    @Given("{word} recorded a visit as a STAFF to {string} at {naturalTime}")
    public void visitor_scans_staff_qrcode_at_given_instant(final String visitorName,
            final String locationName,
            final Instant qrCodeScanTime) {
        scenarioContext.getPlace(locationName)
                .orElseThrow()
                .getLocationStaffDeepLinks()
                .stream()
                .filter(isDeepLinkThatCovers(qrCodeScanTime))
                .findFirst()
                .ifPresent(deepLink -> scanDeepLink(visitorName, qrCodeScanTime, deepLink.getUrl()));
    }

    private Predicate<DeepLink> isDeepLinkThatCovers(Instant qrCodeScanTime) {
        return deepLink -> isStaticDeepLinkThatCovers(qrCodeScanTime, deepLink) ||
                isDynamicDeepLinkThatCovers(qrCodeScanTime, deepLink);
    }

    private boolean isDynamicDeepLinkThatCovers(Instant qrCodeScanTime, DeepLink deepLink) {
        return isDynamicDeepLink(deepLink) && deepLink.getValidityStartTime().isBefore(qrCodeScanTime)
                && deepLink.getValidityStartTime().plus(deepLink.getRenewalInterval()).isAfter(qrCodeScanTime);
    }

    private boolean isDynamicDeepLink(final DeepLink deepLink) {
        return deepLink.getRenewalInterval() != Duration.ZERO;
    }

    private boolean isStaticDeepLinkThatCovers(Instant qrCodeScanTime, DeepLink deepLink) {
        return isStaticDeepLink(deepLink)
                && deepLink.getValidityStartTime().isBefore(qrCodeScanTime);
    }

    private boolean isStaticDeepLink(DeepLink deepLink) {
        return deepLink.getRenewalInterval() == Duration.ZERO;
    }

    private void scanDeepLink(String visitorName,
            Instant qrCodeScanTime,
            URL locationDeepLink) {
        scenarioContext.getOrCreateUser(visitorName)
                .registerDeepLink(locationDeepLink, qrCodeScanTime);
    }
}
