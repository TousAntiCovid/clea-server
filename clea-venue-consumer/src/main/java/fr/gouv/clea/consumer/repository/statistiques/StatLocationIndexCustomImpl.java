package fr.gouv.clea.consumer.repository.statistiques;

import fr.gouv.clea.consumer.model.LocationStat;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

@RequiredArgsConstructor
public class StatLocationIndexCustomImpl implements StatLocationIndexCustom {

    private final ElasticsearchOperations elasticsearchTemplate;

    /**
     * Increment backward OR forward visit of an existing stat_location
     *
     * @param stat statistique with key and value to increment (+1 in backword or +1
     *             in forward)
     */
    @Override
    public void updateByIncrement(LocationStat locationStatToRetrieve, LocationStat locationStatToAdd) {
        LocationStat retrievedLocationStat = elasticsearchTemplate
                .get(locationStatToRetrieve.getId(), LocationStat.class);
        retrievedLocationStat
                .setBackwardVisits(retrievedLocationStat.getBackwardVisits() + locationStatToAdd.getBackwardVisits());
        retrievedLocationStat
                .setForwardVisits(retrievedLocationStat.getForwardVisits() + locationStatToAdd.getForwardVisits());
        elasticsearchTemplate.save(retrievedLocationStat);
    }
}
