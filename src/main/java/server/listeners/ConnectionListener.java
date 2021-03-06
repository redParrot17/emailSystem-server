package server.listeners;

import server.listener_references.Connection;

/**
 * ConnectionListener to be fired whenever a connection is made or destroyed
 */
public interface ConnectionListener extends ServerListener {
    void onConnectionCreated(Connection connection);
    void onConnectionRemoved(Connection connection);
}
