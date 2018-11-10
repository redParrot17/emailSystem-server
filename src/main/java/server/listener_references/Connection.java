package server.listener_references;

import server.TcpServer;

import java.io.PrintWriter;
import java.net.Socket;
import java.security.PublicKey;

/**
 * Connection class that contains information about a socket connection to a client
 */
public class Connection {

    private transient final Socket socket;
    private transient final PublicKey key;
    private transient final TcpServer server;
    private transient final PrintWriter outgoing;
    private final long connectionCreation;

    /**
     *
     * @param server    the {@link TcpServer} currently accepting client connections
     * @param socket    the actual {@link Socket} that the client is connected to
     * @param key       the {@link PublicKey} used for encrypting messages to be sent to the client
     * @param outgoing  the {@link PrintWriter} used for sending messages to the client
     */
    public Connection(TcpServer server, Socket socket, PublicKey key, PrintWriter outgoing) {
        connectionCreation = System.currentTimeMillis();
        this.outgoing = outgoing;
        this.server = server;
        this.socket = socket;
        this.key = key;
    }

    /**
     * Sends a simple message to the client connected through this connection
     * @param data text to be sent
     */
    public void replyText(String data) {
        server.sendText(outgoing, key, data);
    }

    /**
     * Sends a command to the client connected through this connection
     * @param command   command to be sent
     * @param arguments the command arguments
     */
    public void replyCommand(String command, String arguments) {
        server.sendCommand(outgoing, key, command, arguments);
    }

    /**
     * Sends an email to the client connected through this connection
     * @param email {@link Email} to be sent
     */
    public void replyEmail(Email email) {
        server.sendEmail(outgoing, key, email);
    }

    public Socket getSocket() {
        return socket;
    }

    public PublicKey getKey() {
        return key;
    }

    public long getConnectionCreated() {
        return connectionCreation;
    }

    public enum event {CONNECTED,REMOVED}

}
