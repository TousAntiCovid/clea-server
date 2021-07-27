package fr.gouv.clea.consumer.repository.statistiques;

import fr.gouv.clea.consumer.model.StatLocation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

@RequiredArgsConstructor
public class StatLocationRepositoryCustomImpl implements StatLocationRepositoryCustom {

    private final ElasticsearchOperations elasticsearchTemplate;

    /**
     * Increment backward OR forward visit of an existing stat_location
     *
     * @param stat statistique with key and value to increment (+1 in backword or +1
     *             in forward)
     */
    @Override
    public void updateByIncrement(StatLocation statLocationToRetrieve, StatLocation statLocationToAdd) {
        StatLocation retrievedStatLocation = elasticsearchTemplate
                .get(statLocationToRetrieve.getId(), StatLocation.class);
        retrievedStatLocation
                .setBackwardVisits(retrievedStatLocation.getBackwardVisits() + statLocationToAdd.getBackwardVisits());
        retrievedStatLocation
                .setForwardVisits(retrievedStatLocation.getForwardVisits() + statLocationToAdd.getForwardVisits());
        elasticsearchTemplate.save(retrievedStatLocation);
    }
}
