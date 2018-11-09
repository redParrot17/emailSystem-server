package email_system;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import server.TcpServer;
import server.listener_references.Connection;
import server.listeners.CommandListener;
import server.listeners.ConnectionListener;
import server.listeners.EmailListener;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

public class EmailServer {

    private ConcurrentHashMap<Connection, Account> loggedInAccounts;
    private ConnectionListener connectionListener;
    private CommandListener commandListener;
    private HashSet<Account> allAccounts;
    private EmailListener emailListener;
    private final TcpServer tcpServer;

    private final static String ACCOUNT_FILENAME = "";
    private boolean running;

    /**
     * @param tcpServer the {@link TcpServer} instance that clients will be connecting to
     */
    public EmailServer(TcpServer tcpServer) {
        loggedInAccounts = new ConcurrentHashMap<>();
        allAccounts = new HashSet<>();
        this.tcpServer = tcpServer;
        connectionListener = null;
        commandListener = null;
        emailListener = null;
        running = false;
    }

    /**
     * Loads the data and registers the event listeners with the server
     */
    public void start() {
        if (running) return; running = true;
        try { allAccounts = retrieveAllAccounts(ACCOUNT_FILENAME);
        } catch (IOException ioe) {
            running = false; ioe.printStackTrace(); return;
        }
        if (allAccounts == null) { running = false; return; }
        connectionListener = buildConnectionListener();
        commandListener = buildCommandListener();
        emailListener = buildEmailListener();
        tcpServer.addEmailListener(emailListener);
        tcpServer.addCommandListener(commandListener);
        tcpServer.addConnectionListener(connectionListener);
    }

    /**
     * Shuts everything down and saves all the data to the file
     */
    public void stop() {
        if (!running) return; running = false;
        tcpServer.removeEmailListener(emailListener);
        tcpServer.removeCommandListener(commandListener);
        tcpServer.removeConnectionListener(connectionListener);
        loggedInAccounts.keySet().stream().map(Connection::getSocket).forEach(socket -> {
            try { socket.close();
            } catch (IOException ignore) { }
        });
        loggedInAccounts.clear();
        try { saveAllAccounts(ACCOUNT_FILENAME);
        } catch (IOException ioe) { ioe.printStackTrace(); }
        //TODO: what shall we do if the emails and accounts cannot be saved?
    }

    /**
     * Attempts to retrieve all the {@link Account} stored in {@code fileName}
     *
     * @param fileName name of the file the accounts are stored in
     * @return {@link HashSet} of all accounts
     * @throws IOException If there was a problem retrieving the accounts
     */
    private HashSet<Account> retrieveAllAccounts(String fileName) throws IOException {
        String fileContents = ""; // write all data from the file into this variable
        //TODO: open the file and write all the data from it to the fileContents variable

        Type type = new TypeToken<HashSet<Account>>() {}.getType();
        HashSet<Account> accounts = allAccounts = new Gson().fromJson(fileContents, type);
        //TODO: close the file

        return accounts != null ? accounts : new HashSet<>();
    }

    /**
     * Attempts to save all the accounts to the specified file in a Json format
     *
     * @param fileName name of the file the accounts should be saved to
     * @throws IOException If there was a problem saving the accounts to the specified file
     */
    private void saveAllAccounts(String fileName) throws IOException {
        //TODO: open the specified file
        String json = new Gson().toJson(allAccounts); // the data to be written to the file
        //TODO: write json to the file, save, and close
    }

    /**
     * Constructs the {@link ConnectionListener} to be used for handling
     * when clients connect to and disconnect from the server
     * @return the built {@link ConnectionListener}
     */
    private ConnectionListener buildConnectionListener() {
        return new ConnectionListener() {
            @Override
            public void onConnectionCreated(Connection connection) {
                /* Do nothing */
            }

            @Override
            public void onConnectionRemoved(Connection connection) {
                //TODO: Remove the connection and associated account from the HashMap
                // what happens if the connection isn't in the hashmap?
            }
        };
    }

    /**
     * Constructs the {@link CommandListener} to be used for handling
     * when clients send a request to the server
     * @return the built {@link CommandListener}
     */
    private CommandListener buildCommandListener() {
        return command -> {

            Connection connection = command.getConnection();
            String action = command.getCommand();
            String data = command.getArguments();

            //TODO: handle login scenario including adding the connection/account to the HashMap and replying if it was successful
            // reply if the login was successful or invalid
            //TODO: handle creating a new account
            // make sure the account username is unique
            // ignore letter case; lowercase and uppercase shouldn't matter
            //TODO: handle user sending a new email
            //TODO: handle user deleting an existing email
            //TODO: handle user marking an email as read
            //TODO: handle user requesting their emails

            // Don't assume the data is valid. Make sure to have error prevention
            // so that this method never throws an uncaught exception

        };
    }

    /**
     * Constructs the {@link EmailListener} to be used for handling
     * when clients send a new email to the server
     * @return the built {@link EmailListener}
     */
    private EmailListener buildEmailListener() {
        return email -> {

            String[] recipients = email.getRecipients();
            //TODO: put a deep copy " Email newEmail = new Email( oldEmail ); in each of the recipients' accounts"
            // filter out recipients that are duplicates or ones that don't exist
            // don't make it specific to capital or lowercase letters
            // notify accounts that are currently logged in that they have a new email

            // don't let this method throw an uncaught exception

        };
    }

}
