package fr.gouv.clea.consumer.repository.statistiques;

import fr.gouv.clea.consumer.model.ReportStatEntity;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportStatIndex extends ElasticsearchRepository<ReportStatEntity, Long> {
}