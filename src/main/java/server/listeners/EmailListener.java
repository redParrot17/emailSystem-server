package server.listeners;

import server.listener_references.Email;

/**
 * EmailListener to be fired whenever an email is received by the server
 */
public interface EmailListener extends ServerListener {
    void onEmailReceived(Email email);
}
