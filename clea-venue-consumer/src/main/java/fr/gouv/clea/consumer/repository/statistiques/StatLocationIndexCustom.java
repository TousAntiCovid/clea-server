package fr.gouv.clea.consumer.repository.statistiques;

import fr.gouv.clea.consumer.model.LocationStat;

public interface StatLocationIndexCustom {

    void updateByIncrement(LocationStat locationStatToRetrieve, LocationStat locationStatToAdd);
}
