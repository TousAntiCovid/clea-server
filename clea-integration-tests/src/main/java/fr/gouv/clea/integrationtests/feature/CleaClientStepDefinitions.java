package fr.gouv.clea.integrationtests.feature;

import fr.gouv.clea.integrationtests.config.ApplicationProperties;
import fr.gouv.clea.integrationtests.dto.PivotDateStringWreportRequest;
import fr.gouv.clea.integrationtests.dto.WreportRequest;
import fr.gouv.clea.integrationtests.dto.WreportResponse;
import fr.gouv.clea.integrationtests.feature.context.ScenarioContext;
import fr.gouv.clea.integrationtests.model.LocationStat;
import fr.gouv.clea.integrationtests.model.ReportStat;
import fr.gouv.clea.integrationtests.repository.LocationStatIndex;
import fr.gouv.clea.integrationtests.repository.ReportStatIndex;
import fr.gouv.clea.integrationtests.service.CleaBatchService;
import fr.gouv.clea.integrationtests.utils.CleaApiResponseParser;
import fr.inria.clea.lsp.exception.CleaCryptoException;
import fr.inria.clea.lsp.utils.TimeUtils;
import io.cucumber.core.exception.CucumberException;
import io.cucumber.java.ParameterType;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.minio.errors.*;
import io.restassured.http.ContentType;
import lombok.extern.slf4j.Slf4j;
import org.ocpsoft.prettytime.nlp.PrettyTimeParser;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static java.lang.Integer.parseInt;
import static java.util.Collections.frequency;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

@Slf4j
public class CleaClientStepDefinitions {

    private final ScenarioContext scenarioContext;

    private final CleaBatchService cleaBatchService;

    private final String wreportUrl;

    private final ReportStatIndex reportStatIndex;

    private final LocationStatIndex locationStatIndex;

    public CleaClientStepDefinitions(final ScenarioContext scenarioContext,
            final CleaBatchService cleaBatchService,
            final ApplicationProperties applicationProperties, ReportStatIndex reportStatIndex,
            LocationStatIndex locationStatIndex) {
        this.scenarioContext = scenarioContext;
        this.cleaBatchService = cleaBatchService;
        this.wreportUrl = applicationProperties.getWsRest().getBaseUrl().toString().concat("/api/clea/v1/wreport");
        this.reportStatIndex = reportStatIndex;
        this.locationStatIndex = locationStatIndex;
    }

    // TODO Robert registration of the user -> integration tests perimeters to be
    // specified, do we need to test interactions between all apps?
    @Given("{string} registered on TAC")
    public void registered_on_tac(final String username) {
        this.scenarioContext.getOrCreateUser(username);
    }

    @Given("VType of {int}, VCategory1 of {int} and VCategory2 of {int}")
    public void create_or_update_venue_with_specific_configuration(Integer venueType, Integer venueCategory1,
            Integer venueCategory2) {
        this.scenarioContext.updateOrCreateRiskConfig(venueType, venueCategory1, venueCategory2);
    }

    // Dynamic Location
    @Given("{string} created a dynamic QRCode at {instant} with VType as {int}, with VCategory1 as {int}, with VCategory2 as {int}, with a renewal time of \"{int} {word}\" and with a periodDuration of \"{int} hours\"")
    public void dynamic_location_with_a_periodDuration_and_renewalTime(String locationName, Instant periodStartTime,
            Integer venueType, Integer venueCategory1, Integer venueCategory2, Integer qrCodeRenewalInterval,
            String qrCodeRenewalIntervalUnit, Integer periodDuration) throws CleaCryptoException {
        final var qrCodeRenewalIntervalDuration = Duration
                .of(qrCodeRenewalInterval, ChronoUnit.valueOf(qrCodeRenewalIntervalUnit.toUpperCase()));
        this.scenarioContext.getOrCreateDynamicLocation(
                locationName, periodStartTime, venueType, venueCategory1, venueCategory2,
                qrCodeRenewalIntervalDuration,
                periodDuration
        );
        // TODO: add QR id
    }

    @Given("{string} created a dynamic QRCode at {instant} with VType as {int} and with VCategory1 as {int} and with VCategory2 as {int} and with and with a renewal time of \"{int} {word}\"")
    public void dynamic_location_without_periodDuration_with_renewalTime(String locationName, Instant periodStartTime,
            Integer venueType, Integer venueCategory1, Integer venueCategory2, Integer qrCodeRenewalInterval,
            String qrCodeRenewalIntervalUnit) throws CleaCryptoException {
        final var qrCodeRenewalIntervalDuration = Duration
                .of(qrCodeRenewalInterval, ChronoUnit.valueOf(qrCodeRenewalIntervalUnit.toUpperCase()));
        this.scenarioContext.getOrCreateDynamicLocation(
                locationName, periodStartTime, venueType, venueCategory1, venueCategory2,
                qrCodeRenewalIntervalDuration
        );
        // TODO: add QR id
    }

