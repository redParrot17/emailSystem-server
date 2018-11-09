package server.listener_references;

import server.TcpServer;

import java.io.PrintWriter;
import java.net.Socket;
import java.security.PublicKey;

public class Connection {

    private transient final Socket socket;
    private transient final PublicKey key;
    private transient final TcpServer server;
    private transient final PrintWriter outgoing;
    private final long connectionCreation;

    public Connection(TcpServer server, Socket socket, PublicKey key, PrintWriter outgoing) {
        connectionCreation = System.currentTimeMillis();
        this.outgoing = outgoing;
        this.server = server;
        this.socket = socket;
        this.key = key;
    }

    public void replyText(String data) {
        server.sendText(outgoing, key, data);
    }

    public void replyCommand(String command, String arguments) {
        server.sendCommand(outgoing, key, command, arguments);
    }

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
