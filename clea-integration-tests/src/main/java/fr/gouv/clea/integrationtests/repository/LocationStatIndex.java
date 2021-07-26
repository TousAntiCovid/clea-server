package fr.gouv.clea.integrationtests.repository;

import fr.gouv.clea.integrationtests.model.LocationStat;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.time.Instant;
import java.util.List;

public interface LocationStatIndex extends ElasticsearchRepository<LocationStat, String> {

    List<LocationStat> findByVenueTypeAndVenueCategory1AndVenueCategory2AndBackwardVisitsAndForwardVisitsAndPeriod(
            final int venueType,
            final int venueCategory1,
            final int venueCategory2,
            final int backwardVisits,
            final int forwardVisits,
            final Instant period);
}
