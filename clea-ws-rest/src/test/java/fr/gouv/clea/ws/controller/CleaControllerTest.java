package fr.gouv.clea.ws.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.gouv.clea.ws.test.IntegrationTest;
import fr.gouv.clea.ws.test.KafkaManager;
import fr.gouv.clea.ws.test.QrCode;
import fr.gouv.clea.ws.vo.ReportRequest;
import fr.gouv.clea.ws.vo.Visit;
import fr.inria.clea.lsp.utils.TimeUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static fr.gouv.clea.ws.test.RestAssuredManager.givenAuthenticated;
import static fr.gouv.clea.ws.test.TemporalMatchers.isStringDateBetweenNowAndTenSecondsAgo;
import static io.restassured.http.ContentType.JSON;
import static io.restassured.http.ContentType.TEXT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpStatus.*;

@IntegrationTest
class CleaControllerTest {

    @Test
    void infected_user_can_report_himself_as_infected() {
        final var visits = List.of(new Visit("qrCode", 0L));
        final var request = new ReportRequest(visits, 0L);

        givenAuthenticated()
                .contentType(JSON)
                .body(request)
                .post("/api/clea/v1/wreport")

                .then()
                .statusCode(OK.value());
    }

    @Test
    void invalid_content_type_body_causes_415_unsupported_media_type() {
        givenAuthenticated()
                .contentType(TEXT)
                .body("foo")
                .post("/api/clea/v1/wreport")

                .then()
                .statusCode(UNSUPPORTED_MEDIA_TYPE.value());
    }

    @Test
    void a_report_with_a_null_visits_list_causes_400_bad_request() {
        final var request = new ReportRequest(null, 0L);
        givenAuthenticated()
                .contentType(JSON)
                .body(request)
                .post("/api/clea/v1/wreport")

                .then()
                .statusCode(BAD_REQUEST.value());
    }

    @Test
    void a_report_with_malformed_body_causes_400_bad_request() {
        givenAuthenticated()
                .contentType(JSON)
                .body("{ \"id\": 1 }")
                .post("/api/clea/v1/wreport")

                .then()
                .statusCode(BAD_REQUEST.value());
    }

    @Test
    @DisplayName("when pivotDate is null, reject everything")
    void a_null_pivot_date_causes_400_bad_request() {
        final var visits = List.of(new Visit(RandomStringUtils.randomAlphanumeric(20), RandomUtils.nextLong()));
        final var request = new ReportRequest(visits, null);
        givenAuthenticated()
                .contentType(JSON)
                .body(request)
                .post("/api/clea/v1/wreport")

                .then()
                .statusCode(BAD_REQUEST.value())
                .body("httpStatus", equalTo(BAD_REQUEST.value()))
                .body("timestamp", isStringDateBetweenNowAndTenSecondsAgo())
                .body("message", equalTo("Invalid request"))
                .body("validationErrors[0].object", equalTo("ReportRequest"))
                .body("validationErrors[0].field", equalTo("pivotDateAsNtpTimestamp"))
                .body("validationErrors[0].rejectedValue", equalTo(null))
                .body("validationErrors[0].message", containsString("null"))
                .body("validationErrors", hasSize(1));
    }

    @Test
    @DisplayName("when pivotDate is not numeric, reject everything")
    void invalid_pivot_date_format_causes_400_bad_request() {
        givenAuthenticated()
                .contentType(JSON)
                .body(
                        Map.of(
                                "pivotDate", 0,
                                "visits", List.of(
                                        Map.of(
                                                "qrCode", RandomStringUtils.randomAlphanumeric(20),
                                                "qrCodeScanTime", "a"
                                        )
                                )
                        )
                )
                .post("/api/clea/v1/wreport")

                .then()
                .statusCode(BAD_REQUEST.value())
                .body("httpStatus", equalTo(BAD_REQUEST.value()))
                .body("timestamp", isStringDateBetweenNowAndTenSecondsAgo())
                .body("message", equalTo("JSON parse error"))
                .body("validationErrors", hasSize(0));
    }

    @Test
    @DisplayName("when visit list is null, reject everything")
    void a_report_with_a_null_visits_list_causes_400_bad_request_2() {
        final var request = new ReportRequest(null, 0L);
        givenAuthenticated()
                .contentType(JSON)
                .body(request)
                .post("/api/clea/v1/wreport")

                .then()
                .statusCode(BAD_REQUEST.value())
                .body("httpStatus", equalTo(BAD_REQUEST.value()))
                .body("timestamp", isStringDateBetweenNowAndTenSecondsAgo())
                .body("message", equalTo("Invalid request"))
                .body("validationErrors[0].object", equalTo("ReportRequest"))
                .body("validationErrors[0].field", equalTo("visits"))
                .body("validationErrors[0].rejectedValue", hasSize(0))
                .body("validationErrors[0].message", equalTo("must not be empty"))
                .body("validationErrors", hasSize(1));
    }

