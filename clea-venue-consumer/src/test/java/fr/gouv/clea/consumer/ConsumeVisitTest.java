package fr.gouv.clea.consumer;

import fr.gouv.clea.consumer.model.DecodedVisit;
import fr.gouv.clea.consumer.repository.visits.ExposedVisitRepository;
import fr.gouv.clea.consumer.test.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.annotations.DateFormat;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static fr.gouv.clea.consumer.test.ElasticManager.assertThatAllDocumentsFromElastic;
import static fr.gouv.clea.consumer.test.KafkaManager.whenSendDecodedVisit;
import static fr.gouv.clea.consumer.test.ReferenceData.givenBackwardDecodedVisitAt;
import static java.time.ZoneOffset.UTC;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@IntegrationTest
public class ConsumeVisitTest {

    private static final Instant TODAY_AT_MIDNIGHT = Instant.now().truncatedTo(ChronoUnit.DAYS);

    private static final Instant TODAY_AT_8AM = TODAY_AT_MIDNIGHT.plus(8, ChronoUnit.HOURS);

    @Autowired
    private ExposedVisitRepository repository;

    @Test
    void testConsumeVisit() {

        DecodedVisit decodedVisit = givenBackwardDecodedVisitAt(TODAY_AT_8AM);

        whenSendDecodedVisit(decodedVisit);

        // location statistics are stored in elastic
        await().atMost(10, SECONDS).untilAsserted(
                () -> assertThatAllDocumentsFromElastic().containsExactlyInAnyOrder(
                        Map.of(
                                "id", TODAY_AT_8AM + "-vt:1-vc1:2-vc2:3",
                                "@timestamp", elasticDefaultStringRepresentation(TODAY_AT_8AM),
                                "venueType", 1,
                                "venueCategory1", 2,
                                "venueCategory2", 3,
                                "forwardVisits", 0,
                                "backwardVisits", 1
                        )
                )
        );

        // exposed visit are stored in postgre
        var exposedVisitList = repository.findAll();

        assertThat(exposedVisitList).hasSize(7);
        assertThat(exposedVisitList)
                .allMatch(
                        exposedVisitEntity -> exposedVisitEntity.getTimeSlot() >= 1
                                && exposedVisitEntity.getTimeSlot() <= 7
                )
                .allMatch(exposedVisitEntity -> exposedVisitEntity.getVenueType() == 1)
                .allMatch(exposedVisitEntity -> exposedVisitEntity.getVenueCategory1() == 2)
                .allMatch(exposedVisitEntity -> exposedVisitEntity.getVenueCategory2() == 3)
                .allMatch(exposedVisitEntity -> exposedVisitEntity.getBackwardVisits() == 1)
                .allMatch(exposedVisitEntity -> exposedVisitEntity.getForwardVisits() == 0);

    }

    private Object elasticDefaultStringRepresentation(Instant instant) {
        return instant.atOffset(UTC)
                .format(DateTimeFormatter.ofPattern(DateFormat.date_time.getPattern()));
    }

}
