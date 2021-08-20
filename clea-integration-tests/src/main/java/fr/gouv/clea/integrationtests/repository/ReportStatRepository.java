package fr.gouv.clea.integrationtests.repository;

import fr.gouv.clea.integrationtests.model.ReportStat;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ReportStatRepository extends ElasticsearchRepository<ReportStat, String> {
}
