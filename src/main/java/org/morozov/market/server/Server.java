package org.morozov.market.server;

import com.thoughtworks.xstream.XStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.morozov.market.entity.Item;
import org.morozov.market.entity.not_persistence.ItemHolder;
import org.morozov.market.util.PersistenceProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Server {

    private static final Logger logger = LogManager.getLogger(Server.class);

    private static final int LISTENER_PORT = 8080;
    private static final String STOP_SERVER_COMMAND = "stop";
    private static final String PATH_ITEMS_FILE = "src/main/resources/items.xml";

    private static final ExecutorService threadPool = Executors.newCachedThreadPool();
    private final static ConcurrentSkipListSet<String> userSessions = new ConcurrentSkipListSet<>();

    public static void main(String[] args) {
        try {
            ServerSocket socketListener = new ServerSocket(LISTENER_PORT);
            logger.info("Start server...");

            updateItems();

            threadPool.submit(new ServerThread(socketListener));

            Scanner sc = new Scanner(System.in);
            String stop = null;

            while (!STOP_SERVER_COMMAND.equals(stop)) {
                stop = sc.next();
            }

            threadPool.shutdownNow();

            threadPool.awaitTermination(2000, TimeUnit.MILLISECONDS);
            logger.info("Stop server...");
        } catch (IOException e) {
            logger.error("I/O exception", e);
            e.printStackTrace();
        } catch (InterruptedException e) {
            logger.error("Thread pool is shut down!", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static void updateItems() {
        try (final BufferedReader inputStream = new BufferedReader(new FileReader(new File(PATH_ITEMS_FILE)))) {
            StringBuilder str = new StringBuilder();
            String line = inputStream.readLine();
            while (line != null) {
                str.append(line);
                line = inputStream.readLine();
            }
            final XStream xStream = new XStream();
            xStream.alias("item", Item.class);
            xStream.alias("items", ItemHolder.class);
            xStream.addImplicitCollection(ItemHolder.class, "itemList");
            List<Item> itemList = ((ItemHolder) xStream.fromXML(str.toString())).itemList;
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
            logger.info("Items was updated...");
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
