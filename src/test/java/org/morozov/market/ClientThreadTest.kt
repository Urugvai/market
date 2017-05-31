package org.morozov.market

import io.kotlintest.matchers.have
import io.kotlintest.matchers.start
import io.kotlintest.specs.FlatSpec
import org.morozov.market.entity.ItemType
import org.morozov.market.server.ClientThread
import org.morozov.market.util.AppContext
import org.morozov.market.util.PersistenceProvider
import java.io.DataInputStream
import java.io.DataOutputStream
import java.math.BigDecimal
import java.net.ServerSocket
import java.net.Socket

/**
 *  Not finished yet!
 *
 * Created by Morozov on 5/28/2017.
 */
class ClientThreadTest : FlatSpec() {
    override fun beforeAll() {
        super.beforeAll()
        AppContext.init("src/test/resources/config.properties")
        PersistenceProvider.init("test")
        PersistenceProvider.makeInTransaction { em ->
            var itemType = ItemType()
            itemType.name = "Test3"
            itemType.price = BigDecimal(10)
            itemType.removedFromSelling = false
            em.persist(itemType)
            itemType = ItemType()
            itemType.name = "Test4"
            itemType.price = BigDecimal(10)
            itemType.removedFromSelling = false
            em.persist(itemType)
        }
    }


    override fun afterAll() {
        super.afterAll()
        PersistenceProvider.makeInTransaction { entityManager ->
            entityManager.createNativeQuery("DELETE FROM market_item").executeUpdate()
            entityManager.createNativeQuery("DELETE FROM market_item_type").executeUpdate()
            entityManager.createNativeQuery("DELETE FROM market_user").executeUpdate()
        }
    }

    init {
        "ClientThread" should "do typical scenario" {
            var clientThread: Thread? = null
            val runner = Runnable {
                val socketListener = ServerSocket(Integer.valueOf(AppContext.getProperty("serverPort"))!!)
                val socket = socketListener.accept()
                clientThread = ClientThread(socket)
                clientThread?.start()
            }

            Thread(runner).start()

            val socket = Socket("localhost", Integer.valueOf(AppContext.getProperty("serverPort"))!!)
            val reader = DataInputStream(socket.getInputStream())
            val writer = DataOutputStream(socket.getOutputStream())

            reader.readUTF() should start with "Welcome"

            writer.writeUTF("login login")
            writer.flush()
            reader.readUTF() should start with "Available"

            writer.writeUTF("myinfo")
            writer.flush()
            var userInfo = reader.readUTF()
            userInfo should start with "login"
            userInfo should have substring "100"

            writer.writeUTF("viewshop")
            writer.flush()
            val items = reader.readUTF()
            items should have substring "Test3"
            items should have substring "Test4"

            writer.writeUTF("unknown command")
            writer.flush()
            reader.readUTF() should start with "Unknown command"

            writer.writeUTF("buy command")
            writer.flush()
            reader.readUTF() should start with "Imputed item aren't found in the shop"

            writer.writeUTF("buy Test3")
            writer.flush()
            reader.readUTF() should start with "Successful operation"

            writer.writeUTF("myinfo")
            writer.flush()
            userInfo = reader.readUTF()
            userInfo should start with "login"
            userInfo should have substring "90"

            writer.writeUTF("sell command")
            writer.flush()
            reader.readUTF() should start with "Imputed item aren't found for selling"

            writer.writeUTF("sell Test3")
            writer.flush()
            reader.readUTF() should start with "Successful operation"

            writer.writeUTF("myinfo")
            writer.flush()
            userInfo = reader.readUTF()
            userInfo should start with "login"
            userInfo should have substring "100"

            writer.writeUTF("logout")
            writer.flush()
            reader.readUTF() should start with "Welcome"

            clientThread?.interrupt()

            reader.close()
            writer.close()
            socket.close()
        }
    }
}