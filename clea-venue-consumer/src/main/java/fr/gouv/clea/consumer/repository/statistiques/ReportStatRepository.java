package fr.gouv.clea.consumer.repository.statistiques;

import fr.gouv.clea.consumer.model.ReportStatEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.stereotype.Repository;

import java.util.UUID;

import static org.springframework.data.elasticsearch.core.query.IndexQuery.OpType.CREATE;

@Repository
@RequiredArgsConstructor
public class ReportStatRepository {

    private final ElasticsearchOperations operations;

    public String create(ReportStatEntity reportStat) {
        final var indexCoordinates = IndexCoordinates.of(reportStat.buildIndexName());
        final var create = new IndexQueryBuilder()
                .withOpType(CREATE)
                .withId(UUID.randomUUID().toString())
                .withObject(reportStat)
                .build();
        return operations.index(create, indexCoordinates);
    }
}
