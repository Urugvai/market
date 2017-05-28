package org.morozov.market.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.morozov.market.entity.Item;
import org.morozov.market.entity.ItemType;
import org.morozov.market.entity.User;
import org.morozov.market.server.worker.ServerWorker;
import org.morozov.market.util.CommandHolder;
import org.morozov.market.util.DialogHolder;
import org.morozov.market.util.PersistenceProvider;
import org.morozov.market.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

/**
 * Created by Morozov on 5/21/2017.
 */
public class ClientThread extends Thread {

    private static final Logger logger = LogManager.getLogger(ClientThread.class);

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
                print(writer, DialogHolder.WELCOME_SPEECH);

                String login = "";
                do {
                    String command = reader.readLine();

                    String loginCommand = command.substring(command.indexOf("l"), command.length());
                    String[] imputedCommand = loginCommand.split(" ");

                    if (imputedCommand.length != 2
                            || !CommandHolder.LOGIN.equals(imputedCommand[0].toUpperCase())) {
                        print(writer, DialogHolder.UNKNOWN_COMMAND);
                        continue;
                    }

                    login = imputedCommand[1];

                    if (!validLogin(login)) {
                        print(writer, DialogHolder.INCORRECT_LOGIN);
                    } else {
                        break;
                    }
                } while (!Thread.currentThread().isInterrupted());

                User user = ServerWorker.loadOrCreateUser(login);

                if (user == null) {
                    logger.warn("Can't connect user '%s'. Session is closed.", login);
                    return;
                }

                lastLogin = user.getLogin();
                print(writer, DialogHolder.COMMAND_LIST);

                boolean logout = false;

                while (!Thread.currentThread().isInterrupted() && !logout) {
                    String command = reader.readLine();

                    String[] imputedCommand = command.split(" ");

                    if (imputedCommand.length < 1 && imputedCommand.length > 2) {
                        print(writer, DialogHolder.UNKNOWN_COMMAND);
                        continue;
                    }

                    switch (imputedCommand[0].toUpperCase()) {
                        case CommandHolder.LOGOUT:
                            Server.getUserSessions().remove(user.getLogin());
                            logout = true;
                            break;
                        case CommandHolder.VIEWSHOP:
                            StringBuilder list = new StringBuilder();
                            ServerWorker.loadItems().forEach(item ->
                                    list.append(item.getName()).append(" ").append(item.getPrice()).append("\n\r")
                            );
                            print(writer, list.toString());
                            break;
                        case CommandHolder.MYINFO:
                            User loadedUser = ServerWorker.loadUser(user.getId());
                            if (loadedUser == null) {
                                print(writer, DialogHolder.USER_WAS_DELETED);
                                break;
                            }
                            final StringBuilder info = new StringBuilder();
                            info.append(loadedUser.getLogin()).append(" ").append(loadedUser.getAccount()).append("\n\r");
                            if (!loadedUser.getItems().isEmpty()) {
                                info.append("Item list:\n\r");
                                for (Item userItem : loadedUser.getItems()) {
                                    info.append(userItem.getItemType().getName()).append("\n\r");
                                }
                            }
                            print(writer, info.toString());
                            break;
                        case CommandHolder.BUY:
                            PersistenceProvider.makeInTransaction(em -> {
                                ItemType itemTypeForBuying = null;
                                List<ItemType> typedItems = em.createQuery(
                                        "select i from market$ItemType i where i.removedFromSelling = false",
                                        ItemType.class
                                ).getResultList();
                                for (ItemType itemType : typedItems) {
                                    if (itemType.getName().toUpperCase().equals(imputedCommand[1].toUpperCase())) {
                                        itemTypeForBuying = itemType;
                                        break;
                                    }
                                }
                                if (itemTypeForBuying == null) {
                                    print(writer, DialogHolder.ITEM_ARE_NOT_FOUND);
                                    return;
                                }


                                User loadedForBuyingUser = ServerWorker.loadUserInTransaction(user.getId(), writer, em);
                                if (loadedForBuyingUser == null) {
                                    return;
                                }

                                if (loadedForBuyingUser.getAccount().compareTo(itemTypeForBuying.getPrice()) < 0) {
                                    print(writer, DialogHolder.NOT_ENOUGH_FUNDS);
                                    return;
                                }
                                loadedForBuyingUser.setAccount(
                                        loadedForBuyingUser.getAccount().subtract(itemTypeForBuying.getPrice())
                                );
                                Item boughtItem = new Item();
                                boughtItem.setUser(loadedForBuyingUser);
                                boughtItem.setItemType(itemTypeForBuying);
                                loadedForBuyingUser.getItems().add(boughtItem);
                                print(writer, DialogHolder.SUCCESSFUL_OPERATION);
                            });
                            break;
                        case CommandHolder.SELL:
                            PersistenceProvider.makeInTransaction(em -> {
                                User reloadedUser = ServerWorker.loadUserInTransaction(user.getId(), writer, em);
                                if (reloadedUser == null) {
                                    return;
                                }
                                Item itemForSelling = null;
                                for (Item item : reloadedUser.getItems()) {
                                    if (item.getItemType().getName().toUpperCase().equals(imputedCommand[1].toUpperCase())) {
                                        itemForSelling = item;
                                        break;
                                    }
                                }
                                if (itemForSelling == null) {
                                    print(writer, DialogHolder.ITEM_ARE_NOT_FOUND);
                                    return;
                                }
                                reloadedUser.setAccount(
                                        reloadedUser.getAccount().add(itemForSelling.getItemType().getPrice())
                                );
                                reloadedUser.getItems().remove(itemForSelling);
                                em.remove(itemForSelling);
                                print(writer, DialogHolder.SUCCESSFUL_OPERATION);
                            });
                            break;
                        default:
                            print(writer, DialogHolder.UNKNOWN_COMMAND);
                            print(writer, DialogHolder.COMMAND_LIST);
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

    private boolean validLogin(final @Nullable String login) {
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

    private void print(@NotNull final PrintWriter writer, @NotNull final String message) {
        writer.write(message);
        writer.flush();
    }
}
