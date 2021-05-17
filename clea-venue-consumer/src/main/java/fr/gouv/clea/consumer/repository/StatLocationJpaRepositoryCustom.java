package fr.gouv.clea.consumer.repository;

import fr.gouv.clea.consumer.model.StatLocation;

public interface StatLocationJpaRepositoryCustom {
    void insert(StatLocation stat);
    void updateByIncrement(StatLocation stat);
}
