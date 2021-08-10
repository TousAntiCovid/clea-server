package fr.gouv.clea.integrationtests.repository;

import fr.gouv.clea.integrationtests.model.LocationStat;

import java.util.Optional;

public interface CustomStatLocationIndex {

    Optional<LocationStat> saveWithIndexTargeting(LocationStat locationStat);
}
