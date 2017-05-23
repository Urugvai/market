package org.morozov.market.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.morozov.market.entity.Item;
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

                String command = reader.readLine();

                while (!Thread.currentThread().isInterrupted() && !checkLoginCommand(command)) {
                    writer.write(DialogHolder.INCORRECT_LOGIN);
                    writer.flush();
                    command = reader.readLine();
                }

                User user = loadOrCreateUser(command);

                if (user == null) {
                    logger.warn("Can't connect user '%s'. Session is closed.", command);
                    return;
                }

                writer.write(DialogHolder.COMMAND_LIST);
                writer.flush();

                boolean logout = false;

                while (!Thread.currentThread().isInterrupted() && !logout) {
                    command = reader.readLine();

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
                            StringBuilder info = new StringBuilder();
                            info.append(user.getLogin()).append(" ").append(user.getAccount()).append("\n\r");
                            info.append("Item list:\n\r");
                            user.getItems().forEach(item -> info.append(item.getName()).append("\n\r"));
                            writer.write(info.toString());
                            writer.flush();
                            break;
                        case CommandHolder.BUY:
                            Item itemForBuying = null;
                            for (Item item : loadItems()) {
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
                            if (user.getAccount().compareTo(itemForBuying.getPrice()) < 0) {
                                writer.write(DialogHolder.NOT_ENOUGH_FUNDS);
                                writer.flush();
                                break;
                            }
                            user.setAccount(user.getAccount().subtract(itemForBuying.getPrice()));
                            user.getItems().add(itemForBuying);
                            PersistenceProvider.makeInTransaction(em -> em.merge(user));
                            writer.write(DialogHolder.SUCCESSFUL_OPERATION);
                            writer.flush();
                            break;
                        case CommandHolder.SELL:
                            Item itemForSelling = null;
                            for (Item item : user.getItems()) {
                                if (item.getName().toUpperCase().equals(imputedCommand[1].toUpperCase())) {
                                    itemForSelling = item;
                                    break;
                                }
                            }
                            if (itemForSelling == null) {
                                writer.write(DialogHolder.ITEM_ARE_NOT_FOUND);
                                writer.flush();
                                break;
                            }
                            user.setAccount(user.getAccount().add(itemForSelling.getPrice()));
                            user.getItems().remove(itemForSelling);
                            PersistenceProvider.makeInTransaction(em -> em.merge(user));
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
        }
    }

    private boolean checkLoginCommand(final @Nullable String command) {
        if (StringUtils.isBlank(command))
            return false;

        String[] imputedCommand = command.split(" ");

        if (imputedCommand.length < 1 && imputedCommand.length > 2) {
            return false;
        }

        if (!CommandHolder.LOGIN.equals(imputedCommand[0].toUpperCase()))
            return false;

        return isPossibleToOpenSession(imputedCommand[1]);
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
    private List<Item> loadItems() {
        List<Item> loadedItems = PersistenceProvider.makeInTransactionAndReturn(
                (entityManager ->
                        entityManager.createQuery("select i from market$Item i", Item.class).getResultList()
                )
        );
        return loadedItems != null ? loadedItems : Collections.emptyList();
    }
}
