package fr.gouv.clea.consumer.repository.statistiques;

import fr.gouv.clea.consumer.model.LocationStat;

import java.util.Optional;

public interface CustomStatLocationRepository {

    Optional<LocationStat> saveWithIndexTargeting(LocationStat locationStat);

    Optional<LocationStat> findByIdentifier(LocationStat locationStat);
}
