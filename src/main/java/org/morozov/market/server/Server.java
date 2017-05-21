package org.morozov.market.server;

import com.thoughtworks.xstream.XStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.morozov.market.entity.Item;
import org.morozov.market.util.PersistenceProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private static final Logger logger = LogManager.getLogger(Server.class);

    private static final int LISTENER_PORT = 8080;
    private static final String STOP_SERVER_COMMAND = "stop";
    private static final String PATH_ITEMS_FILE = "src/main/resources/items.xml";

    private static final ExecutorService threadPool = Executors.newCachedThreadPool();
    private final static Set<String> userSessions = new HashSet<>();

    public static void main(String[] args) {
        try {
            ServerSocket socketListener = new ServerSocket(LISTENER_PORT);
            logger.info("Start server...");

            updateItems();
            logger.info("Items was updated...");

            threadPool.submit(new ServerThread(socketListener));

            Scanner sc = new Scanner(System.in);
            String stop = null;

            while (!STOP_SERVER_COMMAND.equals(stop)) {
                stop = sc.next();
            }

            threadPool.shutdownNow();
            logger.info("Stop server...");
        } catch (IOException e) {
            logger.error("I/O exception", e);
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private static void updateItems() {
        try (final ObjectInputStream outputStream = new ObjectInputStream(new FileInputStream(new File(PATH_ITEMS_FILE)))) {
            final XStream xStream = new XStream();
            List<Item> itemList = (List<Item>) xStream.fromXML((String) outputStream.readObject());
            PersistenceProvider.makeInTransaction((entityManager) -> {
                List<Item> forDeletingItems =
                        entityManager.createQuery("select i from market$Item i", Item.class).getResultList();
                itemList.forEach(item -> {
                    if (forDeletingItems.contains(item)) {
                        entityManager.merge(item);
                    } else {
                        entityManager.persist(item);
                    }
                });
                forDeletingItems.removeAll(itemList);
                forDeletingItems.forEach(deletingItem -> {
                    deletingItem.getUsers().forEach(user -> {
                        user.setAccount(user.getAccount().add(deletingItem.getPrice()));
                        entityManager.merge(user);
                    });
                    entityManager.remove(deletingItem);
                });
            });
        } catch (Throwable e) {
            logger.error("Some troubles with items updating, updating is skipped...", e);
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
