package fr.gouv.clea.integrationtests.repository;

import fr.gouv.clea.integrationtests.model.LocationStat;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface LocationStatIndex
        extends ElasticsearchRepository<LocationStat, String>, CustomStatLocationIndex {

}
