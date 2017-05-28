package org.morozov.market

import io.kotlintest.specs.FlatSpec
import org.morozov.market.util.AppContext
import org.morozov.market.util.PersistenceProvider

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
        "ClientThread" should "correct work" {
            //            val executor = Executors.newCachedThreadPool()
//            var clientThread:Thread? = null
//            val runner = Runnable {
//                val socketListener = ServerSocket(Integer.valueOf(AppContext.getProperty("serverPort"))!!)
//                val socket = socketListener.accept()
//                clientThread = ClientThread(socket)
//                clientThread?.priority = Thread.MAX_PRIORITY
//                clientThread?.isDaemon = false
//                executor.submit(clientThread)
//            }
//
//            val thread = Thread(runner)
//            executor.submit(thread)
//
//            val socket = Socket("localhost", Integer.valueOf(AppContext.getProperty("serverPort"))!!)
//            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
//            val writer = PrintWriter(socket.getOutputStream())
//
//            reader.readLine() should start with "Welcome"
//
//            writer.write("login login")
//            writer.flush()
//            var command = reader.readLine()
//            while(command == "") {
//                command =  reader.readLine()
//            }
//            reader.readLine() should start with "Available"
//
//            writer.write("myinfo")
//            writer.flush()
//
//            reader.readLine() should start with "login"
//
//            reader.close()
//            writer.close()
//            socket.close()
//            clientThread?.interrupt()
        }
    }
}