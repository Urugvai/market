package org.morozov.market.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.morozov.market.server.worker.ServerWorker;
import org.morozov.market.util.AppContext;
import org.morozov.market.util.PersistenceProvider;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private static final Logger logger = LogManager.getLogger(Server.class);

    private static final ExecutorService threadPool = Executors.newCachedThreadPool();
    private final static ConcurrentSkipListSet<String> userSessions = new ConcurrentSkipListSet<>();

    public static void main(String[] args) {
        AppContext.init("src/main/resources/config.properties");
        logger.info("Application context was initialized ...");
        try {
            PersistenceProvider.init(AppContext.getProperty("persistenceUnitName"));
            logger.info("Persistence was initialized ...");
            ServerSocket socketListener =
                    new ServerSocket(Integer.valueOf(AppContext.getProperty("serverPort")));
            logger.info("Start server...");

            ServerWorker.updateItemTypes(AppContext.getProperty("pathItemTypesFile"));

            threadPool.submit(new ServerThread(socketListener));

            Scanner sc = new Scanner(System.in);
            String stop = null;

            while (!AppContext.getProperty("stopServerCommand").equals(stop)) {
                stop = sc.next();
            }

            threadPool.shutdownNow();

            logger.info("Stop server...");
        } catch (IOException e) {
            logger.error("I/O exception", e);
            e.printStackTrace();
        }
    }


    public static ExecutorService getThreadPool() {
        return threadPool;
    }

    @NotNull
    public static Set<String> getUserSessions() {
        return userSessions;
    }
}
