package fr.gouv.clea.consumer.repository.statistiques;

import fr.gouv.clea.consumer.model.StatLocation;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface StatLocationRepository
        extends ElasticsearchRepository<StatLocation, String>, StatLocationRepositoryCustom {

    Optional<StatLocation> findByVenueTypeAndVenueCategory1AndVenueCategory2AndPeriodStart(int venueType,
            int venueCategory1,
            int venueCategory2,
            Instant periodStart);

}
