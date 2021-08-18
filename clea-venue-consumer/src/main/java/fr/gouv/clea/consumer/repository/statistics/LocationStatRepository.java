package fr.gouv.clea.consumer.repository.statistics;

import fr.gouv.clea.consumer.model.LocationStat;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.NoSuchIndexException;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static org.springframework.data.elasticsearch.core.query.IndexQuery.OpType.CREATE;

@Repository
@RequiredArgsConstructor
public class LocationStatRepository {

    private final ElasticsearchOperations operations;

    public String create(LocationStat locationStat) {
        final var indexCoordinates = IndexCoordinates.of(locationStat.buildIndexName());
        final var create = new IndexQueryBuilder()
                .withOpType(CREATE)
                .withObject(locationStat)
                .build();
        return operations.index(create, indexCoordinates);
    }

    public LocationStat update(LocationStat locationStat) {
        final var indexCoordinates = IndexCoordinates.of(locationStat.buildIndexName());
        return operations.save(locationStat, indexCoordinates);
    }

    public Optional<LocationStat> findById(LocationStat locationStat) {
        final var index = IndexCoordinates.of(locationStat.buildIndexName());
        try {
            return Optional.ofNullable(
                    operations.get(locationStat.buildId(), LocationStat.class, index)
            );
        } catch (NoSuchIndexException e) {
            return Optional.empty();
        }
    }
}
