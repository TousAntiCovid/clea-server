package fr.gouv.clea.consumer.repository.statistiques;

import fr.gouv.clea.consumer.model.LocationStat;
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

        IndexCoordinates indexCoordinates = getIndexCoordinates(locationStat);

        log.debug("Saving {} to {}", locationStat, indexCoordinates);

        LocationStat saved = operations.save(locationStat, indexCoordinates);
        operations.indexOps(indexCoordinates).refresh();

        return Optional.of(saved);
    }

    @Override
    public Optional<LocationStat> findByIdentifier(LocationStat locationStat) {
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.matchQuery("id", locationStat.getId()))
                .build();
        SearchHits<LocationStat> LocationStats = operations
                .search(
                        searchQuery, LocationStat.class,
                        IndexCoordinates.of(getIndexCoordinates(locationStat).getIndexName())
                );
        if (LocationStats.iterator().hasNext()) {
            return Optional.of(LocationStats.iterator().next().getContent());
        } else {
            return Optional.empty();
        }

    }

    private <S extends LocationStat> IndexCoordinates getIndexCoordinates(S locationStat) {
        String indexName = "health-clealocations-" + LocalDate.ofInstant(
                locationStat.getPeriodStart(),
                ZoneOffset.UTC
        )
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
