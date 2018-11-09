package server.listeners;

import server.listener_references.Message;

public interface MessageListener extends ServerListener {
    void onMessageReceived(Message message);
}
