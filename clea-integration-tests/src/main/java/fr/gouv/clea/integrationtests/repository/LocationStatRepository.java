package fr.gouv.clea.integrationtests.repository;

import fr.gouv.clea.integrationtests.repository.model.LocationStat;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface LocationStatRepository extends ElasticsearchRepository<LocationStat, String> {
}
