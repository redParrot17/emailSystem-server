package server.listeners;

import server.listener_references.Message;

/**
 * MessageListener to be fired whenever a simple message is received by the server
 */
public interface MessageListener extends ServerListener {
    void onMessageReceived(Message message);
}
