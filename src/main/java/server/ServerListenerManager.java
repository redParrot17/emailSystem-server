package server;

import server.listener_references.Email;
import server.listener_references.Command;
import server.listener_references.Connection;
import server.listener_references.Message;
import server.listeners.CommandListener;
import server.listeners.ConnectionListener;
import server.listeners.EmailListener;
import server.listeners.MessageListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerListenerManager {

    private ExecutorService executor;
    private volatile List<MessageListener> messageListeners;
    private volatile List<ConnectionListener> connectionListeners;
    private volatile List<CommandListener> commandListeners;
    private volatile List<EmailListener> emailListeners;

    public ServerListenerManager() {
        executor = Executors.newCachedThreadPool();
        connectionListeners = new ArrayList<>();
        messageListeners = new ArrayList<>();
        commandListeners = new ArrayList<>();
        emailListeners = new ArrayList<>();
    }

    public void addMessageListener(MessageListener listener) {
        Objects.requireNonNull(listener);
        messageListeners.add(listener);
    }

    public void removeMessageListener(MessageListener listener) {
        Objects.requireNonNull(listener);
        messageListeners.remove(listener);
    }

    public void addCommandListener(CommandListener listener) {
        Objects.requireNonNull(listener);
        commandListeners.add(listener);
    }

    public void removeCommandListener(CommandListener listener) {
        Objects.requireNonNull(listener);
        commandListeners.remove(listener);
    }

    public void addConnectionListener(ConnectionListener listener) {
        Objects.requireNonNull(listener);
        connectionListeners.add(listener);
    }

    public void removeConnectionListener(ConnectionListener listener) {
        Objects.requireNonNull(listener);
        connectionListeners.remove(listener);
    }

    public void addEmailListener(EmailListener listener) {
        Objects.requireNonNull(listener);
        emailListeners.add(listener);
    }

    public void removeEmailListener(EmailListener listener) {
        Objects.requireNonNull(listener);
        emailListeners.remove(listener);
    }

    public void removeAllListeners() {
        connectionListeners.clear();
        commandListeners.clear();
        messageListeners.clear();
        emailListeners.clear();
    }

    public synchronized void raiseMessageEvent(Message message) {
        messageListeners.forEach(listener -> executor.submit((Callable<Void>) () -> {
            listener.onMessageReceived(message);
            return null;
        }));
    }

    public synchronized void raiseConnectionEvent(Connection connection, Connection.event event) {
        connectionListeners.forEach(listener -> executor.submit((Callable<Void>) () -> {
            if (event == Connection.event.CONNECTED) listener.onConnectionCreated(connection);
            if (event == Connection.event.REMOVED) listener.onConnectionRemoved(connection);
            return null;
        }));
    }

    public synchronized void raiseCommandEvent(Command command) {
        commandListeners.forEach(listener -> executor.submit((Callable<Void>) () -> {
            listener.onCommandReceived(command);
            return null;
        }));
    }

    public synchronized void raiseEmailEvent(Email email) {
        emailListeners.forEach(listener -> executor.submit((Callable<Void>) () -> {
            listener.onEmailReceived(email);
            return null;
        }));
    }

}
