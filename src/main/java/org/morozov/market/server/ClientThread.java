package org.morozov.market.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

            while (!Thread.currentThread().isInterrupted()) {
                command = reader.readLine();

                String[] imputedCommand = command.split(" ");

                if (imputedCommand.length < 1 && imputedCommand.length > 2) {
                    writer.write(DialogHolder.UNKNOWN_COMMAND);
                    writer.flush();
                    continue;
                }

                switch (imputedCommand[0].toUpperCase()) {
                    case CommandHolder.LOGOUT:
                        break;
                    case CommandHolder.VIEWSHOP:
                        break;
                    case CommandHolder.MYINFO:
                        break;
                    case CommandHolder.BUY:
                        break;
                    case CommandHolder.SELL:
                        break;
                    default:
                        writer.write(DialogHolder.UNKNOWN_COMMAND);
                        writer.flush();
                        break;
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
}
