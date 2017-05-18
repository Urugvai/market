package org.morozov.market.util;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;

/**
 * Created by Morozov on 5/18/2017.
 */
public class PersistenceProvider {

    private static final String PERSISTENCE_UNIT_NAME = "market";
    private static EntityManagerFactory factory = javax.persistence.Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);

    public static EntityManager getEntityManager() {
        return factory.createEntityManager();
    }

    public static void makeInTransaction(TransactionWrapper wrapper) {
        EntityManager em = factory.createEntityManager();
        try {
            EntityTransaction transaction = em.getTransaction();
            transaction.begin();
            wrapper.run();
            transaction.commit();
        } finally {
            em.close();
        }
    }
}
