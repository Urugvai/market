package org.morozov.market.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.morozov.market.entity.ItemType;
import org.morozov.market.entity.User;
import org.morozov.market.util.CommandHolder;
import org.morozov.market.util.DialogHolder;
import org.morozov.market.util.PersistenceProvider;
import org.morozov.market.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.Socket;
import java.util.Collections;
import java.util.List;

/**
 * Created by Morozov on 5/21/2017.
 */
public class ClientThread extends Thread {

    private static final Logger logger = LogManager.getLogger(ClientThread.class);

    private static final int INIT_ACCOUNT = 100;

    private final Socket socket;

    private String lastLogin = null;

    public ClientThread(@NotNull final Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try (final Socket socket = this.socket;
             final BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             final PrintWriter writer = new PrintWriter(socket.getOutputStream())) {
            while (!Thread.currentThread().isInterrupted()) {
                writer.write(DialogHolder.WELCOME_SPEECH);
                writer.flush();

                String login = "";
                do {
                    String command = reader.readLine();

                    String[] imputedCommand = command.split(" ");

                    if (imputedCommand.length != 2
                            || !CommandHolder.LOGIN.equals(imputedCommand[0].toUpperCase())) {
                        writer.write(DialogHolder.UNKNOWN_COMMAND);
                        writer.flush();
                        continue;
                    }

                    login = imputedCommand[1];

                    if (!checkLoginCommand(login)) {
                        writer.write(DialogHolder.INCORRECT_LOGIN);
                        writer.flush();
                    } else {
                        break;
                    }
                } while (!Thread.currentThread().isInterrupted());

                User user = loadOrCreateUser(login);

                if (user == null) {
                    logger.warn("Can't connect user '%s'. Session is closed.", login);
                    return;
                }

                lastLogin = user.getLogin();
                writer.write(DialogHolder.COMMAND_LIST);
                writer.flush();

                boolean logout = false;

                while (!Thread.currentThread().isInterrupted() && !logout) {
                    String command = reader.readLine();

                    String[] imputedCommand = command.split(" ");

                    if (imputedCommand.length < 1 && imputedCommand.length > 2) {
                        writer.write(DialogHolder.UNKNOWN_COMMAND);
                        writer.flush();
                        continue;
                    }

                    switch (imputedCommand[0].toUpperCase()) {
                        case CommandHolder.LOGOUT:
                            Server.getUserSessions().remove(user.getLogin());
                            logout = true;
                            break;
                        case CommandHolder.VIEWSHOP:
                            StringBuilder list = new StringBuilder();

                            loadItems().forEach(item ->
                                    list.append(item.getName()).append(" ").append(item.getPrice()).append("\n\r")
                            );

                            writer.write(list.toString());
                            writer.flush();
                            break;
                        case CommandHolder.MYINFO:
                            User loadedUser = loadUser(user.getId());
                            if (loadedUser == null) {
                                writer.write(DialogHolder.USER_WAS_DELETED);
                                writer.flush();
                                break;
                            }
                            final StringBuilder info = new StringBuilder();
                            info.append(loadedUser.getLogin()).append(" ").append(loadedUser.getAccount()).append("\n\r");
                            info.append("ItemType list:\n\r");
//                            for (ItemType userItem : loadedUser.getItems()) {
//                                info.append(userItem.getName()).append("\n\r");
//                            }
                            writer.write(info.toString());
                            writer.flush();
                            break;
                        case CommandHolder.BUY:
                            ItemType itemForBuying = null;
                            for (ItemType item : loadItems()) {
                                if (item.getName().toUpperCase().equals(imputedCommand[1].toUpperCase())) {
                                    itemForBuying = item;
                                    break;
                                }
                            }
                            if (itemForBuying == null) {
                                writer.write(DialogHolder.ITEM_ARE_NOT_FOUND);
                                writer.flush();
                                break;
                            }

                            User loadedForBuyingUser = loadUser(user.getId());
                            if (loadedForBuyingUser == null) {
                                writer.write(DialogHolder.USER_WAS_DELETED);
                                writer.flush();
                                break;
                            }

                            if (loadedForBuyingUser.getAccount().compareTo(itemForBuying.getPrice()) < 0) {
                                writer.write(DialogHolder.NOT_ENOUGH_FUNDS);
                                writer.flush();
                                break;
                            }
                            loadedForBuyingUser.setAccount(loadedForBuyingUser.getAccount().subtract(itemForBuying.getPrice()));
//                            loadedForBuyingUser.getItems().add(itemForBuying);
                            PersistenceProvider.makeInTransaction(em -> em.merge(loadedForBuyingUser));
                            writer.write(DialogHolder.SUCCESSFUL_OPERATION);
                            writer.flush();
                            break;
                        case CommandHolder.SELL:
                            PersistenceProvider.makeInTransaction(em -> {
                                List<User> reloadedUsers = em.createQuery(
                                        "select u from market$User u join fetch u.items i where u.id = :userId", User.class)
                                        .setParameter("userId", user.getId())
                                        .setMaxResults(1)
                                        .getResultList();
                                if (reloadedUsers == null || reloadedUsers.isEmpty()) {
                                    writer.write(DialogHolder.USER_WAS_DELETED);
                                    writer.flush();
                                    return;
                                }
                                User reloadedUser = reloadedUsers.get(0);
                                ItemType itemForSelling = null;
//                                for (ItemType item : reloadedUser.getItems()) {
//                                    if (item.getName().toUpperCase().equals(imputedCommand[1].toUpperCase())) {
//                                        itemForSelling = item;
//                                        break;
//                                    }
//                                }
                                if (itemForSelling == null) {
                                    writer.write(DialogHolder.ITEM_ARE_NOT_FOUND);
                                    writer.flush();
                                    return;
                                }
                                itemForSelling = loadItem(itemForSelling.getId());
                                if (itemForSelling == null) {
                                    writer.write(DialogHolder.ITEM_WAS_DELETED);
                                    writer.flush();
                                    return;
                                }
                                reloadedUser.setAccount(reloadedUser.getAccount().add(itemForSelling.getPrice()));
//                                reloadedUser.getItems().remove(itemForSelling);
//                                itemForSelling.getUsers().remove(reloadedUser);
                                em.merge(itemForSelling);
                                em.merge(reloadedUser);
                            });
                            writer.write(DialogHolder.SUCCESSFUL_OPERATION);
                            writer.flush();
                            break;
                        default:
                            writer.write(DialogHolder.UNKNOWN_COMMAND);
                            writer.flush();
                            break;
                    }
                }
            }
        } catch (IOException e) {
            logger.error("IO exception in client thread, thread is interrupted...", e);
        } finally {
            if (lastLogin != null)
                Server.getUserSessions().remove(lastLogin);
        }
    }

