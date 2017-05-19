package org.morozov.market

import org.apache.logging.log4j.LogManager
import org.morozov.market.entity.Item
import org.morozov.market.entity.User
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test
import java.math.BigDecimal
import java.util.*
import javax.persistence.EntityManager
import javax.persistence.Persistence
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Created by Morozov on 5/18/2017.
 */

class PersistenceProviderTest {

    private val logger = LogManager.getLogger(PersistenceProviderTest::class)

    private var em: EntityManager? = null

    @BeforeClass
    fun init() {
        em = Persistence.createEntityManagerFactory("test").createEntityManager()
    }

    @AfterClass
    fun clear() {
        em?.transaction?.begin()
        em?.createNativeQuery("DELETE FROM market_user_item")?.executeUpdate()
        em?.createNativeQuery("DELETE FROM market_item")?.executeUpdate()
        em?.createNativeQuery("DELETE FROM market_user")?.executeUpdate()
        em?.transaction?.commit()
        em!!.close()
    }

    @Test
    fun crudEntityTest() {
        logger.info("Persist new entities")
        em?.transaction?.begin()
        val item = Item()
        item.name = "test"
        item.price = BigDecimal(10)
        em?.persist(item)
        val item2 = Item()
        item2.name = "test2"
        item2.price = BigDecimal.valueOf(20)
        em?.persist(item2)
        val items = listOf(item, item2)
        val user = User()
        user.login = "testUser"
        user.loginDate = Date(System.currentTimeMillis())
        user.account = BigDecimal(44)
        user.items = items
        em?.persist(user)
        em?.transaction?.commit()

        logger.info("Reload new entities")
        val reloadedItems = em?.createQuery("select i from market\$Item i", Item::class.java)?.resultList

        assertNotNull(reloadedItems)
        assertEquals(2, reloadedItems?.size)

        val reloadedUser = em?.createQuery("select u from market\$User u", User::class.java)?.resultList
        assertNotNull(reloadedUser)
        assertEquals(1, reloadedUser?.size)
        assertEquals("testUser", reloadedUser?.get(0)?.login)

        logger.info("Remove item entity")
        em?.transaction?.begin()
        em?.remove(item)
        em?.flush()
        em?.transaction?.commit()

        val surviveItems = em?.createQuery("select i from market\$Item i", Item::class.java)?.resultList
        assertEquals(1, surviveItems?.size)
        assertTrue { item2.id == surviveItems?.get(0)?.id }
    }
}