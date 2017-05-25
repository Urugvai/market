package org.morozov.market

import org.apache.logging.log4j.LogManager
import org.morozov.market.entity.Item
import org.morozov.market.entity.ItemType
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
        em?.createNativeQuery("DELETE FROM market_item")?.executeUpdate()
        em?.createNativeQuery("DELETE FROM market_item_type")?.executeUpdate()
        em?.createNativeQuery("DELETE FROM market_user")?.executeUpdate()
        em?.transaction?.commit()
        em!!.close()
    }

    @Test
    fun crudEntityTest() {
        logger.info("Persist new entities")
        em?.transaction?.begin()
        val itemType = ItemType()
        itemType.name = "test"
        itemType.price = BigDecimal(10)
        em?.persist(itemType)
        val itemType2 = ItemType()
        itemType2.name = "test2"
        itemType2.price = BigDecimal.valueOf(20)
        em?.persist(itemType2)
        val item = Item()
        item.itemType = itemType
        val item2 = Item()
        item2.itemType = itemType2
        val items = ArrayList<Item>()
        items.add(item)
        items.add(item2)
        em?.persist(item2)
        val user = User()
        user.login = "testUser"
        user.account = BigDecimal(44)
        user.items = items
        item.user = user
        item2.user = user
        em?.persist(user)
        em?.transaction?.commit()
        logger.info("Reload new entities")
        val reloadedItemTypes = em?.createQuery("select i from market\$ItemType i", ItemType::class.java)?.resultList

        assertNotNull(reloadedItemTypes)
        assertEquals(2, reloadedItemTypes?.size)

        val reloadedItems = em?.createQuery("select i from market\$Item i", Item::class.java)?.resultList

        assertNotNull(reloadedItems)
        assertEquals(2, reloadedItems?.size)

        val reloadedUsers =
                em?.createQuery("select u from market\$User u", User::class.java)?.resultList
        assertNotNull(reloadedUsers)
        assertEquals(1, reloadedUsers?.size)
        assertEquals("testUser", reloadedUsers?.get(0)?.login)
        assertEquals(2, reloadedUsers?.get(0)?.items?.size)

        logger.info("Remove item entity")
        em?.transaction?.begin()
        em?.remove(item)
        em?.remove(item2)
        em?.remove(itemType)
        em?.transaction?.commit()

        val surviveItems = em?.createQuery("select i from market\$ItemType i", ItemType::class.java)?.resultList
        assertEquals(1, surviveItems?.size)
        assertTrue { itemType2.id == surviveItems?.get(0)?.id }

        em?.transaction?.begin()
        em?.refresh(user)
        em?.transaction?.commit()
        assertNotNull(user)
        assertEquals("testUser", user.login)
        assertEquals(0, user.items?.size)
    }
}