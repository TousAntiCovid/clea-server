package fr.gouv.clea.integrationtests.repository;

import fr.gouv.clea.integrationtests.repository.model.ReportStat;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportStatRepository extends ElasticsearchRepository<ReportStat, String> {
}
