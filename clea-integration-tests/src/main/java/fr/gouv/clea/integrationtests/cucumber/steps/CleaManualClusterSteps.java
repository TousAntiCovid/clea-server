package fr.gouv.clea.integrationtests.cucumber.steps;

import fr.gouv.clea.integrationtests.config.ApplicationProperties;
import fr.gouv.clea.integrationtests.cucumber.ScenarioContext;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.time.ZoneId;

import static io.cucumber.messages.internal.com.google.common.net.HttpHeaders.LOCATION;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.URLENC;
import static org.hamcrest.Matchers.endsWith;

@RequiredArgsConstructor
public class CleaManualClusterSteps {

    private final ApplicationProperties applicationProperties;

    private final ScenarioContext scenarioContext;

    @When("a manual cluster report is made for {string} at {naturalTime}")
    public void create_cluster_manually(final String locationName, final Instant clusterScanTime) {
        final var place = this.scenarioContext.getPlace(locationName);
        final var deeplink = place.getDeepLinkAt(clusterScanTime).getUrl().toString();

        given()
                .baseUri(applicationProperties.getVenueConsumer().getBaseUrl().toString())
                .contentType(URLENC)
                .params(
                        "deeplink", deeplink,
                        "dateTime", clusterScanTime.atZone(ZoneId.systemDefault()).toLocalDateTime().toString(),
                        "zoneId", ZoneId.systemDefault().toString()
                )
                .when()
                .post("/cluster-declaration")
                .then()
                .statusCode(302)
                .header(LOCATION, endsWith("/cluster-declaration?success=true"));

    }

}
