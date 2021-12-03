package fr.gouv.clea.integrationtests.cucumber.steps;

import fr.gouv.clea.integrationtests.config.ApplicationProperties;
import fr.gouv.clea.integrationtests.cucumber.ScenarioContext;
import fr.gouv.clea.integrationtests.service.visitorsimulator.WreportRequest;
import fr.gouv.clea.integrationtests.service.visitorsimulator.WreportResponse;
import fr.gouv.clea.integrationtests.utils.CleaApiResponseParser;
import io.cucumber.core.exception.CucumberException;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static fr.inria.clea.lsp.utils.TimeUtils.ntpTimestampFromInstant;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.time.Duration.ofDays;
import static java.time.Instant.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class CleaReportSteps {

    private final ScenarioContext scenarioContext;

    private final URL cleaReportUrl;

    private final URL cleaHealthUrl;

    public CleaReportSteps(final ScenarioContext ctx, final ApplicationProperties applicationProperties)
            throws MalformedURLException {
        scenarioContext = ctx;
        cleaReportUrl = new URL(applicationProperties.getWsRest().getBaseUrl(), "/api/clea/v1/wreport");
        this.cleaHealthUrl = new URL(applicationProperties.getWsRest().getBaseUrl(), "/actuator/health");
    }

    @Given("application clea ws rest is ready")
    public void applicationCleaIsReady() {
        given()
                .when()
                .get(cleaHealthUrl)
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
    }

    @When("{word} declares himself/herself sick")
    public void visitor_declares_himself_sick(String visitorName) {
        final var visitor = scenarioContext.getVisitor(visitorName);
        final var request = WreportRequest.builder()
                .pivotDate(ntpTimestampFromInstant(now().minus(ofDays(14))))
                .visits(visitor.getLocalList())
                .build();

        final var response = given()
                .contentType(JSON)
                .body(request)
                .when()
                .post(cleaReportUrl)
                .then()
                .statusCode(200)
                .contentType(JSON)
                .extract()
                .as(WreportResponse.class);
        visitor.setLastReportResponse(response);
    }

    @When("{word} declares himself/herself sick with a {naturalTime} pivot date")
    public void visitor_declares_sick(String visitorName, Instant pivotDate) {
        final var visitor = scenarioContext.getVisitor(visitorName);
        final var request = WreportRequest.builder()
                .pivotDate(ntpTimestampFromInstant(pivotDate))
                .visits(visitor.getLocalList())
                .build();

        final var response = given()
                .contentType(JSON)
                .body(request)
                .when()
                .post(cleaReportUrl)
                .then()
                .statusCode(200)
                .contentType(JSON)
                .extract()
                .as(WreportResponse.class);
        visitor.setLastReportResponse(response);
    }

    @When("{word} declares himself/herself sick with a {naturalTime} pivot date with no QRCode")
    public void visitor_declares_sick_with_pivot_date_and_no_deeplink(String visitorName, Instant pivotDate) {
        final var localList = scenarioContext.getVisitor(visitorName).getLocalList();
        final var request = new WreportRequest(
                ntpTimestampFromInstant(pivotDate), localList.stream()
                        .map(visit -> visit.withDeepLinkLocationSpecificPart(""))
                        .collect(Collectors.toList())
        );

        given()
                .contentType(JSON)
                .body(request)
                .when()
                .post(cleaReportUrl)
                .then()
                .contentType(JSON)
                .statusCode(400)
                .body("message", equalTo("Invalid request"));
    }

    @When("{word} declares himself/herself sick with malformed QrCode")
    public void visitor_declares_sick_with_malformed_deeplink(String visitorName) {
        final Instant pivotDate = now().minus(ofDays(13));
        final var localList = scenarioContext.getVisitor(visitorName).getLocalList();
        final var request = new WreportRequest(
                ntpTimestampFromInstant(pivotDate),
                localList.stream()
                        .map(visit -> visit.withDeepLinkLocationSpecificPart("malformed"))
                        .collect(Collectors.toList())
        );
        given()
                .contentType(JSON)
                .body(request)
                .when()
                .post(cleaReportUrl)
                .then()
                .contentType(JSON)
                .statusCode(500)
                .body("message", equalTo("Last unit does not have enough valid bits"));
    }

    @When("{word} declares himself/herself sick with malformed scan time")
    public void visitor_declares_sick_with_malformed_scanTime(String visitorName) {
        final Instant pivotDate = now().minus(ofDays(14));
        final var visitor = scenarioContext.getVisitor(visitorName);
        final var request = new WreportRequest(
                ntpTimestampFromInstant(pivotDate),
                visitor.getLocalList().stream()
                        .map(visit -> visit.withScanTime(-1L))
                        .collect(Collectors.toList())
        );
        final var response = given()
                .contentType(JSON)
                .body(request)
                .when()
                .post(cleaReportUrl)
                .then()
                .contentType(JSON)
                .statusCode(200)
                .body("message", equalTo("0/1 accepted visits"))
                .extract()
                .as(WreportResponse.class);
        visitor.setLastReportResponse(response);
    }

    @When("{word} declares himself/herself sick with no scan time")
    public void visitor_declares_sick_with_no_scanTime(String visitorName) {
        final Instant pivotDate = now().minus(ofDays(14));
        final var localList = scenarioContext.getVisitor(visitorName).getLocalList();
        final var request = new WreportRequest(
                ntpTimestampFromInstant(pivotDate), localList.stream()
                        .map(visit -> visit.withScanTime(null))
                        .collect(Collectors.toList())
        );
        given()
                .contentType(JSON)
                .body(request)
                .when()
                .post(cleaReportUrl)
                .then()
                .contentType(JSON)
                .statusCode(200)
                .body("message", equalTo("0/1 accepted visits"));
    }

    @Then("{word} has {int} rejected visit(s)")
    public void visitor_has_precise_rejected_visits(String visitorName, Integer rejectedVisits) {
        final var visitor = scenarioContext.getVisitor(visitorName);
        final Optional<WreportResponse> lastReportResponseOptional = visitor.getLastReportResponse();
        if (lastReportResponseOptional.isPresent() && null != lastReportResponseOptional.get().getMessage()) {
            assertThat(CleaApiResponseParser.getRejectedVisits(lastReportResponseOptional.get().getMessage()))
                    .isEqualTo(rejectedVisits);
        } else {
            throw new CucumberException("last report response does not contain any message");
        }
    }

    @Then("{word} cannot send his/her visits")
    public void visitor_cannot_send_visits(String visitorName) {
        final var visitor = scenarioContext.getVisitor(visitorName);
        final Optional<WreportResponse> lastReportResponse = visitor.getLastReportResponse();
        lastReportResponse.ifPresentOrElse(
                response -> assertThat(response.getSuccess()).isFalse(),
                () -> assertThat(lastReportResponse).isEmpty()
        );
    }

    @Then("{word} sends his/her visits")
    public void visitor_sends_visits(String visitorName) {
        final var visitor = scenarioContext.getVisitor(visitorName);
        visitor.getLastReportResponse().ifPresent(response -> assertThat(response.getSuccess()).isTrue());
    }

    @When("{word} declares himself/herself sick with malformed pivot date")
    public void visitor_declares_sick_with_malformed_pivotDate(String visitorName) {
        final var localList = scenarioContext.getVisitor(visitorName).getLocalList();

        given()
                .contentType(JSON)
                .body(Map.of("pivotDate", "malformed", "visits", localList))
                .when()
                .post(cleaReportUrl)
                .then()
                .contentType(JSON)
                .statusCode(400)
                .body("message", equalTo("JSON parse error"));
    }
}
