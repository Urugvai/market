package org.morozov.market.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.net.Socket;

/**
 * Created by Morozov on 5/21/2017.
 */
public class ClientThread extends Thread {

    private static final Logger logger = LogManager.getLogger(ClientThread.class);

    private final Socket socket;

    public ClientThread(@NotNull final Socket socket) {
        this.socket = socket;
    }

    public void run() {
        if (isPossibleToOpenSession("login")) {

        } else {
            logger.warn("");
        }

    }

    @NotNull
    private static synchronized boolean isPossibleToOpenSession(@NotNull final String login) {
        if (!Server.getUserSessions().contains(login)) {
            Server.getUserSessions().add(login);
            return true;
        } else {
            return false;
        }
    }
}
