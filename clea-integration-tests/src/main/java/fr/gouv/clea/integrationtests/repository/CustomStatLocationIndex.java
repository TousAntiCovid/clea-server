package fr.gouv.clea.integrationtests.repository;

import fr.gouv.clea.integrationtests.model.LocationStat;

import java.time.Instant;
import java.util.Optional;

public interface CustomStatLocationIndex {

    Optional<LocationStat> saveWithIndexTargeting(LocationStat locationStat);

    Optional<LocationStat> findByIdentifier(String id, Instant periodStart);
}
