package org.morozov.market.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by Morozov on 5/21/2017.
 */
public class ServerThread extends Thread {

    private static final Logger logger = LogManager.getLogger(ServerThread.class);

    private final ServerSocket serverSocket;

    public ServerThread(@NotNull final ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Socket client = serverSocket.accept();
                Server.getThreadPool().submit(new ClientThread(client));
            }
        } catch (IOException e) {
            logger.error("Some troubles with server thread", e);
        }
    }
}
