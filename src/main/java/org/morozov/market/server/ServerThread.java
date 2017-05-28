package org.morozov.market.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.morozov.market.util.AppContext;

import java.io.IOException;
import java.io.InterruptedIOException;
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
        try (final ServerSocket serverSocket = this.serverSocket) {
            serverSocket.setSoTimeout(Integer.valueOf(AppContext.getProperty("serverTimeout")));
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Socket client = serverSocket.accept();
                    Server.getThreadPool().submit(new ClientThread(client));
                } catch (InterruptedIOException ignored) {
                }
            }
        } catch (IOException e) {
            logger.error("Some troubles with server thread", e);
        }
    }
}
