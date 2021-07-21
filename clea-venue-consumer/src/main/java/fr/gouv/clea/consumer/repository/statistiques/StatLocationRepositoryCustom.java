package fr.gouv.clea.consumer.repository.statistiques;

import fr.gouv.clea.consumer.model.StatLocation;

public interface StatLocationRepositoryCustom {

    void updateByIncrement(StatLocation statLocationToRetrieve, StatLocation statLocationToAdd);
}