    @Given("{string} created a static QRCode at {instant} with VType as {int}, with VCategory1 as {int}, with VCategory2 as {int} and with a periodDuration of \"{int} hours\"")
    public void static_location_without_renewalTime_with_periodDuration(String locationName, Instant periodStartTime,
            Integer venueType, Integer venueCategory1, Integer venueCategory2, Integer periodDuration)
            throws CleaCryptoException {
        this.scenarioContext.getOrCreateStaticLocation(
                locationName, periodStartTime, venueType, venueCategory1, venueCategory2,
                periodDuration
        );
        // TODO: add QR id
    }

    @Given("{string} created a static QRCode at {instant} with VType as {string} and VCategory1 as {int} and with VCategory2 as {int}")
    public void static_location_with_default_periodDuration(String locationName, Instant periodStartTime,
            Integer venueType, Integer venueCategory1, Integer venueCategory2) throws CleaCryptoException {
        this.scenarioContext.getOrCreateStaticLocationWithUnlimitedDuration(
                locationName, periodStartTime, venueType, venueCategory1, venueCategory2
        );
        // TODO: add QR id
    }

    // Visitor scan a QR code at given instant
    @Given("{string} recorded a visit to {string} at {instant}")
    public void visitor_scans_qrcode_at_given_instant(String visitorName, String locationName, Instant qrCodeScanTime)
            throws CleaCryptoException {
        final var location = this.scenarioContext.getLocation(locationName);
        final var deepLink = location.getQrCodeAt(qrCodeScanTime);
        this.scenarioContext.getOrCreateUser(visitorName).registerDeepLink(deepLink.getQrCode(), qrCodeScanTime);
    }

    // Visitor scan a staff QR code at given instant
    @Given("{string} recorded a visit as a STAFF to {string} at {instant}")
    public void visitor_scans_staff_qrcode_at_given_instant(String visitorName, String locationName,
            Instant qrCodeScanTime) throws CleaCryptoException {
        final var location = this.scenarioContext.getStaffLocation(locationName);
        final var qr = location.getQrCodeAt(qrCodeScanTime);
        this.scenarioContext.getOrCreateUser(visitorName).registerDeepLink(qr.getQrCode(), qrCodeScanTime);
    }

    // Visitor scan a QR code at a given Instant, but the scanned QR code is valid
    // for another Instant
    @Given("{string} recorded a visit to {string} at {instant} with a QR code valid for {string}")
    public void visitor_scans_qrcode_at_given_instant_but_qr_code_valid_for_another_instant(String visitorName,
            String locationName, Instant qrCodeScanTime, Instant qrCodeValidTime) throws CleaCryptoException {
        final var location = this.scenarioContext.getLocation(locationName);
        final var qr = location.getQrCodeAt(qrCodeValidTime);
        this.scenarioContext.getOrCreateUser(visitorName).registerDeepLink(qr.getQrCode(), qrCodeScanTime);
    }

    @When("Cluster detection triggered")
    public void trigger_cluster_identification() throws IOException, InterruptedException {
        cleaBatchService.triggerNewClusterIdenfication();
    }

