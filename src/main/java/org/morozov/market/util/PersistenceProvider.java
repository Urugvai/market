package org.morozov.market.util;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

/**
 * Created by Morozov on 5/18/2017.
 */
public class PersistenceProvider {

    private static final String PERSISTENCE_UNIT_NAME = "market";
    private static EntityManagerFactory factory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);

    public static EntityManager getEntityManager() {
        return factory.createEntityManager();
    }

    public static void makeInTransaction(TransactionWrapper wrapper) {
        EntityManager em = factory.createEntityManager();
        try {
            EntityTransaction transaction = em.getTransaction();
            transaction.begin();
            wrapper.run(em);
            transaction.commit();
        } finally {
            em.close();
        }
    }
}
