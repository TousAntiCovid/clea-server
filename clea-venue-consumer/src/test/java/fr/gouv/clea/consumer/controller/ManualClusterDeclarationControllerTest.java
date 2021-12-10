package fr.gouv.clea.consumer.controller;

import fr.gouv.clea.consumer.repository.visits.ExposedVisitRepository;
import fr.gouv.clea.consumer.service.VisitExpositionAggregatorService;
import fr.gouv.clea.consumer.test.IntegrationTest;
import fr.gouv.clea.consumer.test.ReferenceData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static fr.gouv.clea.consumer.test.ReferenceData.*;
import static fr.inria.clea.lsp.utils.TimeUtils.NB_SECONDS_PER_HOUR;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.URLENC;
import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.FOUND;
import static org.springframework.http.HttpStatus.OK;

@IntegrationTest
public class ManualClusterDeclarationControllerTest {

    @Autowired
    private ExposedVisitRepository repository;

    @Autowired
    private VisitExpositionAggregatorService service;

    private long periodStart;

    @BeforeEach
    public void setup() {
        periodStart = (long) LOCATION_1.getLocationSpecificPart().getCompressedPeriodStartTime()
                * NB_SECONDS_PER_HOUR;
    }

    @Test
    void create_cluster_manually_with_no_context() {

        given()
                .urlEncodingEnabled(false)

                .when()
                .contentType(URLENC)
                .params(
                        Map.of(
                                "deeplink", LOCATION_1_URL.toString(),
                                "date", LocalDateTime.now().plus(1, ChronoUnit.HOURS).format(ISO_DATE_TIME)
                        )
                )
                .post("/cluster-declaration")
                .then()
                .statusCode(FOUND.value());

        final var exposedVisitList = repository.findAll();

        assertThat(exposedVisitList).isNotEmpty();
        assertThat(exposedVisitList)
                .allMatch(
                        exposedVisit -> exposedVisit.getLocationTemporaryPublicId()
                                .equals(LOCATION_1_LOCATION_TEMPORARY_SPECIFIC_ID),
                        "has uuid" + LOCATION_1_LOCATION_TEMPORARY_SPECIFIC_ID
                )
                .allMatch(exposedVisit -> exposedVisit.getPeriodStart() == periodStart)
                .allMatch(exposedVisit -> exposedVisit.getForwardVisits() == 100, "has 100 forward visits");
    }

    @Test
    void create_cluster_manually_with_existing_context() {

        final var visit = ReferenceData.defaultVisit()
                .qrCodeScanTime(
                        LocalDateTime.parse(
                                LocalDateTime.now().plus(1, ChronoUnit.HOURS).format(ISO_DATE_TIME), ISO_DATE_TIME
                        ).atZone(ZoneId.of("UTC")).toInstant()
                )
                .isBackward(false)
                .build();
        service.updateExposureCount(visit, false);

        given()
                .urlEncodingEnabled(false)

                .when()
                .contentType(URLENC)
                .params(
                        Map.of(
                                "deeplink", LOCATION_1_URL.toString(),
                                "date", LocalDateTime.now().plus(1, ChronoUnit.HOURS).format(ISO_DATE_TIME)
                        )
                )
                .post("/cluster-declaration")

                .then()
                .statusCode(FOUND.value());

        final var exposedVisitList = repository.findAll();

        assertThat(exposedVisitList).isNotEmpty();
        assertThat(exposedVisitList)
                .allMatch(
                        exposedVisit -> exposedVisit.getLocationTemporaryPublicId()
                                .equals(LOCATION_1_LOCATION_TEMPORARY_SPECIFIC_ID),
                        "has uuid" + LOCATION_1_LOCATION_TEMPORARY_SPECIFIC_ID
                )
                .allMatch(exposedVisit -> exposedVisit.getPeriodStart() == periodStart)
                .allMatch(exposedVisit -> exposedVisit.getForwardVisits() == 101, "has 101 forward visits");
    }

    @Test
    void create_cluster_manually_with_wrong_deeplink_and_correct_date_then_no_visit_save_in_database() {

        given()
                .urlEncodingEnabled(false)

                .when()
                .contentType(URLENC)
                .params(
                        Map.of(
                                "deeplink", "test",
                                "date", LocalDateTime.now().plus(1, ChronoUnit.HOURS).format(ISO_DATE_TIME)
                        )
                )
                .post("/cluster-declaration")

                .then()
                .statusCode(OK.value());

        assertThat(repository.findAll()).isEmpty();

    }

    @Test
    void create_cluster_manually_with_correct_deeplink_and_wrong_date_then_no_visit_save_in_database() {

        given()
                .urlEncodingEnabled(false)

                .when()
                .contentType(URLENC)
                .params(
                        Map.of(
                                "deeplink", LOCATION_1_URL.toString(),
                                "date", ""
                        )
                )
                .post("/cluster-declaration")

                .then()
                .statusCode(OK.value());

        assertThat(repository.findAll()).isEmpty();

    }

}
