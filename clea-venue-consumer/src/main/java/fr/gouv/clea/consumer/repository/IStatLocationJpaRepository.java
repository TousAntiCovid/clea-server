package fr.gouv.clea.consumer.repository;

import fr.gouv.clea.consumer.model.StatLocation;
import fr.gouv.clea.consumer.model.StatLocationKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IStatLocationJpaRepository
        extends JpaRepository<StatLocation, StatLocationKey>, StatLocationJpaRepositoryCustom {

}
