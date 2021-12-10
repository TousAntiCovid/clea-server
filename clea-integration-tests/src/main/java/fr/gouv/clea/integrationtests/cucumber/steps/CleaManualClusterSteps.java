package fr.gouv.clea.integrationtests.cucumber.steps;

import fr.gouv.clea.integrationtests.config.ApplicationProperties;
import fr.gouv.clea.integrationtests.cucumber.ScenarioContext;
import fr.inria.clea.lsp.exception.CleaCryptoException;
import io.cucumber.java.en.When;
import io.restassured.http.ContentType;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static io.restassured.RestAssured.given;

public class CleaManualClusterSteps {

    private final URL cleaManualClusterDeclarationtUrl;

    private final ScenarioContext scenarioContext;

    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

    public CleaManualClusterSteps(ScenarioContext scenarioContext, ApplicationProperties applicationProperties)
            throws MalformedURLException {
        this.scenarioContext = scenarioContext;
        this.cleaManualClusterDeclarationtUrl = new URL(
                applicationProperties.getVenueConsumer().getBaseUrl(), "/cluster-declaration"
        );
    }

    @When("a manual cluster report is made for {string} at {naturalTime}")
    public void create_cluster_manually(final String locationName, final Instant clusterScanTime)
            throws CleaCryptoException {
        final var location = this.scenarioContext.getLocation(locationName);
        final var deeplink = location.getQrCodeAt(clusterScanTime).getDeepLink().toString();

        given()
                .contentType(ContentType.URLENC)
                .params(
                        Map.of(
                                "deeplink", deeplink,
                                "date", clusterScanTime.toString()
                        )
                )

                .when()
                .post(cleaManualClusterDeclarationtUrl)

                .then()
                .statusCode(302);

    }

}
