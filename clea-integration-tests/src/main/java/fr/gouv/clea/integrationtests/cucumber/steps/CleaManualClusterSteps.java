package fr.gouv.clea.integrationtests.cucumber.steps;

import fr.gouv.clea.integrationtests.config.ApplicationProperties;
import fr.gouv.clea.integrationtests.cucumber.ScenarioContext;
import fr.inria.clea.lsp.exception.CleaCryptoException;
import io.cucumber.java.en.When;
import io.restassured.http.ContentType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static java.time.ZoneOffset.UTC;

public class CleaManualClusterSteps {

    private final URL cleaManualClusterDeclarationtUrl;

    private final ScenarioContext scenarioContext;

    public CleaManualClusterSteps(ScenarioContext scenarioContext, ApplicationProperties applicationProperties)
            throws MalformedURLException {
        this.scenarioContext = scenarioContext;
        this.cleaManualClusterDeclarationtUrl = new URL(
                applicationProperties.getVenueConsumer().getBaseUrl(), "/cluster-declaration"
        );
    }

    @When("a manual cluster report is made for {string} at {naturalTime}")
    public void create_cluster_manually(final String locationName, final Instant clusterStartTime)
            throws CleaCryptoException {
        final var location = this.scenarioContext.getLocation(locationName);
        final var deeplink = location.getQrCodeAt(clusterStartTime)
                .getDeepLink();

        given()
                .contentType(ContentType.URLENC)
                .params(Map.of(
                        "deeplink", deeplink.toString(),
                        "date", clusterStartTime.toString()
                ))

                .when()
                .post(cleaManualClusterDeclarationtUrl)

                .then()
                .statusCode(302);
    }

}
