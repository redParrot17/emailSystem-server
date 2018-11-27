package email_system;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import server.TcpServer;
import server.listener_references.Connection;
import server.listeners.CommandListener;
import server.listeners.ConnectionListener;
import server.listeners.EmailListener;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class EmailServer {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().setLenient().disableHtmlEscaping().create();

    private ConcurrentHashMap<Connection, Account> loggedInAccounts;
    private ConnectionListener connectionListener;
    private CommandListener commandListener;
    private HashSet<Account> allAccounts;
    private EmailListener emailListener;
    private final TcpServer tcpServer;

    private final static String ACCOUNT_FILENAME = "accounts.json";
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
        StringBuilder fileContents = new StringBuilder(); // write all data from the file into this variable
        Scanner readFile = new Scanner(new File(fileName));
        while (readFile.hasNextLine()) {
            fileContents.append(readFile.nextLine()).append("\n");
        }
        Type type = new TypeToken<HashSet<Account>>() {}.getType();
        HashSet<Account> accounts = allAccounts = GSON.fromJson(fileContents.toString(), type);
        readFile.close();
        return accounts != null ? accounts : new HashSet<>();
    }

    /**
     * Attempts to save all the accounts to the specified file in a Json format
     *
     * @param fileName name of the file the accounts should be saved to
     * @throws IOException If there was a problem saving the accounts to the specified file
     */
    private void saveAllAccounts(String fileName) throws IOException {
    	PrintWriter outputFile = new PrintWriter(new File(fileName));
        String json = GSON.toJson(allAccounts); // the data to be written to the file
        Scanner inputFile = new Scanner(json);
        inputFile.useDelimiter("");
        while (inputFile.hasNext()) {
        	outputFile.printf(inputFile.next());
        }
        outputFile.flush();
        outputFile.close();
        inputFile.close();
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
            	if (loggedInAccounts.contains(connection)) {
            		loggedInAccounts.remove(connection);
            	}
            	else {
            		return;
            	}
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

            switch (action) {

                case "login":
                    try {
                        String[] credentials = data.split(",",2);
                        credentials[0] = credentials[0].toLowerCase();
                        char[] pass = new char[credentials[1].length()];
                        for (int i=0; i<pass.length; i++) {
                            pass[i] = credentials[1].charAt(i);
                        }
                        Account account = allAccounts.stream().filter(a -> a.getEmail_address().equals(credentials[0])).findFirst().get();
                        if (account.checkPassword(pass)) {

                            //TODO: Call the connection#replyCommand method with "login" as the command and "valid" as the argument
                        	connection.replyCommand("login", "valid");
                            //TODO: .put a new entry into loggedInAccounts with connection and account as the parameters
                        	loggedInAccounts.put(connection, account);
                        } else {
                            connection.replyCommand("login", "invalid");
                        }
                    } catch (Exception e) {

               
                    	//TODO: Call the connection#replyCommand method with "login" as the command and "invalid" as the argument
                    	connection.replyCommand("login", "invalid");
                    }
                    break;

                case "new-account":
                    String[] credentials = data.split(",",2);
                    credentials[0] = credentials[0].toLowerCase();
                    char[] pass = new char[credentials[1].length()];
                    for (int i=0; i<pass.length; i++) {
                        pass[i] = credentials[1].charAt(i);
                    }
                    if (allAccounts.stream().filter(a -> a.getEmail_address().equals(credentials[0])).count() <= 0) {

                        //TODO: Create a new Account with credentials[0] and pass as the parameters
                    	Account newAccount = new Account(credentials[0], pass);
                        //TODO: Add the new account to allAccounts
                    	allAccounts.add(newAccount);
                        //TODO: .put a new entry into loggedInAccounts using connection and the newly created Account as the parameters
                    	loggedInAccounts.putIfAbsent(connection, newAccount);
                        //TODO: Call the connection#replyCommand method with "login" as the command and "valid" as the argument
                    	connection.replyCommand("login", "valid");
                        try { saveAllAccounts(ACCOUNT_FILENAME);
                        } catch (IOException e) { e.printStackTrace(); }
                    } else {

                        //TODO: Call the connection#replyCommand method with "login" as the command and "invalid" as the argument
                    	connection.replyCommand("login", "invalid");
                    }
                    break;

                case "delete-email":

                    //TODO: Retrieve the account from loggedInAccounts
                    //TODO: Retrieve the email from the Account using the UUID stored in the command argument
                    //TODO: Remove the email from the Account

                    try { saveAllAccounts(ACCOUNT_FILENAME);
                    } catch (IOException e) { e.printStackTrace(); }
                    break;

                case "read-email":

                    //TODO: Retrieve the Account from loggedInAccounts
                    //TODO: Retrieve the email from the Account using the UUID stored in the command argument
                    //TODO: If the Email exists, call the Email#setHasOpened method and set it to true

                    try { saveAllAccounts(ACCOUNT_FILENAME);
                    } catch (IOException e) { e.printStackTrace(); }
                    break;

                case "get-emails":
                    Account ac = loggedInAccounts.getOrDefault(connection, null);
                    if (ac != null) connection.replyCommand("email-list", new Gson().toJson(ac.getAllReceivedEmails()));
                    break;
            }
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

            //TODO: Make every recipient all lowercase
            for (String r : recipients) {
            	r.toLowerCase();
            }
            //TODO: Remove any duplicates from recipients
            //Since it is a String array the size is immutable so the duplicate is set to null instead of removed.
            for (int i = 0; i < recipients.length; i++) {
            	for (int j = 0; j < recipients.length; j++) {
            		if (j != i) {
            			if (recipients[j] == recipients[i] && recipients[i] != null) {
            				recipients[j] = null;
            			}
            		}
            	}
            }
            //TODO: For each unique recipient that exists within allAccounts, put a deep copy " Email newEmail = new Email( oldEmail ); " into the recipient's account
            //TODO: If the Account is located in loggedInAccounts, use the getConnectionFromAccount method to obtain the logged-in connection...
            //TODO: ...call the connection#replyEmail with email as the parameter

            // don't let this method throw an uncaught exception

            try { saveAllAccounts(ACCOUNT_FILENAME);
            } catch (IOException e) { e.printStackTrace(); }
        };
    }

    private Optional<Connection> getConnectionFromAccount(Account account) {
        return loggedInAccounts.entrySet().stream()
                .filter(e -> e.getValue().equals(account))
                .findFirst().map(Map.Entry::getKey);
    }

}
