package fr.gouv.clea.integrationtests.cucumber.steps;

import fr.gouv.clea.integrationtests.cucumber.ScenarioContext;
import io.cucumber.java.en.Given;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class CleaVisitorUserSteps {

    private final ScenarioContext scenarioContext;

    @Given("{word} registered on TAC")
    public void registered_on_tac(final String username) {
        scenarioContext.getOrCreateUser(username);
    }

    @Given("users {wordList} are registered on TAC")
    public void list_registered_on_tac(final List<String> usernames) {
        usernames.forEach(scenarioContext::getOrCreateUser);
    }

    @Given("{word} recorded a visit to {string} at {naturalTime}")
    public void visitor_scans_deepLink_at_given_instant(final String visitorName,
            final String placeName,
            final Instant scanTime) {
        final var deepLink = scenarioContext.getPlace(placeName)
                .getDeepLinkAt(scanTime)
                .getUrl();
        scenarioContext.getVisitor(visitorName)
                .scans(deepLink, scanTime);
    }

    @Given("{word} recorded a visit as a STAFF to {string} at {naturalTime}")
    public void visitor_scans_staff_deepLink_at_given_instant(final String visitorName,
            final String placeName,
            final Instant scanTime) {
        final var deepLink = scenarioContext.getPlace(placeName)
                .getDeepLinkAt(scanTime)
                .getUrl();
        scenarioContext.getVisitor(visitorName)
                .scans(deepLink, scanTime);
    }
}
