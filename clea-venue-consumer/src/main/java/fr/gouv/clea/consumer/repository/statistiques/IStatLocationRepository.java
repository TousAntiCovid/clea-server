package fr.gouv.clea.consumer.repository.statistiques;

import fr.gouv.clea.consumer.model.StatLocation;
import fr.gouv.clea.consumer.model.StatLocationKey;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface IStatLocationRepository
        extends ElasticsearchRepository<StatLocation, StatLocationKey>, StatLocationRepositoryCustom {

    Optional<StatLocation> findByVenueTypeAndVenueCategory1AndVenueCategory2AndPeriod(int venueType, int venueCategory1,
            int venueCategory2, Instant period);

}
