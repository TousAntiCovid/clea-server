package fr.gouv.clea.integrationtests.repository;

import fr.gouv.clea.integrationtests.repository.model.LocationStat;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocationStatRepository extends ElasticsearchRepository<LocationStat, String> {
}
