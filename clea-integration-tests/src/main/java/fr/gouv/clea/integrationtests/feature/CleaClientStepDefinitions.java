package fr.gouv.clea.integrationtests.feature;

import fr.gouv.clea.ApiException;
import fr.gouv.clea.integrationtests.config.ApplicationProperties;
import fr.gouv.clea.integrationtests.feature.context.ScenarioContext;
import fr.gouv.clea.integrationtests.feature.context.Visitor;
import fr.gouv.clea.integrationtests.model.malformed.PivotDateTypeReportRequest;
import fr.gouv.clea.integrationtests.service.CleaBatchService;
import fr.gouv.clea.integrationtests.utils.CleaApiResponseParser;
import fr.gouv.clea.model.ErrorResponse;
import fr.gouv.clea.model.ReportResponse;
import fr.gouv.clea.qr.LocationQrCodeGenerator;
import fr.gouv.clea.qr.model.QRCode;
import fr.inria.clea.lsp.exception.CleaCryptoException;
import fr.inria.clea.lsp.exception.CleaEncodingException;
import io.cucumber.core.exception.CucumberException;
import io.cucumber.java.ParameterType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.java8.En;
import io.minio.errors.*;
import io.restassured.http.ContentType;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.assertj.core.api.Assertions;
import org.ocpsoft.prettytime.nlp.PrettyTimeParser;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class CleaClientStepDefinitions implements En {

    private final ScenarioContext scenarioContext;

    private final CleaBatchService cleaBatchService;

    private final ApplicationProperties applicationProperties;

    public CleaClientStepDefinitions(final ScenarioContext scenarioContext, final CleaBatchService cleaBatchService,
            final ApplicationProperties applicationProperties) {
        this.scenarioContext = Objects.requireNonNull(scenarioContext, "scenarioAppContext must not be null");
        this.cleaBatchService = cleaBatchService;
        this.applicationProperties = applicationProperties;
    }

    // TODO Robert registration of the user -> integration tests perimeters to be
    // specified, do we need to test interactions between all apps?
    @Given("{string} registered on TAC")
    public void registered_on_tac(final String username) {
        this.scenarioContext.getOrCreateUser(username);
    }

    @Given("VType of {string}, VCategory1 of {string} and VCategory2 of {int} has risk configuration of \\(Threshold , ExposureTime, Risklevel) for backward \\({int},{int},{float}) and for forward \\({int},{int},{float})")
    public void create_or_update_venue_with_specific_configuration(String venueType, String venueCategory1,
            Integer venueCategory2, int backwardThreshold, int backwardExposureTime, float backwardRiskLevel,
            int forwardThreshold, int forwardExposureTime, float forwardRiskLevel) {
        this.scenarioContext.updateOrCreateRiskConfig(
                venueType, venueCategory1, venueCategory2, backwardThreshold, backwardExposureTime, backwardRiskLevel,
                forwardThreshold, forwardExposureTime, forwardRiskLevel
        );
    }

    // Dynamic Location
    @Given("{string} created a dynamic QRCode at {instant} with VType as {string} and with VCategory1 as {string} and with VCategory2 as {int} and with a renewal time of \"{int} {word}\" and with a periodDuration of \"{int} hours\"")
    public void dynamic_location_with_a_periodDuration_and_renewalTime(String locationName, Instant periodStartTime,
            String venueType, String venueCategory1, Integer venueCategory2, Integer qrCodeRenewalInterval,
            String qrCodeRenewalIntervalUnit, Integer periodDuration) throws CleaCryptoException {
        Duration qrCodeRenewalIntervalDuration = Duration
                .of(qrCodeRenewalInterval, ChronoUnit.valueOf(qrCodeRenewalIntervalUnit.toUpperCase()));
        LocationQrCodeGenerator location = this.scenarioContext.getOrCreateDynamicLocation(
                locationName, periodStartTime, venueType, venueCategory1, venueCategory2, qrCodeRenewalIntervalDuration,
                periodDuration
        );
        // TODO: add QR id
    }

    @Given("{string} created a dynamic QRCode at {instant} with VType as {string} and with VCategory1 as {string} and with VCategory2 as {int} and with and with a renewal time of \"{int} {word}\"")
    public void dynamic_location_without_periodDuration_with_renewalTime(String locationName, Instant periodStartTime,
            String venueType, String venueCategory1, Integer venueCategory2, Integer qrCodeRenewalInterval,
            String qrCodeRenewalIntervalUnit) throws CleaCryptoException {
        Duration qrCodeRenewalIntervalDuration = Duration
                .of(qrCodeRenewalInterval, ChronoUnit.valueOf(qrCodeRenewalIntervalUnit.toUpperCase()));
        LocationQrCodeGenerator location = this.scenarioContext.getOrCreateDynamicLocation(
                locationName, periodStartTime, venueType, venueCategory1, venueCategory2, qrCodeRenewalIntervalDuration
        );
        // TODO: add QR id
    }

    @Given("{string} created a static QRCode at {instant} with VType as {string} and with VCategory1 as {string} and with VCategory2 as {int} and with a periodDuration of \"{int} hours\"")
    public void static_location_without_renewalTime_with_periodDuration(String locationName, Instant periodStartTime,
            String venueType, String venueCategory1, Integer venueCategory2, Integer periodDuration)
            throws CleaCryptoException {
        LocationQrCodeGenerator location = this.scenarioContext.getOrCreateStaticLocation(
                locationName, periodStartTime, venueType, venueCategory1, venueCategory2, periodDuration
        );
        // TODO: add QR id
    }

    @Given("{string} created a static QRCode at {instant} with VType as {string} and VCategory1 as {string} and with VCategory2 as {int}")
    public void static_location_with_default_periodDuration(String locationName, Instant periodStartTime,
            String venueType, String venueCategory1, Integer venueCategory2) throws CleaCryptoException {
        LocationQrCodeGenerator location = this.scenarioContext
                .getOrCreateStaticLocation(locationName, periodStartTime, venueType, venueCategory1, venueCategory2);
        // TODO: add QR id
    }

    // Visitor scan a QR code at given instant
    @Given("{string} recorded a visit to {string} at {instant}")
    public void visitor_scans_qrcode_at_given_instant(String visitorName, String locationName, Instant qrCodeScanTime)
            throws CleaCryptoException {
        LocationQrCodeGenerator location = this.scenarioContext.getLocation(locationName);
        QRCode qr = location.getQrCodeAt(qrCodeScanTime);
        this.scenarioContext.getOrCreateUser(visitorName).scanQrCode(qr.getQrCode(), qrCodeScanTime);
    }

    // Visitor scan a staff QR code at given instant
    @Given("{string} recorded a visit as a STAFF to {string} at {instant}")
    public void visitor_scans_staff_qrcode_at_given_instant(String visitorName, String locationName,
            Instant qrCodeScanTime) throws CleaCryptoException {
        LocationQrCodeGenerator location = this.scenarioContext.getStaffLocation(locationName);
        QRCode qr = location.getQrCodeAt(qrCodeScanTime);
        this.scenarioContext.getOrCreateUser(visitorName).scanQrCode(qr.getQrCode(), qrCodeScanTime);
    }

    // Visitor scan a QR code at a given Instant, but the scanned QR code is valid
    // for another Instant
    @Given("{string} recorded a visit to {string} at {instant} with a QR code valid for {string}")
    public void visitor_scans_qrcode_at_given_instant_but_qr_code_valid_for_another_instant(String visitorName,
            String locationName, Instant qrCodeScanTime, Instant qrCodeValidTime) throws CleaCryptoException {
        LocationQrCodeGenerator location = this.scenarioContext.getLocation(locationName);
        QRCode qr = location.getQrCodeAt(qrCodeValidTime);
        this.scenarioContext.getOrCreateUser(visitorName).scanQrCode(qr.getQrCode(), qrCodeScanTime);
    }

    @When("Cluster detection triggered")
    public void trigger_cluster_identification() throws IOException, InterruptedException {
        cleaBatchService.triggerNewClusterIdenfication();
    }

    @When("{string} declares himself/herself sick")
    public void visitor_declares_himself_sick(String visitorName) throws ApiException {
        final Visitor visitor = this.scenarioContext.getVisitor(visitorName);
        visitor.sendReportAndSaveResponse();
    }

    @When("{string} declares himself/herself sick with a {instant} pivot date")
    public void visitor_declares_sick(String visitorName, Instant pivotDate) throws ApiException {
        final Visitor visitor = this.scenarioContext.getVisitor(visitorName);
        visitor.sendReportAndSaveResponse(pivotDate);
    }

    @When("{string} declares himself/herself sick with a {instant} pivot date with no QRCode")
    public void visitor_declares_sick_with_pivot_date_and_no_qrCode(String visitorName, Instant pivotDate) {
        final Visitor visitor = this.scenarioContext.getVisitor(visitorName);
        Assertions.assertThatThrownBy(
                () -> visitor.sendReportAndSaveResponse(pivotDate), String.valueOf(ApiException.class)
        );
    }

    @When("{string} declares himself/herself sick with malformed QrCode")
    public void visitor_declares_sick_with_malformed_qrCode(String visitorName) {
        final Instant pivotDate = Instant.now().minus(Duration.ofDays(13));
        final Visitor visitor = this.scenarioContext.getVisitor(visitorName);
        Assertions.assertThatThrownBy(() -> visitor.sendReportWithEmptyQrCodeField(pivotDate))
                .isInstanceOf(ApiException.class);
    }

    @When("{string} declares himself/herself sick with malformed scan time")
    public void visitor_declares_sick_with_malformed_scanTime(String visitorName) throws ApiException {
        final Instant pivotDate = Instant.now().minus(Duration.ofDays(13));
        final Visitor visitor = this.scenarioContext.getVisitor(visitorName);
        visitor.sendReportWithMalformedScanTime(pivotDate);
    }

    @When("{string} declares himself/herself sick with no scan time")
    public void visitor_declares_sick_with_no_scanTime(String visitorName) {
        final Instant pivotDate = Instant.now().minus(Duration.ofDays(13));
        final Visitor visitor = this.scenarioContext.getVisitor(visitorName);
        Assertions.assertThatThrownBy(() -> visitor.sendReportWithNullScanTime(pivotDate))
                .isInstanceOf(ApiException.class);
    }

    @When("{string} declares himself/herself sick with no QrCode")
    public void visitor_declares_sick_with_no_qrCode(String visitorName) {
        final Instant pivotDate = Instant.now().minus(Duration.ofDays(13));
        final Visitor visitor = this.scenarioContext.getVisitor(visitorName);
        Assertions.assertThatThrownBy(() -> visitor.sendReportWithEmptyQrCodeField(pivotDate))
                .isInstanceOf(ApiException.class);
    }

    @When("{string} asks for exposure status")
    public void visitor_asks_for_exposure_status(final String visitorName) {
        noOp();
    }

    @Then("Exposure status should reports {string} as not being at risk")
    public void visitor_should_not_be_at_risk(String visitorName) throws CleaEncodingException, IOException,
            ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException,
            InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        float riskLevel = this.scenarioContext.getOrCreateUser(visitorName).getStatus();
        assertThat(riskLevel).isEqualTo(0);
    }

    @Then("Exposure status should reports {string} as being at risk of {float}")
    public void visitor_should_be_at_specified_risk(String visitorName, Float risk) throws CleaEncodingException,
            IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException,
            InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        float riskLevel = this.scenarioContext.getVisitor(visitorName).getStatus();
        assertThat(riskLevel).isEqualTo(risk);
    }

    @Then("Exposure status request for {string} should include only {int} visit\\(s) to {string} at {string}")
    public void visitor_should_include_only_(String visitorName, Integer nbVisits, String locationName,
            String qrScanTime) {
        final Visitor visitor = this.scenarioContext.getVisitor(visitorName);
        assertThat(visitor.getLocalList().size()).isEqualTo(nbVisits);
    }

    @Then("{string} has {int} rejected visit(s)")
    public void visitor_has_precise_rejected_visits(String visitorName, Integer rejectedVisits) {
        final Visitor visitor = this.scenarioContext.getVisitor(visitorName);
        final Optional<ReportResponse> lastReportResponseOptional = visitor.getLastReportResponse();
        if (lastReportResponseOptional.isPresent() && null != lastReportResponseOptional.get().getMessage()) {
            assertThat(CleaApiResponseParser.getRejectedVisits(lastReportResponseOptional.get().getMessage()))
                    .isEqualTo(rejectedVisits);
        } else {
            throw new CucumberException("last report response does not contain any message");
        }
    }

    @Then("{string} cannot send his/her visits")
    public void visitor_cannot_send_visits(String visitorName) {
        final Visitor visitor = this.scenarioContext.getVisitor(visitorName);
        final Optional<ReportResponse> lastReportResponse = visitor.getLastReportResponse();
        lastReportResponse.ifPresentOrElse(
                response -> assertThat(response.getSuccess()).isFalse(),
                () -> assertThat(lastReportResponse).isEmpty()
        );
    }

    @Then("{string} sends his/her visits")
    public void visitor_sends_visits(String visitorName) {
        final Visitor visitor = this.scenarioContext.getVisitor(visitorName);
        visitor.getLastReportResponse().ifPresent(response -> assertThat(response.getSuccess()).isTrue());
    }

    @When("{string} declares himself/herself sick with malformed pivot date")
    public void visitor_declares_sick_with_malformed_pivotDate(String visitorName) {
        final Visitor visitor = this.scenarioContext.getVisitor(visitorName);
        final String reportUrl = applicationProperties.getWsRest().getBaseUrl().toString()
                .concat("/api/clea/v1/wreport");
        final ErrorResponse apiReportErrorResponse = given()
                .contentType(ContentType.JSON)
                .body(new PivotDateTypeReportRequest("malformed", visitor.getLocalList()))
                .when()
                .post(reportUrl)
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .extract()
                .as(ErrorResponse.class);
        assertThat(apiReportErrorResponse.getHttpStatus()).isNotNull()
                .isEqualTo(String.valueOf(HttpStatus.SC_BAD_REQUEST));
        assertThat(apiReportErrorResponse.getMessage()).isEqualTo("JSON parse error");
    }

    private static void noOp() {
        // no operations cucumber method for "context" phrases in gherkin files
    }

    @ParameterType(".*")
    public Instant instant(final String naturalLanguage) {
        final String item = naturalLanguage;
        final String truc = "truc";
        return new PrettyTimeParser().parse(item).get(0).toInstant();
    }

}
