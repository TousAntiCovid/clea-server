package fr.gouv.clea.consumer.controller;

import fr.gouv.clea.consumer.repository.visits.ExposedVisitRepository;
import fr.gouv.clea.consumer.service.VisitExpositionAggregatorService;
import fr.gouv.clea.consumer.test.IntegrationTest;
import fr.gouv.clea.consumer.test.ReferenceData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.LocalDateTime;

import static fr.gouv.clea.consumer.test.ReferenceData.*;
import static fr.inria.clea.lsp.utils.TimeUtils.NB_SECONDS_PER_HOUR;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.HTML;
import static io.restassured.http.ContentType.URLENC;
import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.HOURS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
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
    void can_declare_cluster_on_location_without_existing_visits() {

        final var redirectResponse = given()
                .urlEncodingEnabled(false)
                .contentType(URLENC)
                .params(
                        "deeplink", LOCATION_1_URL.toString(),
                        "date", LocalDateTime.now(UTC).minus(1, HOURS).toString(),
                        "timezone", "Europe/Paris"
                )

                .expect()
                .statusCode(FOUND.value())

                .when()
                .post("/cluster-declaration");

        final var headerLocationValue = redirectResponse.then().extract().header("Location");

        given()
                .urlEncodingEnabled(false)

                .when()
                .contentType(HTML)
                .get(headerLocationValue)

                .then()
                .statusCode(OK.value())
                .contentType(HTML)
                .body(
                        "html.body.form.div.h3",
                        equalTo(
                                "Cluster enregistré avec succès, le cluster sera actif la prochaine fois que le batch sera déclenché."
                        )
                );

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
    void can_declare_cluster_on_location_with_existing_visits() {

        final var visit = ReferenceData.defaultVisit()
                .qrCodeScanTime(
                        Instant.now().minus(1, HOURS)
                )
                .isBackward(false)
                .build();
        service.updateExposureCount(visit, false);

        final var redirectResponse = given()
                .urlEncodingEnabled(false)
                .contentType(URLENC)
                .params(
                        "deeplink", LOCATION_1_URL.toString(),
                        "date", LocalDateTime.now(UTC).minus(1, HOURS).toString(),
                        "timezone", "UTC"
                )

                .expect()
                .statusCode(FOUND.value())

                .when()
                .post("/cluster-declaration");

        final var headerLocationValue = redirectResponse.then().extract().header("Location");

        given()
                .urlEncodingEnabled(false)

                .when()
                .contentType(HTML)
                .get(headerLocationValue)

                .then()
                .statusCode(OK.value())
                .contentType(HTML)
                .body(
                        "html.body.form.div.h3",
                        equalTo(
                                "Cluster enregistré avec succès, le cluster sera actif la prochaine fois que le batch sera déclenché."
                        )
                );

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

    @ParameterizedTest
    @CsvSource({
            "test,'Le deeplink doit être une URL'",
            "'','Le deeplink ne doit pas être vide'",
            "' ','Le deeplink ne doit pas être vide'",
            "https://tac.gouv.fr?v=0#AB5Y974Y0v9Puhn2hyo5nH8ajVwNeYLAt8aJGwrviGL944dKIFwDRYxKoBcWdxP11A4Go-, 'Le deeplink est invalide : une erreur est survenue lors du décodage de la partie spécifique au lieu'",
            "https://tac.gouv.fr?v=0#AB5Y974Y0v9Puhn2hyo5nH8ajVwNeYLAt8aJGwrviGL944dKIFwDRYxKoBcWdxP11A4Go--oDXg_eGih65qi3sfquGxYVYCpe4d0K_-fRNKO6-UUBkRK4fp-JoEuYZRu4VTRHZAZhaT2aP64ZFW8VdUsS2v6tLfntEjLyWybJKC6gTIBT2Sf0XeAL7iwsQOVVhS3DYDnIQCv00nUOjh2lkDuT-S9dtZu9by, 'Le deeplink est invalide : une erreur est survenue lors du déchiffrement de la partie spécifique au lieu'",
            "https://bonjour.tousanticovid.gouv.fr/places.html, 'Le deeplink est invalide : URL doit être de la forme \"https://tac.gouv.fr?v=0#AB5Y\"'"
    })
    void cant_declare_cluster_with_malformed_deeplink(String deeplink, String errorMessage) {

        given()
                .urlEncodingEnabled(false)

                .when()
                .contentType(URLENC)
                .params(
                        "deeplink", deeplink,
                        "date", LocalDateTime.now(UTC).minus(1, HOURS).toString()

                )
                .post("/cluster-declaration")

                .then()
                .contentType(HTML)
                .body("html.body.form.div.p", equalTo(errorMessage))
                .statusCode(OK.value());

        assertThat(repository.findAll()).isEmpty();

    }

    @ParameterizedTest
    @CsvSource({
            "'','La date du cluster ne doit pas être vide'",
            "' ','La date du cluster ne doit pas être vide'",
            "10/10/2021,'La date du cluster date doit être de la forme \"2000-10-31T01:30\"'",
            "aaaa,'La date du cluster date doit être de la forme \"2000-10-31T01:30\"'"
    })
    void cant_declare_cluster_with_malformed_date(String date, String message) {

        given()
                .urlEncodingEnabled(false)

                .when()
                .contentType(URLENC)
                .params(
                        "deeplink", LOCATION_1_URL.toString(),
                        "date", date

                )
                .post("/cluster-declaration")

                .then()
                .contentType(HTML)
                .body("html.body.form.div.p", equalTo(message))
                .statusCode(OK.value());

        assertThat(repository.findAll()).isEmpty();

    }

    @Test
    void cant_declare_cluster_with_future_date() {

        given()
                .urlEncodingEnabled(false)

                .when()
                .contentType(URLENC)
                .params(
                        "deeplink", LOCATION_1_URL.toString(),
                        "date", LocalDateTime.now(UTC).plus(1, HOURS).toString()

                )
                .post("/cluster-declaration")

                .then()
                .contentType(HTML)
                .body("html.body.form.div.p", equalTo("La date du cluster date ne peut pas être située dans le futur"))
                .statusCode(OK.value());

        assertThat(repository.findAll()).isEmpty();
    }

}
