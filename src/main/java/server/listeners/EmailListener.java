package server.listeners;

import server.listener_references.Email;

public interface EmailListener extends ServerListener {
    void onEmailReceived(Email email);
}
