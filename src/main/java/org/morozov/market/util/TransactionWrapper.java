package org.morozov.market.util;

import javax.persistence.EntityManager;

/**
 * Created by Morozov on 5/18/2017.
 */
@FunctionalInterface
public interface TransactionWrapper {
    void run(EntityManager entityManager);
}