    @Test
    @DisplayName("when visit list is empty, reject everything")
    void a_report_with_an_empty_visits_list_causes_400_bad_request() {
        final var request = new ReportRequest(List.of(), 0L);
        givenAuthenticated()
                .contentType(JSON)
                .body(request)
                .post("/api/clea/v1/wreport")

                .then()
                .statusCode(BAD_REQUEST.value())
                .body("httpStatus", equalTo(BAD_REQUEST.value()))
                .body("timestamp", isStringDateBetweenNowAndTenSecondsAgo())
                .body("message", equalTo("Invalid request"))
                .body("validationErrors[0].object", equalTo("ReportRequest"))
                .body("validationErrors[0].field", equalTo("visits"))
                .body("validationErrors[0].rejectedValue", hasSize(0))
                .body("validationErrors[0].message", equalTo("must not be empty"))
                .body("validationErrors", hasSize(1));
    }

    @Test
    @DisplayName("when a qrCode is null reject just the visit")
    void a_visit_with_a_null_qrcode_is_ignored() {
        final var now = Instant.now();
        final var nowAsNtpTimestamp = TimeUtils.ntpTimestampFromInstant(now);
        final var visits = List.of(
                new Visit(QrCode.LOCATION_1_URL.getRef(), nowAsNtpTimestamp),
                new Visit(null, nowAsNtpTimestamp)
        );
        final var request = new ReportRequest(visits, 3L);
        givenAuthenticated()
                .contentType(JSON)
                .body(request)
                .post("/api/clea/v1/wreport")

                .then()
                .statusCode(OK.value())
                .body("success", is(true))
                .body("message", is("1 reports processed, 1 rejected"));

        final var records = KafkaManager.getRecords(1, "dev.clea.fct.visit-scan");

        assertThat(records)
                .extracting(ConsumerRecord::value)
                .extracting(value -> new ObjectMapper().convertValue(value, Map.class))
                .singleElement()
                .hasFieldOrPropertyWithValue(
                        "encryptedLocationMessage",
                        QrCode.LOCATION_1_LOCATION_SPECIFIC_PART_DECODED_BASE64
                )
                .hasFieldOrPropertyWithValue("isBackward", false)
                .hasFieldOrPropertyWithValue("qrCodeScanTime", now.getEpochSecond() * 1000)
                .hasFieldOrPropertyWithValue("type", 0)
                .hasFieldOrPropertyWithValue("version", 0);
    }

    @Test
    @DisplayName("when a qrCode is empty reject just the visit")
    void a_visit_with_an_empty_qrcode_is_ignored() {
        final var nowAsNtpTimestamp = TimeUtils.currentNtpTime();
        final var nowEpochMs = TimeUtils.instantFromTimestamp(nowAsNtpTimestamp).getEpochSecond() * 1000;
        final var visits = List.of(
                new Visit(QrCode.LOCATION_1_URL.getRef(), nowAsNtpTimestamp),
                new Visit("", 2L)
        );
        final var request = new ReportRequest(visits, 3L);

        givenAuthenticated()
                .contentType(JSON)
                .body(request)
                .post("/api/clea/v1/wreport")

                .then()
                .statusCode(OK.value())
                .body("success", is(true))
                .body("message", is("1 reports processed, 1 rejected"));

        final var records = KafkaManager.getRecords(1, "dev.clea.fct.visit-scan");

        assertThat(records)
                .extracting(ConsumerRecord::value)
                .extracting(value -> new ObjectMapper().convertValue(value, Map.class))
                .singleElement()
                .hasFieldOrPropertyWithValue(
                        "encryptedLocationMessage",
                        QrCode.LOCATION_1_LOCATION_SPECIFIC_PART_DECODED_BASE64
                )
                .hasFieldOrPropertyWithValue("isBackward", false)
                .hasFieldOrPropertyWithValue("qrCodeScanTime", nowEpochMs)
                .hasFieldOrPropertyWithValue("type", 0)
                .hasFieldOrPropertyWithValue("version", 0);
    }

