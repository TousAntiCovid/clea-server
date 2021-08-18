package fr.gouv.clea.integrationtests.repository;

import fr.gouv.clea.integrationtests.model.LocationStat;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.time.Instant;
import java.util.Optional;

public interface LocationStatIndex extends ElasticsearchRepository<LocationStat, String> {

    Optional<LocationStat> findByPeriodStartAndVenueTypeAndVenueCategory1AndVenueCategory2(
            Instant periodStart,
            int venueType,
            int venueCategory1,
            int venueCategory2);
}