    private boolean checkLoginCommand(final @Nullable String login) {
        return !StringUtils.isBlank(login) && isPossibleToOpenSession(login);
    }

    private static synchronized boolean isPossibleToOpenSession(@NotNull final String login) {
        if (!StringUtils.isBlank(login) && !Server.getUserSessions().contains(login)) {
            Server.getUserSessions().add(login);
            return true;
        } else {
            return false;
        }
    }

    @Nullable
    private User loadOrCreateUser(@NotNull final String login) {
        return PersistenceProvider.makeInTransactionAndReturn((em) -> {
            List<User> users =
                    em.createQuery("select u from market$User u where u.login = :login", User.class)
                            .setParameter("login", login)
                            .getResultList();
            if (users.isEmpty()) {
                User user = new User();
                user.setLogin(login);
                user.setAccount(BigDecimal.valueOf(INIT_ACCOUNT));
                em.persist(user);
                return user;
            }
            return users.get(0);
        });
    }

    @NotNull
    private List<ItemType> loadItems() {
        List<ItemType> loadedItems = PersistenceProvider.makeInTransactionAndReturn(
                (entityManager ->
                        entityManager.createQuery("select i from market$ItemType i", ItemType.class).getResultList()
                )
        );
        return loadedItems != null ? loadedItems : Collections.emptyList();
    }

    @Nullable
    private User loadUser(final @NotNull String userId) {
        List<User> users = PersistenceProvider.makeInTransactionAndReturn(
                (entityManager ->
                        entityManager.createQuery(
                                "select u from market$User u join fetch u.items i where u.id = :userId", User.class)
                                .setParameter("userId", userId)
                                .setMaxResults(1)
                                .getResultList()
                ));
        return users == null || users.isEmpty() ? null : users.get(0);
    }

    @Nullable
    private ItemType loadItem(final @NotNull String itemId) {
        List<ItemType> items = PersistenceProvider.makeInTransactionAndReturn(
                (entityManager ->
                        entityManager.createQuery(
                                "select i from market$ItemType i join fetch i.users u where i.id = :itemId", ItemType.class)
                                .setParameter("itemId", itemId)
                                .setMaxResults(1)
                                .getResultList()
                ));
        return items == null || items.isEmpty() ? null : items.get(0);
    }
}
