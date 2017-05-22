package org.morozov.market.util;

import org.morozov.market.entity.BaseEntity;

import javax.persistence.EntityManager;

/**
 * Created by Morozov on 5/22/2017.
 */
public interface CallableTransactionWrapper<T extends BaseEntity> {
    T call(EntityManager entityManager);
}
