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
import java.time.format.DateTimeFormatter;

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
    public void create_cluster_manually(String locationName, Instant qrCodeScanTime) throws CleaCryptoException {
        final var location = this.scenarioContext.getLocation(locationName);
        final var deeplink = location.getQrCodeAt(qrCodeScanTime).getDeepLink().toString();
        var date = LocalDateTime.ofInstant(qrCodeScanTime, ZoneId.of("UTC")).format(formatter);
        MultiValueMap<String, String> clusterParams = new LinkedMultiValueMap<>();
        clusterParams.add("deeplink", deeplink);
        clusterParams.add("date", date);
        given().contentType(ContentType.URLENC).params(clusterParams).when().post(cleaManualClusterDeclarationtUrl)
                .then().statusCode(200);

    }

}
