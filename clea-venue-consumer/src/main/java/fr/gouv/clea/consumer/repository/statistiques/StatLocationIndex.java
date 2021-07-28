package fr.gouv.clea.consumer.repository.statistiques;

import fr.gouv.clea.consumer.model.LocationStat;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface StatLocationIndex
        extends ElasticsearchRepository<LocationStat, String>, StatLocationIndexCustom {

    Optional<LocationStat> findByVenueTypeAndVenueCategory1AndVenueCategory2AndPeriodStart(int venueType,
            int venueCategory1,
            int venueCategory2,
            Instant periodStart);

}
