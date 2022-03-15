package fr.gouv.clea.integrationtests.cucumber.steps;

import fr.gouv.clea.integrationtests.cucumber.ScenarioContext;
import fr.gouv.clea.integrationtests.service.CleaBatchService;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class ClusterDetectionSteps {

    private final CleaBatchService cleaBatchService;

    private final ScenarioContext scenarioContext;

    @When("cluster detection triggered")
    public void trigger_cluster_identification() throws IOException, InterruptedException {
        cleaBatchService.triggerNewClusterIdenfication();
    }

    @When("{word} asks for exposure status")
    public void visitor_asks_for_exposure_status(final String visitorName) {
    }

    @Then("exposure status should reports {word} as not being at risk")
    public void visitor_should_not_be_at_risk(String visitorName) {
        final var riskLevel = this.scenarioContext.getOrCreateUser(visitorName).getStatus();
        assertThat(riskLevel).isEqualTo(0);
    }

    @Then("exposure status should reports {word} as being at risk of {float}")
    public void visitor_should_be_at_specified_risk(String visitorName, Float risk) {
        final var riskLevel = this.scenarioContext.getVisitor(visitorName).getStatus();
        assertThat(riskLevel).isEqualTo(risk);
    }

    @Then("exposure status request for {word} should include only {int} visit\\(s) to {string} at {string}")
    public void visitor_should_include_only_expected_visits(String visitorName, Integer nbVisits, String locationName,
            String qrScanTime) {
        final var visitor = this.scenarioContext.getVisitor(visitorName);
        assertThat(visitor.getLocalList().size()).isEqualTo(nbVisits);
    }
}
