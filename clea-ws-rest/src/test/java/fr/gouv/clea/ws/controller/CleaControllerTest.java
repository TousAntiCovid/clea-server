package fr.gouv.clea.ws.controller;

import fr.gouv.clea.ws.api.model.ReportRequest;
import fr.gouv.clea.ws.api.model.Visit;
import fr.gouv.clea.ws.test.IntegrationTest;
import fr.gouv.clea.ws.test.QrCode;
import fr.inria.clea.lsp.utils.TimeUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static fr.gouv.clea.ws.test.KafkaManager.*;
import static fr.gouv.clea.ws.test.RestAssuredManager.givenAuthenticated;
import static fr.gouv.clea.ws.test.TemporalMatchers.isStringDateBetweenNowAndTenSecondsAgo;
import static io.restassured.http.ContentType.JSON;
import static io.restassured.http.ContentType.TEXT;
import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpStatus.*;

@IntegrationTest
class CleaControllerTest {

    @Test
    void infected_user_can_report_himself_as_infected() {
        final var now = Instant.now();
        final var nowEpochMs = now.getEpochSecond() * 1000;
        final var nowNtpTimestamp = TimeUtils.ntpTimestampFromInstant(now);
        final var request = ReportRequest.builder()
                .pivotDate(0L)
                .visits(
                        List.of(
                                new Visit(QrCode.LOCATION_1_URL.getRef(), nowNtpTimestamp)
                        )
                )
                .build();

        givenAuthenticated()
                .contentType(JSON)
                .body(request)
                .post("/api/clea/v1/wreport")

                .then()
                .statusCode(OK.value())
                .body("success", is(true))
                .body("message", is("1/1 accepted visits"));

        assertThatNextRecordInTopic("dev.clea.fct.visit-scans")
                .hasNoKey()
                .hasNoHeader("__TypeId__")
                .hasJsonValue("encryptedLocationMessage", QrCode.LOCATION_1_LOCATION_SPECIFIC_PART_DECODED_BASE64)
                .hasJsonValue("isBackward", false)
                .hasJsonValue("qrCodeScanTime", nowEpochMs)
                .hasJsonValue("type", 0)
                .hasJsonValue("version", 0);
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
    void a_report_with_malformed_body_causes_400_bad_request() {
        givenAuthenticated()
                .contentType(JSON)
                .body("{ \"id\": 1 }")
                .post("/api/clea/v1/wreport")

                .then()
                .statusCode(BAD_REQUEST.value());
    }

    @Test
    void a_null_pivot_date_causes_400_bad_request() {
        final var request = ReportRequest.builder()
                .pivotDate(null)
                .visits(
                        List.of(
                                new Visit(RandomStringUtils.randomAlphanumeric(20), RandomUtils.nextLong())
                        )
                )
                .build();
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
                .body("validationErrors[0].field", equalTo("pivotDate"))
                .body("validationErrors[0].rejectedValue", equalTo(null))
                .body("validationErrors[0].message", containsString("null"))
                .body("validationErrors", hasSize(1));
    }

    @Test
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
    void a_report_with_a_null_visits_list_causes_400_bad_request() {
        final var request = ReportRequest.builder()
                .pivotDate(0L)
                .visits(null)
                .build();
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
                .body("validationErrors[0].rejectedValue", nullValue())
                .body("validationErrors[0].message", equalTo("must not be empty"))
                .body("validationErrors", hasSize(1));
    }

    @Test
    void a_report_with_an_empty_visits_list_causes_400_bad_request() {
        final var request = ReportRequest.builder()
                .pivotDate(0L)
                .visits(List.of())
                .build();
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
    void a_visit_with_a_null_qrcode_is_ignored() {
        final var now = Instant.now();
        final var nowEpochMs = now.getEpochSecond() * 1000;
        final var nowNtpTimestamp = TimeUtils.ntpTimestampFromInstant(now);
        final var request = ReportRequest.builder()
                .pivotDate(3L)
                .visits(
                        List.of(
                                new Visit(QrCode.LOCATION_1_URL.getRef(), nowNtpTimestamp),
                                new Visit(null, nowNtpTimestamp)
                        )
                )
                .build();
        givenAuthenticated()
                .contentType(JSON)
                .body(request)
                .post("/api/clea/v1/wreport")

                .then()
                .statusCode(OK.value())
                .body("success", is(true))
                .body("message", is("1/2 accepted visits"));

        assertThatNextRecordInTopic("dev.clea.fct.visit-scans")
                .hasNoKey()
                .hasNoHeader("__TypeId__")
                .hasJsonValue("encryptedLocationMessage", QrCode.LOCATION_1_LOCATION_SPECIFIC_PART_DECODED_BASE64)
                .hasJsonValue("isBackward", false)
                .hasJsonValue("qrCodeScanTime", nowEpochMs)
                .hasJsonValue("type", 0)
                .hasJsonValue("version", 0);
    }

    @Test
    void a_visit_with_an_empty_qrcode_is_ignored() {
        final var nowAsNtpTimestamp = TimeUtils.currentNtpTime();
        final var nowEpochMs = TimeUtils.instantFromTimestamp(nowAsNtpTimestamp).getEpochSecond() * 1000;
        final var request = ReportRequest.builder()
                .pivotDate(3L)
                .visits(
                        List.of(
                                new Visit(QrCode.LOCATION_1_URL.getRef(), nowAsNtpTimestamp),
                                new Visit("", 2L)
                        )
                )
                .build();

        givenAuthenticated()
                .contentType(JSON)
                .body(request)
                .post("/api/clea/v1/wreport")

                .then()
                .statusCode(OK.value())
                .body("success", is(true))
                .body("message", is("1/2 accepted visits"));

        assertThatNextRecordInTopic("dev.clea.fct.visit-scans")
                .hasJsonValue("encryptedLocationMessage", QrCode.LOCATION_1_LOCATION_SPECIFIC_PART_DECODED_BASE64)
                .hasJsonValue("isBackward", false)
                .hasJsonValue("qrCodeScanTime", nowEpochMs)
                .hasJsonValue("type", 0)
                .hasJsonValue("version", 0);
    }

    @Test
    void a_visit_with_a_blank_qrcode_is_ignored() {
        final var nowAsNtpTimestamp = TimeUtils.currentNtpTime();
        final var nowEpochMs = TimeUtils.instantFromTimestamp(nowAsNtpTimestamp).getEpochSecond() * 1000;
        final var request = ReportRequest.builder()
                .pivotDate(3L)
                .visits(
                        List.of(
                                new Visit(QrCode.LOCATION_1_URL.getRef(), nowAsNtpTimestamp),
                                new Visit("      ", 2L)
                        )
                )
                .build();

        givenAuthenticated()
                .contentType(JSON)
                .body(request)
                .post("/api/clea/v1/wreport")

                .then()
                .statusCode(OK.value())
                .body("success", is(true))
                .body("message", is("1/2 accepted visits"));

        assertThatNextRecordInTopic("dev.clea.fct.visit-scans")
                .hasJsonValue("encryptedLocationMessage", QrCode.LOCATION_1_LOCATION_SPECIFIC_PART_DECODED_BASE64)
                .hasJsonValue("isBackward", false)
                .hasJsonValue("qrCodeScanTime", nowEpochMs)
                .hasJsonValue("type", 0)
                .hasJsonValue("version", 0);
    }

    @Test
    void a_visit_with_a_null_qrcode_scantime_is_ignored() {
        final var nowAsNtpTimestamp = TimeUtils.currentNtpTime();
        final var nowEpochMs = TimeUtils.instantFromTimestamp(nowAsNtpTimestamp).getEpochSecond() * 1000;
        final var request = ReportRequest.builder()
                .pivotDate(3L)
                .visits(
                        List.of(
                                new Visit(QrCode.LOCATION_1_URL.getRef(), nowAsNtpTimestamp),
                                new Visit(QrCode.LOCATION_2_URL.getRef(), null)
                        )
                )
                .build();

        givenAuthenticated()
                .contentType(JSON)
                .body(request)
                .post("/api/clea/v1/wreport")

                .then()
                .statusCode(OK.value())
                .body("success", is(true))
                .body("message", is("1/2 accepted visits"));

        assertThatNextRecordInTopic("dev.clea.fct.visit-scans")
                .hasJsonValue("encryptedLocationMessage", QrCode.LOCATION_1_LOCATION_SPECIFIC_PART_DECODED_BASE64)
                .hasJsonValue("isBackward", false)
                .hasJsonValue("qrCodeScanTime", nowEpochMs)
                .hasJsonValue("type", 0)
                .hasJsonValue("version", 0);
    }

    @Test
    void a_visit_with_a_negative_qrcode_scantime_is_ignored() {
        final var nowAsNtpTimestamp = TimeUtils.currentNtpTime();
        final var nowEpochMs = TimeUtils.instantFromTimestamp(nowAsNtpTimestamp).getEpochSecond() * 1000;
        final var request = ReportRequest.builder()
                .pivotDate(3L)
                .visits(
                        List.of(
                                new Visit(QrCode.LOCATION_1_URL.getRef(), nowAsNtpTimestamp),
                                new Visit(QrCode.LOCATION_2_URL.getRef(), -1L)
                        )
                )
                .build();

        givenAuthenticated()
                .contentType(JSON)
                .body(request)
                .post("/api/clea/v1/wreport")

                .then()
                .statusCode(OK.value())
                .body("success", is(true))
                .body("message", is("1/2 accepted visits"));

        assertThatNextRecordInTopic("dev.clea.fct.visit-scans")
                .hasJsonValue("encryptedLocationMessage", QrCode.LOCATION_1_LOCATION_SPECIFIC_PART_DECODED_BASE64)
                .hasJsonValue("isBackward", false)
                .hasJsonValue("qrCodeScanTime", nowEpochMs)
                .hasJsonValue("type", 0)
                .hasJsonValue("version", 0);
    }

    @Test
    void a_visit_with_a_malformed_qrcode_scantime_causes_400_bad_request() {
        final var nowAsNtpTimestamp = TimeUtils.currentNtpTime();
        final var nowEpochMs = TimeUtils.instantFromTimestamp(nowAsNtpTimestamp).getEpochSecond() * 1000;
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
    void a_report_with_no_valid_visit_returns_200_and_visits_are_ignored() {
        final var request = ReportRequest.builder()
                .pivotDate(2L)
                .visits(
                        List.of(
                                new Visit(" ", 1L)
                        )
                )
                .build();

        givenAuthenticated()
                .contentType(JSON)
                .body(request)
                .post("/api/clea/v1/wreport")

                .then()
                .statusCode(OK.value())
                .body("message", equalTo("0/1 accepted visits"));
    }
}
