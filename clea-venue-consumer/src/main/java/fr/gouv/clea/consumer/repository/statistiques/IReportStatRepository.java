package fr.gouv.clea.consumer.repository.statistiques;

import fr.gouv.clea.consumer.model.ReportStatEntity;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface IReportStatRepository extends ElasticsearchRepository<ReportStatEntity, Long> {
}
