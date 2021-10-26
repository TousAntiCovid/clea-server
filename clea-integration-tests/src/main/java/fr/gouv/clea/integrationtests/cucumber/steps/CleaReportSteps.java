package fr.gouv.clea.integrationtests.cucumber.steps;

import fr.gouv.clea.integrationtests.config.ApplicationProperties;
import fr.gouv.clea.integrationtests.cucumber.ScenarioContext;
import fr.gouv.clea.integrationtests.service.visitorsimulator.WreportRequest;
import fr.gouv.clea.integrationtests.service.visitorsimulator.WreportResponse;
import fr.gouv.clea.integrationtests.utils.CleaApiResponseParser;
import fr.inria.clea.lsp.utils.TimeUtils;
import io.cucumber.core.exception.CucumberException;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.http.ContentType;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class CleaReportSteps {

    private final ScenarioContext scenarioContext;

    private final URL cleaReportUrl;

    public CleaReportSteps(ScenarioContext scenarioContext, ApplicationProperties applicationProperties)
            throws MalformedURLException {
        this.scenarioContext = scenarioContext;
        this.cleaReportUrl = new URL(applicationProperties.getWsRest().getBaseUrl(), "/api/clea/v1/wreport");
    }

    @When("{word} declares himself/herself sick")
    public void visitor_declares_himself_sick(String visitorName) {
        final var visitor = this.scenarioContext.getVisitor(visitorName);
        final var request = WreportRequest.builder()
                .pivotDate(TimeUtils.ntpTimestampFromInstant(Instant.now().minus(Duration.ofDays(14))))
                .visits(visitor.getLocalList())
                .build();

        final var response = given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post(cleaReportUrl)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .as(WreportResponse.class);
        visitor.setLastReportResponse(response);
    }

    @When("{word} declares himself/herself sick with a {naturalTime} pivot date")
    public void visitor_declares_sick(String visitorName, Instant pivotDate) {
        final var visitor = scenarioContext.getVisitor(visitorName);
        final var request = WreportRequest.builder()
                .pivotDate(TimeUtils.ntpTimestampFromInstant(pivotDate))
                .visits(visitor.getLocalList())
                .build();

        final var response = given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post(cleaReportUrl)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .as(WreportResponse.class);
        visitor.setLastReportResponse(response);
    }

    @When("{word} declares himself/herself sick with a {naturalTime} pivot date with no QRCode")
    public void visitor_declares_sick_with_pivot_date_and_no_deeplink(String visitorName, Instant pivotDate) {
        final var localList = this.scenarioContext.getVisitor(visitorName).getLocalList();
        final var request = new WreportRequest(
                TimeUtils.ntpTimestampFromInstant(pivotDate), localList.stream()
                        .map(visit -> visit.withDeepLinkLocationSpecificPart(""))
                        .collect(Collectors.toList())
        );

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post(cleaReportUrl)
                .then()
                .contentType(ContentType.JSON)
                .statusCode(400)
                .body("message", equalTo("Invalid request"));
    }

    @When("{word} declares himself/herself sick with malformed QrCode")
    public void visitor_declares_sick_with_malformed_deeplink(String visitorName) {
        final Instant pivotDate = Instant.now().minus(Duration.ofDays(13));
        final var localList = this.scenarioContext.getVisitor(visitorName).getLocalList();
        final var request = new WreportRequest(
                TimeUtils.ntpTimestampFromInstant(pivotDate),
                localList.stream()
                        .map(visit -> visit.withDeepLinkLocationSpecificPart("malformed"))
                        .collect(Collectors.toList())
        );
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post(cleaReportUrl)
                .then()
                .contentType(ContentType.JSON)
                .statusCode(500)
                .body("message", equalTo("Last unit does not have enough valid bits"));
    }

    @When("{word} declares himself/herself sick with malformed scan time")
    public void visitor_declares_sick_with_malformed_scanTime(String visitorName) {
        final Instant pivotDate = Instant.now().minus(Duration.ofDays(14));
        final var visitor = scenarioContext.getVisitor(visitorName);
        final var request = new WreportRequest(
                TimeUtils.ntpTimestampFromInstant(pivotDate),
                visitor.getLocalList().stream()
                        .map(visit -> visit.withScanTime(-1L))
                        .collect(Collectors.toList())
        );
        final var response = given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post(cleaReportUrl)
                .then()
                .contentType(ContentType.JSON)
                .statusCode(200)
                .body("message", equalTo("0/1 accepted visits"))
                .extract()
                .as(WreportResponse.class);
        visitor.setLastReportResponse(response);
    }

    @When("{word} declares himself/herself sick with no scan time")
    public void visitor_declares_sick_with_no_scanTime(String visitorName) {
        final Instant pivotDate = Instant.now().minus(Duration.ofDays(14));
        final var localList = this.scenarioContext.getVisitor(visitorName).getLocalList();
        final var request = new WreportRequest(
                TimeUtils.ntpTimestampFromInstant(pivotDate), localList.stream()
                        .map(visit -> visit.withScanTime(null))
                        .collect(Collectors.toList())
        );
        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post(cleaReportUrl)
                .then()
                .contentType(ContentType.JSON)
                .statusCode(200)
                .body("message", equalTo("0/1 accepted visits"));
    }

    @Then("{word} has {int} rejected visit(s)")
    public void visitor_has_precise_rejected_visits(String visitorName, Integer rejectedVisits) {
        final var visitor = this.scenarioContext.getVisitor(visitorName);
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
        final var visitor = this.scenarioContext.getVisitor(visitorName);
        final Optional<WreportResponse> lastReportResponse = visitor.getLastReportResponse();
        lastReportResponse.ifPresentOrElse(
                response -> assertThat(response.getSuccess()).isFalse(),
                () -> assertThat(lastReportResponse).isEmpty()
        );
    }

    @Then("{word} sends his/her visits")
    public void visitor_sends_visits(String visitorName) {
        final var visitor = this.scenarioContext.getVisitor(visitorName);
        visitor.getLastReportResponse().ifPresent(response -> assertThat(response.getSuccess()).isTrue());
    }

    @When("{word} declares himself/herself sick with malformed pivot date")
    public void visitor_declares_sick_with_malformed_pivotDate(String visitorName) {
        final var localList = this.scenarioContext.getVisitor(visitorName).getLocalList();

        given()
                .contentType(ContentType.JSON)
                .body(
                        Map.of(
                                "pivotDate", "malformed",
                                "visits", localList
                        )
                )
                .when()
                .post(cleaReportUrl)
                .then()
                .contentType(ContentType.JSON)
                .statusCode(400)
                .body("message", equalTo("JSON parse error"));
    }
}
