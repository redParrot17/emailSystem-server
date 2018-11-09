package server.listeners;

import server.listener_references.Connection;

public interface ConnectionListener extends ServerListener {
    void onConnectionCreated(Connection connection);
    void onConnectionRemoved(Connection connection);
}
