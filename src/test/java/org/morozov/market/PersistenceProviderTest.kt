package org.morozov.market

import org.morozov.market.entity.Item
import org.testng.annotations.Test
import java.math.BigDecimal
import javax.persistence.Persistence
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Created by Morozov on 5/18/2017.
 */

class PersistenceProviderTest {

    @Test
    fun crudEntityTest() {
        val em = Persistence.createEntityManagerFactory("test").createEntityManager()
        em.transaction.begin()
        val item = Item()
        item.name = "test"
        item.price = BigDecimal.valueOf(10)
        em.persist(item)
        em.transaction.commit()

        val reloadedItem = em.createQuery("select i from market\$Item i", item.javaClass).singleResult

        assertNotNull(reloadedItem)
        assertTrue { item.id == reloadedItem.id }

        em.transaction.begin()
        em.remove(item)
        em.flush()
        em.transaction.commit()

        val removedItems = em.createQuery("select i from market\$Item i", item.javaClass).resultList
        assertTrue { removedItems.isEmpty() }
        em.close()
    }
}