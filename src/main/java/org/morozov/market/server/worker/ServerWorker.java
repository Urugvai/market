package org.morozov.market.server.worker;

import com.thoughtworks.xstream.XStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.morozov.market.entity.ItemType;
import org.morozov.market.entity.User;
import org.morozov.market.entity.not_persistence.ItemHolder;
import org.morozov.market.util.AppContext;
import org.morozov.market.util.DialogHolder;
import org.morozov.market.util.PersistenceProvider;

import javax.persistence.EntityManager;
import java.io.*;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Created by Morozov on 5/27/2017.
 */
public class ServerWorker {

    private static final Logger logger = LogManager.getLogger(ServerWorker.class);

    @SuppressWarnings("unchecked")
    public static void updateItemTypes(@NotNull String filePath) {
        try (final BufferedReader inputStream = new BufferedReader(new FileReader(new File(filePath)))) {
            StringBuilder str = new StringBuilder();
            String line = inputStream.readLine();
            while (line != null) {
                str.append(line);
                line = inputStream.readLine();
            }
            final XStream xStream = new XStream();
            xStream.alias("item", ItemType.class);
            xStream.alias("items", ItemHolder.class);
            xStream.addImplicitCollection(ItemHolder.class, "itemList");
            List<ItemType> itemList = ((ItemHolder) xStream.fromXML(str.toString())).itemList;
            PersistenceProvider.makeInTransaction((entityManager) -> {
                List<ItemType> forDeletingItems =
                        entityManager.createQuery("select i from market$ItemType i", ItemType.class).getResultList();
                itemList.forEach(item -> {
                    if (item.getRemovedFromSelling() == null) {
                        item.setRemovedFromSelling(false);
                    }
                    if (forDeletingItems.contains(item)) {
                        entityManager.merge(item);
                    } else {
                        entityManager.persist(item);
                    }
                });
                forDeletingItems.removeAll(itemList);
                for (ItemType itemType : forDeletingItems) {
                    itemType.setRemovedFromSelling(true);
                    entityManager.merge(itemType);
                }
            });
            logger.info("Items was updated...");
        } catch (Throwable e) {
            logger.error("Some troubles with items updating, updating is skipped...", e);
        }
    }

    @Nullable
    public static User loadOrCreateUser(@NotNull final String login) {
        return PersistenceProvider.makeInTransactionAndReturn((em) -> {
            List<User> users =
                    em.createQuery("select u from market$User u where u.login = :login", User.class)
                            .setParameter("login", login)
                            .getResultList();
            if (users.isEmpty()) {
                User user = new User();
                user.setLogin(login);
                user.setAccount(
                        BigDecimal.valueOf(Integer.valueOf(AppContext.getProperty("userInitAccount")))
                );
                em.persist(user);
                return user;
            }
            return users.get(0);
        });
    }

    @NotNull
    public static List<ItemType> loadItems() {
        List<ItemType> loadedItems = PersistenceProvider.makeInTransactionAndReturn(
                (entityManager ->
                        entityManager.createQuery(
                                "select i from market$ItemType i where i.removedFromSelling = false",
                                ItemType.class
                        ).getResultList()
                )
        );
        return loadedItems != null ? loadedItems : Collections.emptyList();
    }

    @Nullable
    public static User loadUser(@NotNull final UUID userId) {
        List<User> users = PersistenceProvider.makeInTransactionAndReturn(
                (entityManager ->
                        entityManager.createQuery(
                                "select u from market$User u join fetch u.items i where u.id = :userId", User.class)
                                .setParameter("userId", userId.toString())
                                .setMaxResults(1)
                                .getResultList()
                ));
        return users == null || users.isEmpty() ? null : users.get(0);
    }

    @Nullable
    public static User loadUserInTransaction(
            @NotNull final UUID userId, @Nullable final DataOutputStream writer, @NotNull final EntityManager em) {
        List<User> reloadedUsers = em.createQuery(
                "select u from market$User u join fetch u.items i where u.id = :userId", User.class)
                .setParameter("userId", userId.toString())
                .setMaxResults(1)
                .getResultList();
        if (reloadedUsers == null || reloadedUsers.isEmpty()) {
            if (writer != null)
                print(writer, DialogHolder.USER_WAS_DELETED);
            return null;
        }
        return reloadedUsers.get(0);
    }

    public static void print(@NotNull final DataOutputStream writer, @NotNull final String message) {
        try {
            writer.writeUTF(message);
            writer.flush();
        } catch (IOException e) {
            logger.error(String.format("Error while write message '%s'", message), e);
        }
    }
}
