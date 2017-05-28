package org.morozov.market.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

/**
 * Created by Morozov on 5/18/2017.
 */
public class PersistenceProvider {
    private static EntityManagerFactory factory;

    public static void init(String unitName) {
        factory = Persistence.createEntityManagerFactory(unitName);
    }

    @NotNull
    public static EntityManager getEntityManager() {
        return factory.createEntityManager();
    }

    public static void makeInTransaction(@NotNull TransactionWrapper wrapper) {
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

    @Nullable
    public static <T> T makeInTransactionAndReturn(@NotNull CallableTransactionWrapper<T> wrapper) {
        T entity;
        EntityManager em = factory.createEntityManager();
        try {
            EntityTransaction transaction = em.getTransaction();
            transaction.begin();
            entity = wrapper.call(em);
            transaction.commit();
        } finally {
            em.close();
        }
        return entity;
    }
}
