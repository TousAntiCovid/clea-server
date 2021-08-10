package fr.gouv.clea.consumer.repository.statistiques;

import fr.gouv.clea.consumer.model.LocationStat;

import java.util.Optional;

public interface CustomStatLocationIndex {

    Optional<LocationStat> saveWithIndexTargeting(LocationStat locationStat);
}
