package fr.gouv.clea.consumer.repository.statistiques;

import fr.gouv.clea.consumer.model.LocationStat;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StatLocationIndex
        extends ElasticsearchRepository<LocationStat, String>, CustomStatLocationIndex<LocationStat> {

}
