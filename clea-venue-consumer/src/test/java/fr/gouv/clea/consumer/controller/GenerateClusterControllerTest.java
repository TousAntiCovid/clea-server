package fr.gouv.clea.consumer.controller;

import fr.gouv.clea.consumer.repository.visits.ExposedVisitRepository;
import fr.gouv.clea.consumer.service.VisitExpositionAggregatorService;
import fr.gouv.clea.consumer.test.IntegrationTest;
import fr.gouv.clea.consumer.test.ReferenceData;
import fr.inria.clea.lsp.utils.TimeUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import static fr.gouv.clea.consumer.test.ReferenceData.LOCATION_1_URL;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.URLENC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.FOUND;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@ExtendWith(MockitoExtension.class)
@IntegrationTest
public class GenerateClusterControllerTest {

    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

    @Autowired
    private ExposedVisitRepository repository;

    @Autowired
    private VisitExpositionAggregatorService service;

    private String deeplink;

    private String date;

    private MultiValueMap<String, String> clusterParams;

    private long periodStart;

    @BeforeEach
    public void setup() {
        deeplink = LOCATION_1_URL.toString();
        periodStart = (long) ReferenceData.LOCATION_1.getLocationSpecificPart().getCompressedPeriodStartTime()
                * TimeUtils.NB_SECONDS_PER_HOUR;
        LocalDateTime localDate = LocalDateTime.now().plus(1, ChronoUnit.HOURS);
        date = localDate.format(formatter);
        clusterParams = new LinkedMultiValueMap<>();
    }

    @Test
    void create_cluster_manually_with_no_context() {

        clusterParams.add("deeplink", deeplink);
        clusterParams.add("date", date);

        given()
                .urlEncodingEnabled(false)
                .when()
                .contentType(URLENC)
                .params(clusterParams)
                .post("/cluster-declaration")
                .then()
                .statusCode(FOUND.value());

        final var exposedVisitList = repository.findAll();

        assertThat(exposedVisitList).isNotEmpty();
        assertThat(exposedVisitList)
                .allMatch(
                        exposedVisit -> exposedVisit.getLocationTemporaryPublicId()
                                .equals(ReferenceData.LOCATION_1_LOCATION_TEMPORARY_SPECIFIC_ID),
                        "has uuid" + ReferenceData.LOCATION_1_LOCATION_TEMPORARY_SPECIFIC_ID
                )
                .allMatch(exposedVisit -> exposedVisit.getPeriodStart() == periodStart)
                .allMatch(exposedVisit -> exposedVisit.getForwardVisits() == 100, "has 100 forward visits");
    }

    @Test
    void create_cluster_manually_with_existing_context() {

        final var visit = ReferenceData.defaultVisit()
                .qrCodeScanTime(
                        LocalDateTime.parse(date, DateTimeFormatter.ISO_DATE_TIME).atZone(ZoneId.of("UTC")).toInstant()
                )
                .isBackward(false)
                .build();
        service.updateExposureCount(visit, false);

        clusterParams.add("deeplink", deeplink);
        clusterParams.add("date", date);

        given()
                .urlEncodingEnabled(false)
                .when()
                .contentType(URLENC)
                .params(clusterParams)
                .post("/cluster-declaration")
                .then()
                .statusCode(FOUND.value());

        final var exposedVisitList = repository.findAll();

        assertThat(exposedVisitList).isNotEmpty();
        assertThat(exposedVisitList)
                .allMatch(
                        exposedVisit -> exposedVisit.getLocationTemporaryPublicId()
                                .equals(ReferenceData.LOCATION_1_LOCATION_TEMPORARY_SPECIFIC_ID),
                        "has uuid" + ReferenceData.LOCATION_1_LOCATION_TEMPORARY_SPECIFIC_ID
                )
                .allMatch(exposedVisit -> exposedVisit.getPeriodStart() == periodStart)
                .allMatch(exposedVisit -> exposedVisit.getForwardVisits() == 101, "has 101 forward visits");
    }

    @Test
    void create_cluster_manually_with_wrong_deeplink_and_correct_date_then_no_visit_save_in_database() {

        clusterParams.add("deeplink", "test");
        clusterParams.add("date", date);

        given()
                .urlEncodingEnabled(false)
                .when()
                .contentType(URLENC)
                .params(clusterParams)
                .post("/cluster-declaration")
                .then()
                .statusCode(INTERNAL_SERVER_ERROR.value());

        assertThat(repository.findAll()).isEmpty();

    }

    @Test
    void create_cluster_manually_with_correct_deeplink_and_wrong_date_then_no_visit_save_in_database() {

        clusterParams.add("deeplink", LOCATION_1_URL.toString());
        clusterParams.add("date", "");

        given()
                .urlEncodingEnabled(false)

                .when()
                .contentType(URLENC)
                .params(clusterParams)
                .post("/cluster-declaration")

                .then()
                .statusCode(INTERNAL_SERVER_ERROR.value());

        assertThat(repository.findAll()).isEmpty();

    }

}
