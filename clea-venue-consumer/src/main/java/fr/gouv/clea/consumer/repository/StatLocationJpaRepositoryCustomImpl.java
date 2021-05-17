package fr.gouv.clea.consumer.repository;

import fr.gouv.clea.consumer.model.StatLocation;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Repository
public class StatLocationJpaRepositoryCustomImpl  implements StatLocationJpaRepositoryCustom{

    @PersistenceContext
    private EntityManager em;

    /**
     * @param stat object to Insert in database
     * @throws DataIntegrityViolationException if stat line already exists
     */
    @Override
    @Transactional
    public void insert(StatLocation stat) {
        em.persist(stat);
        em.flush();
    }

    /**
     * Increment backward OR forward visit of an existing stat_location
     * @param stat statistique with key and value to increment (+1 in backword or +1 in forward)
     */
    @Override
    @Transactional
    public void updateByIncrement(StatLocation stat) {

        em.createQuery("UPDATE StatLocation AS l "+
                "SET l.backwardVisits=l.backwardVisits + :backVisits, l.forwardVisits=l.forwardVisits + :forwardVisits "+
                "WHERE l.statLocationKey= :key")
                .setParameter("backVisits", stat.getBackwardVisits())
                .setParameter("forwardVisits", stat.getForwardVisits())
                .setParameter("key", stat.getStatLocationKey())
                .executeUpdate();
    }
}
