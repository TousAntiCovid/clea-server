package fr.gouv.clea.integrationtests.repository;

import fr.gouv.clea.integrationtests.model.LocationStat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
public class CustomStatLocationIndexImpl implements CustomStatLocationIndex {

    private final ElasticsearchOperations operations;

    @Nullable
    private Document mapping;

    @Override
    public Optional<LocationStat> saveWithIndexTargeting(LocationStat locationStat) {

        IndexCoordinates indexCoordinates = getIndexCoordinates(locationStat.getPeriodStart());

        log.debug("Saving {} to {}", locationStat, indexCoordinates);

        LocationStat saved = operations.save(locationStat, indexCoordinates);
        operations.indexOps(indexCoordinates).refresh();

        return Optional.of(saved);
    }

    @Override
    public Optional<LocationStat> findByIdentifier(String id, Instant periodStart) {

        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.matchQuery("id", id))
                .build();

        SearchHits<LocationStat> locationStats = operations
                .search(
                        searchQuery, LocationStat.class,
                        IndexCoordinates.of(getIndexCoordinates(periodStart).getIndexName())
                );
        if (!locationStats.getSearchHits().isEmpty()) {
            return Optional.of(locationStats.getSearchHits().stream().findFirst().get().getContent());
        } else {
            return Optional.empty();
        }

    }

    private <S extends LocationStat> IndexCoordinates getIndexCoordinates(Instant periodStart) {
        String indexName = "health-clealocations-" + LocalDate.ofInstant(periodStart, ZoneOffset.UTC)
                .toString()
                .replace('-', '.');

        IndexCoordinates indexCoordinates = IndexCoordinates.of(indexName);
        IndexOperations indexOps = operations.indexOps(indexCoordinates);
        if (!indexOps.exists()) {
            indexOps.create();
            if (mapping == null) {
                mapping = indexOps.createMapping(LocationStat.class);
            }
            indexOps.putMapping(mapping);
        }
        return indexCoordinates;
    }
}