    @When("{string} declares himself/herself sick")
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
                .post(wreportUrl)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .as(WreportResponse.class);
        visitor.setLastReportResponse(response);
    }

    @When("{string} declares himself/herself sick with a {instant} pivot date")
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
                .post(wreportUrl)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .as(WreportResponse.class);
        visitor.setLastReportResponse(response);
    }

    @When("{string} declares himself/herself sick with a {instant} pivot date with no QRCode")
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
                .post(wreportUrl)
                .then()
                .contentType(ContentType.JSON)
                .statusCode(400)
                .body("message", equalTo("Invalid request"));
    }

    @When("{string} declares himself/herself sick with malformed QrCode")
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
                .post(wreportUrl)
                .then()
                .contentType(ContentType.JSON)
                .statusCode(500)
                .body("message", equalTo("Last unit does not have enough valid bits"));
    }

    @When("{string} declares himself/herself sick with malformed scan time")
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
                .post(wreportUrl)
                .then()
                .contentType(ContentType.JSON)
                .statusCode(200)
                .body("message", equalTo("0 reports processed, 1 rejected"))
                .extract()
                .as(WreportResponse.class);
        visitor.setLastReportResponse(response);
    }

    @When("{string} declares himself/herself sick with no scan time")
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
                .post(wreportUrl)
                .then()
                .contentType(ContentType.JSON)
                .statusCode(400)
                .body("message", equalTo("Invalid request"));
    }

    @When("{string} asks for exposure status")
    public void visitor_asks_for_exposure_status(final String visitorName) {
        noOp();
    }

    @Then("Exposure status should reports {string} as not being at risk")
    public void visitor_should_not_be_at_risk(String visitorName) throws IOException, ServerException,
            InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException,
            InvalidResponseException, XmlParserException, InternalException {
        final var riskLevel = this.scenarioContext.getOrCreateUser(visitorName).getStatus();
        assertThat(riskLevel).isEqualTo(0);
    }

    @Then("Exposure status should reports {string} as being at risk of {float}")
    public void visitor_should_be_at_specified_risk(String visitorName, Float risk) throws IOException, ServerException,
            InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException,
            InvalidResponseException, XmlParserException, InternalException {
        final var riskLevel = this.scenarioContext.getVisitor(visitorName).getStatus();
        assertThat(riskLevel).isEqualTo(risk);
    }

    @Then("Exposure status request for {string} should include only {int} visit\\(s) to {string} at {string}")
    public void visitor_should_include_only_expected_visits(String visitorName, Integer nbVisits, String locationName,
            String qrScanTime) {
        final var visitor = this.scenarioContext.getVisitor(visitorName);
        assertThat(visitor.getLocalList().size()).isEqualTo(nbVisits);
    }

    @Then("{string} has {int} rejected visit(s)")
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

    @Then("{string} cannot send his/her visits")
    public void visitor_cannot_send_visits(String visitorName) {
        final var visitor = this.scenarioContext.getVisitor(visitorName);
        final Optional<WreportResponse> lastReportResponse = visitor.getLastReportResponse();
        lastReportResponse.ifPresentOrElse(
                response -> assertThat(response.getSuccess()).isFalse(),
                () -> assertThat(lastReportResponse).isEmpty()
        );
    }

    @Then("{string} sends his/her visits")
    public void visitor_sends_visits(String visitorName) {
        final var visitor = this.scenarioContext.getVisitor(visitorName);
        visitor.getLastReportResponse().ifPresent(response -> assertThat(response.getSuccess()).isTrue());
    }

    @When("{string} declares himself/herself sick with malformed pivot date")
    public void visitor_declares_sick_with_malformed_pivotDate(String visitorName) {
        final var localList = this.scenarioContext.getVisitor(visitorName).getLocalList();
        final var request = new PivotDateStringWreportRequest("malformed", localList);

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post(wreportUrl)
                .then()
                .contentType(ContentType.JSON)
                .statusCode(400)
                .body("message", equalTo("JSON parse error"));
    }

    private static void noOp() {
        // no operations cucumber method for "context" phrases in gherkin files
    }

    @ParameterType(".*")
    public Instant instant(final String naturalLanguage) {
        return new PrettyTimeParser().parse(naturalLanguage).get(0).toInstant();
    }

    @And("statistics by location are")
    public void statisticsByLocationAre(List<Map<String, String>> expectedIndexContent) throws InterruptedException {
        // TODO: replace with kafka topics monitoring
        Thread.sleep(20000);
        expectedIndexContent.forEach(entry -> {
            Instant periodStart = new PrettyTimeParser().parse(entry.get("period_start")).get(0).toInstant();
            var stringStatPeriod = periodStart
                    .atOffset(ZoneOffset.UTC)
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            var id = String.format(
                    "%s-vt:%d-vc1:%d-vc2:%d",
                    stringStatPeriod,
                    parseInt(entry.get("venue_type")),
                    parseInt(entry.get("venue_category1")),
                    parseInt(entry.get("venue_category2"))
            );
            Optional<LocationStat> indexResponse = locationStatIndex.findByIdentifier(id, periodStart);

            assertThat(indexResponse).isNotEmpty();
        });
    }

    @Then("statistics by wreport are")
    public void statisticsByWreportAre(List<Map<String, Integer>> expectedIndexContent) throws InterruptedException {
        // TODO: replace with kafka topics monitoring
        Thread.sleep(20000);
        expectedIndexContent.stream().distinct().forEach(entry -> {
            List<ReportStat> indexResponse = reportStatIndex.findByReportedAndRejectedAndCloseAndBackwardsAndForwards(
                    entry.get("reported"),
                    entry.get("rejected"),
                    entry.get("is_closed"),
                    entry.get("backwards"),
                    entry.get("forwards")
            );
            assertThat(indexResponse).size().isEqualTo(frequency(expectedIndexContent, entry));
        });
        assertThat(reportStatIndex.count()).isEqualTo(expectedIndexContent.size());
    }
}
