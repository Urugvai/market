package org.morozov.market

import io.kotlintest.specs.FlatSpec
import org.morozov.market.entity.ItemType
import org.morozov.market.entity.User
import org.morozov.market.server.worker.ServerWorker
import org.morozov.market.util.AppContext
import org.morozov.market.util.PersistenceProvider
import java.math.BigDecimal
import kotlin.test.assertNotNull

/**
 * Created by Morozov on 5/27/2017.
 */
class ServerWorkerTest : FlatSpec() {

    private val PATH_ITEM_TYPES_FILE = "src/test/resources/item_types.xml"

    override fun beforeAll() {
        super.beforeAll()
        AppContext.init("src/test/resources/config.properties")
        PersistenceProvider.init("test")
    }


    override fun afterEach() {
        super.afterEach()
        PersistenceProvider.makeInTransaction { entityManager ->
            entityManager.createNativeQuery("DELETE FROM market_item").executeUpdate()
            entityManager.createNativeQuery("DELETE FROM market_item_type").executeUpdate()
            entityManager.createNativeQuery("DELETE FROM market_user").executeUpdate()
        }
    }

    init {
        "ServerWorker" should "update item types" {
            PersistenceProvider.makeInTransaction { em ->
                var itemType = ItemType()
                itemType.name = "Test3"
                itemType.price = BigDecimal(10)
                em.persist(itemType)
                itemType = ItemType()
                itemType.name = "Test4"
                itemType.price = BigDecimal(10)
                em.persist(itemType)
            }
            var types = PersistenceProvider.makeInTransactionAndReturn { em ->
                em.createQuery("select i from market\$ItemType i", ItemType::class.java).resultList
            }
            assertNotNull(types)
            types?.size shouldBe 2

            ServerWorker.updateItemTypes(PATH_ITEM_TYPES_FILE)

            types = PersistenceProvider.makeInTransactionAndReturn { em ->
                em.createQuery("select i from market\$ItemType i", ItemType::class.java).resultList
            }
            assertNotNull(types)
            types?.size shouldBe 4

            forSome(types!!) { it.name shouldBe "Test3"; it.removedFromSelling shouldBe true }
            forSome(types) { it.name shouldBe "Test4"; it.removedFromSelling shouldBe true }
        }
        "ServerWorker" should "loadOrCreateUser" {
            PersistenceProvider.makeInTransaction { em ->
                val user = User()
                user.login = "test"
                user.account = BigDecimal(44)
                em?.persist(user)
            }

            var user = ServerWorker.loadOrCreateUser("test")
            assertNotNull(user)

            user = ServerWorker.loadOrCreateUser("test2")
            assertNotNull(user)
            user?.account shouldEqual BigDecimal.valueOf(100)
        }
        "ServerWorker" should "loadItems" {
            PersistenceProvider.makeInTransaction { em ->
                val itemType = ItemType()
                itemType.name = "test"
                itemType.price = BigDecimal(10)
                itemType.removedFromSelling = false
                em?.persist(itemType)
                val itemType2 = ItemType()
                itemType2.name = "test2"
                itemType2.price = BigDecimal.valueOf(20)
                itemType2.removedFromSelling = true
                em?.persist(itemType2)
            }

            val items = ServerWorker.loadItems()
            items.size shouldBe 1
            forAll(items) { it.name shouldBe "test" }
        }
        "ServerWorker" should "loadUser" {
            var user: User? = null
            PersistenceProvider.makeInTransaction { em ->
                user = User()
                user?.login = "test"
                user?.account = BigDecimal(44)
                em?.persist(user)
            }

            val loadedUser = ServerWorker.loadUser(user!!.id)
            assertNotNull(loadedUser)
            loadedUser?.login shouldBe "test"
        }
        "ServerWorker" should "loadUserInTransaction" {
            var user: User? = null
            PersistenceProvider.makeInTransaction { em ->
                user = User()
                user?.login = "test"
                user?.account = BigDecimal(44)
                em?.persist(user)
            }

            val loadedUser: User? = PersistenceProvider.makeInTransactionAndReturn { em ->
                ServerWorker.loadUserInTransaction(user!!.id, null, em)
            }
            assertNotNull(loadedUser)
            loadedUser?.login shouldBe "test"
        }
    }
}