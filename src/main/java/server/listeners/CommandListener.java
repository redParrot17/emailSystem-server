package server.listeners;

import server.listener_references.Command;

public interface CommandListener extends ServerListener {
    void onCommandReceived(Command command);
}
