package fr.gouv.clea.integrationtests.repository;

import fr.gouv.clea.integrationtests.model.ReportStat;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface ReportStatIndex extends ElasticsearchRepository<ReportStat, String> {

    List<ReportStat> findByReportedAndRejectedAndCloseAndBackwardsAndForwards(
            final int reported,
            final int rejected,
            final int close,
            final int backwards,
            final int forwards);
}
