package fr.gouv.clea.integrationtests.repository;

import fr.gouv.clea.integrationtests.repository.model.LocationStat;
import org.springframework.context.annotation.Profile;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

@Profile("!int")
public interface LocationStatRepository extends ElasticsearchRepository<LocationStat, String> {
}
