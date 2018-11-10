package server.listeners;

import server.listener_references.Command;

/**
 * CommandListener to be fired whenever a command is received by the server
 */
public interface CommandListener extends ServerListener {
    void onCommandReceived(Command command);
}
