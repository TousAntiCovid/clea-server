package fr.gouv.clea.consumer.repository;

import fr.gouv.clea.consumer.model.LocationStat;
import fr.gouv.clea.consumer.repository.statistiques.LocationStatRepository;
import fr.gouv.clea.consumer.test.IntegrationTest;
import org.elasticsearch.ElasticsearchStatusException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.UncategorizedDataAccessException;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTest
class LocationStatRepositoryTest {

    @Autowired
    private LocationStatRepository locationStatRepository;

    @Test
    void cant_create_successively_two_documents_with_same_id() {
        // initial save should work because the document doesnt exist
        final var first = LocationStat.builder()
                .periodStart(Instant.parse("2019-08-13T10:00:00Z"))
                .venueType(4)
                .venueCategory1(1)
                .venueCategory2(2)
                .backwardVisits(1)
                .forwardVisits(1)
                .build();
        locationStatRepository.create(first);

        // a second save attempt to create the document with other values should fail
        // because de document already exist
        final var second = LocationStat.builder()
                .periodStart(Instant.parse("2019-08-13T10:00:00Z"))
                .venueType(4)
                .venueCategory1(1)
                .venueCategory2(2)
                .backwardVisits(1)
                .forwardVisits(1)
                .build();
        assertThatThrownBy(() -> locationStatRepository.create(second))
                .isInstanceOf(UncategorizedDataAccessException.class)
                .hasRootCauseExactlyInstanceOf(ElasticsearchStatusException.class)
                .hasRootCauseMessage(
                        "Elasticsearch exception [" +
                                "type=version_conflict_engine_exception, " +
                                "reason=[2019-08-13T10:00:00Z-vt:4-vc1:1-vc2:2]: version conflict, " +
                                "document already exists (current version [1])]"
                );

    }

    @Test
    void should_throw_OptimisticLockingFailureException_on_concurrent_modification() {
        final var locationStat = LocationStat.builder()
                .periodStart(Instant.parse("2019-08-13T10:00:00Z"))
                .venueType(4)
                .venueCategory1(1)
                .venueCategory2(2)
                .backwardVisits(1)
                .forwardVisits(1)
                .build();
        locationStatRepository.create(locationStat);

        final var firstLocationStatV1 = locationStatRepository.findById(locationStat)
                .orElseThrow();
        final var secondLocationStatV1 = locationStatRepository.findById(locationStat)
                .orElseThrow();

        locationStatRepository.update(firstLocationStatV1.withBackwardVisits(2));

        assertThatThrownBy(() -> locationStatRepository.update(secondLocationStatV1.withBackwardVisits(3)))
                .isExactlyInstanceOf(OptimisticLockingFailureException.class)
                .hasMessageStartingWith("Cannot index a document due to seq_no+primary_term conflict;")
                .hasRootCauseExactlyInstanceOf(ElasticsearchStatusException.class)
                .hasRootCauseMessage(
                        "Elasticsearch exception [" +
                                "type=version_conflict_engine_exception, " +
                                "reason=[2019-08-13T10:00:00Z-vt:4-vc1:1-vc2:2]: version conflict, " +
                                "required seqNo [0], primary term [1]. current document has seqNo [1] and primary term [1]]"
                );
    }
}
