import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import server.TcpServer;
import server.listener_references.Email;
import server.listeners.CommandListener;
import server.listeners.EmailListener;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;

public class HandleEmails {

    private static final Gson GSON = new Gson();

    private HashMap<String, HashSet<Email>> database = new HashMap<>();

    public HandleEmails(TcpServer server) {
        server.addEmailListener(buildEmailListener());
        server.addCommandListener(buildCommandListener());
    }

    private EmailListener buildEmailListener() {
        return email -> {
            String[] recipients = email.getRecipients();
            for (String user : recipients) {
                database.putIfAbsent(user, new HashSet<>());
                database.get(user).add(new Email(email));
            }
        };
    }

    private CommandListener buildCommandListener() {
        return command -> {
            String cmd = command.getCommand();
            if (cmd.equals("get-emails")) {
                Type type = new TypeToken<HashSet<Email>>() {}.getType();
                HashSet<Email> list = database.getOrDefault(command.getArguments(), new HashSet<>());
                String json = GSON.toJson(list, type);
                command.getConnection().replyCommand("email-list", json);
            } else if (cmd.equals("delete-emails")) {
                database.remove(command.getArguments());
            }
        };
    }

}
