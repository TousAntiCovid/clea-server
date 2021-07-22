package fr.gouv.clea.consumer.repository.statistiques;

import fr.gouv.clea.consumer.model.StatLocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

public class StatLocationRepositoryCustomImpl implements StatLocationRepositoryCustom {

    @Autowired
    protected ElasticsearchOperations elasticsearchTemplate;

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