    @Test
    @DisplayName("when a qrCode is blank reject just the visit")
    void a_visit_with_a_blank_qrcode_is_ignored() {
        final var nowAsNtpTimestamp = TimeUtils.currentNtpTime();
        final var nowEpochMs = TimeUtils.instantFromTimestamp(nowAsNtpTimestamp).getEpochSecond() * 1000;
        final var visits = List.of(
                new Visit(QrCode.LOCATION_1_URL.getRef(), nowAsNtpTimestamp),
                new Visit("      ", 2L)
        );
        final var request = new ReportRequest(visits, 3L);

        givenAuthenticated()
                .contentType(JSON)
                .body(request)
                .post("/api/clea/v1/wreport")

                .then()
                .statusCode(OK.value())
                .body("success", is(true))
                .body("message", is("1 reports processed, 1 rejected"));

        final var records = KafkaManager.getRecords(1, "dev.clea.fct.visit-scan");

        assertThat(records)
                .extracting(ConsumerRecord::value)
                .extracting(value -> new ObjectMapper().convertValue(value, Map.class))
                .singleElement()
                .hasFieldOrPropertyWithValue(
                        "encryptedLocationMessage",
                        QrCode.LOCATION_1_LOCATION_SPECIFIC_PART_DECODED_BASE64
                )
                .hasFieldOrPropertyWithValue("isBackward", false)
                .hasFieldOrPropertyWithValue("qrCodeScanTime", nowEpochMs)
                .hasFieldOrPropertyWithValue("type", 0)
                .hasFieldOrPropertyWithValue("version", 0);
    }

    @Test
    @DisplayName("when a qrScan is null reject just the visit")
    void a_visit_with_a_null_qrcode_scantime_is_ignored() {
        final var nowAsNtpTimestamp = TimeUtils.currentNtpTime();
        final var nowEpochMs = TimeUtils.instantFromTimestamp(nowAsNtpTimestamp).getEpochSecond() * 1000;
        final var visits = List.of(
                new Visit(QrCode.LOCATION_1_URL.getRef(), nowAsNtpTimestamp),
                new Visit(QrCode.LOCATION_2_URL.getRef(), null)
        );
        final var request = new ReportRequest(visits, 3L);

        givenAuthenticated()
                .contentType(JSON)
                .body(request)
                .post("/api/clea/v1/wreport")

                .then()
                .statusCode(OK.value())
                .body("success", is(true))
                .body("message", is("1 reports processed, 1 rejected"));

        final var records = KafkaManager.getRecords(1, "dev.clea.fct.visit-scan");

        assertThat(records)
                .extracting(ConsumerRecord::value)
                .extracting(value -> new ObjectMapper().convertValue(value, Map.class))
                .singleElement()
                .hasFieldOrPropertyWithValue(
                        "encryptedLocationMessage",
                        QrCode.LOCATION_1_LOCATION_SPECIFIC_PART_DECODED_BASE64
                )
                .hasFieldOrPropertyWithValue("isBackward", false)
                .hasFieldOrPropertyWithValue("qrCodeScanTime", nowEpochMs)
                .hasFieldOrPropertyWithValue("type", 0)
                .hasFieldOrPropertyWithValue("version", 0);
    }

    @Test
    @DisplayName("when a qrScan is not numeric reject everything")
    void a_visit_with_a_malformed_qrcode_scantime_is_ignored() {
        givenAuthenticated()
                .contentType(JSON)
                .body(
                        Map.of(
                                "pivotDate", 3,
                                "visits", List.of(
                                        Map.of(
                                                "qrCode", QrCode.LOCATION_1_URL.getRef(),
                                                "qrCodeScanTime", 1
                                        ),
                                        Map.of(
                                                "qrCode", QrCode.LOCATION_2_URL.getRef(),
                                                "qrCodeScanTime", "a"
                                        )
                                )
                        )
                )
                .post("/api/clea/v1/wreport")

                .then()
                .statusCode(BAD_REQUEST.value())
                .body("httpStatus", equalTo(BAD_REQUEST.value()))
                .body("timestamp", isStringDateBetweenNowAndTenSecondsAgo())
                .body("message", equalTo("JSON parse error"))
                .body("validationErrors", hasSize(0));
    }

    @Test
    @DisplayName("when no valid visit then reject everything")
    void a_report_with_no_valid_visit_causes_400_bad_request() {
        final var visits = List.of(
                new Visit(" ", 1L)
        );
        final var request = new ReportRequest(visits, 2L);

        givenAuthenticated()
                .contentType(JSON)
                .body(request)
                .post("/api/clea/v1/wreport")

                .then()
                .statusCode(BAD_REQUEST.value())
                .body("httpStatus", equalTo(BAD_REQUEST.value()))
                .body("timestamp", isStringDateBetweenNowAndTenSecondsAgo())
                .body("message", equalTo("Invalid request"))
                .body("validationErrors[0].object", equalTo("Visit"))
                .body("validationErrors[0].field", equalTo("qrCode"))
                .body("validationErrors[0].rejectedValue", equalTo(" "))
                .body("validationErrors[0].message", containsString("must not be blank"))
                .body("validationErrors", hasSize(1));
    }
}
