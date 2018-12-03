package email_system;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import server.TcpServer;
import server.listener_references.Connection;
import server.listener_references.Email;
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
            @Override public void onConnectionCreated(Connection connection) { }
            @Override public void onConnectionRemoved(Connection connection) {
            	if (loggedInAccounts.containsKey(connection)) {
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
                        	connection.replyCommand("login", "valid");
                        	loggedInAccounts.put(connection, account);
                        } else {
                            connection.replyCommand("login", "invalid");
                        }
                    } catch (Exception e) {
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
                    	Account newAccount = new Account(credentials[0], pass);
                    	allAccounts.add(newAccount);
                    	loggedInAccounts.putIfAbsent(connection, newAccount);
                    	connection.replyCommand("login", "valid");
                        try { saveAllAccounts(ACCOUNT_FILENAME);
                        } catch (IOException e) { e.printStackTrace(); }
                    } else {
                    	connection.replyCommand("login", "invalid");
                    }
                    break;

                case "delete-email":
                	Account account_deleted = loggedInAccounts.get(command.getConnection());

                	// retrieve the email from the Account if it exists
                	if (account_deleted.getEmailFromUUID(command.getArguments()).isPresent()) {
                		Email email = account_deleted.getEmailFromUUID(command.getArguments()).get();
                        account_deleted.removeEmail(email);
                	}

                    try { saveAllAccounts(ACCOUNT_FILENAME);
                    } catch (IOException e) { e.printStackTrace(); }
                    break;

                case "read-email":
                	Account account_read = loggedInAccounts.get(command.getConnection());

                    // retrieve the email from the Account if it exists
                	if (account_read.getEmailFromUUID(command.getArguments()).isPresent()) {
                		Email email = account_read.getEmailFromUUID(command.getArguments()).get();
                		email.setHasOpened(true);
                	}

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

            // Make every recipient lowercase
            for (int i = 0; i < recipients.length; i++) {
            	recipients[i] = recipients[i].toLowerCase();
            }

            // Removes all the duplicate recipients
            HashSet<String> UniqueRecipients = new HashSet<String>();
            for (String r : recipients) {
            	UniqueRecipients.add(r);
            }

            allAccounts.stream().filter(account -> UniqueRecipients.contains(account.getEmail_address())).forEach(account -> {
                account.addEmail(new Email(email));
                if (loggedInAccounts.containsValue(account)) {
                    getConnectionFromAccount(account).ifPresent(connection -> {
                        connection.replyEmail(email);
                    });
                }
            });

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
