package org.morozov.market.util;

import javax.persistence.EntityManager;

/**
 * Created by Morozov on 5/22/2017.
 */
public interface CallableTransactionWrapper<T> {
    T call(EntityManager entityManager);
}
