package server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.codec.binary.Base64;
import server.listener_references.Command;
import server.listener_references.Connection;
import server.listener_references.Email;
import server.listener_references.Message;
import server.listeners.CommandListener;
import server.listeners.ConnectionListener;
import server.listeners.EmailListener;
import server.listeners.MessageListener;
import server.packets.CommandPacket;
import server.packets.EmailPacket;
import server.packets.EncryptionPacket;

import javax.crypto.spec.GCMParameterSpec;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TcpServer implements AutoCloseable, Runnable {

    private static final Gson GSON = new GsonBuilder().setLenient().disableHtmlEscaping().serializeNulls().create();
    private ServerListenerManager listenerManager;
    private ExecutorService executorService;
    private ExecutorService threadPool;
    private ServerSocket serverSocket;
    private KeyPair serverKeys;
    private boolean alive;
    private int backlog;
    private int timeout;
    private int port;

    public TcpServer(int port) {
        listenerManager = new ServerListenerManager();
        executorService = null;
        serverSocket = null;
        threadPool = null;
        serverKeys = null;
        this.port = port;
        this.timeout = 0;
        this.backlog = 0;
        alive = false;
    }

    public TcpServer(int port, int timeout) {
        listenerManager = new ServerListenerManager();
        this.timeout = timeout > 0 ? timeout : 0;
        executorService = null;
        serverSocket = null;
        threadPool = null;
        serverKeys = null;
        this.port = port;
        this.backlog = 0;
        alive = false;
    }

    public TcpServer(int port, int timeout, int backLog) {
        listenerManager = new ServerListenerManager();
        this.timeout = timeout > 0 ? timeout : 0;
        executorService = null;
        this.backlog = backLog;
        serverSocket = null;
        threadPool = null;
        serverKeys = null;
        this.port = port;
        alive = false;
    }

    public TcpServer(int port, int timeout, int backLog, boolean startImmediately) throws ServerException {
        listenerManager = new ServerListenerManager();
        this.timeout = timeout > 0 ? timeout : 0;
        executorService = null;
        this.backlog = backLog;
        serverSocket = null;
        threadPool = null;
        serverKeys = null;
        this.port = port;
        alive = false;

        if (startImmediately) start().join();
    }

    public CompletableFuture<Void> start() throws ServerException {
        if (alive) throw new ServerException("Server is already running");
        try { serverKeys = EcoCryptography.generateKeys();
        } catch (NoSuchAlgorithmException e) {
            throw new ServerException("Unable to generate async encryption keys: " + e.getMessage());
        }
        if (serverKeys == null) throw new ServerException("Failed to generate async encryption keys");
        try {
            serverSocket = new ServerSocket(port, backlog);
        } catch (IOException ioe) {
            throw new ServerException("Failed to create server: " + ioe.getMessage());
        }
        alive = true;
        if (backlog > 0) threadPool = Executors.newFixedThreadPool(backlog);
        else threadPool = Executors.newCachedThreadPool();
        executorService = Executors.newSingleThreadExecutor();
        executorService.submit(this);
        return CompletableFuture.completedFuture(null);
    }

    @SuppressWarnings("unused")
    public void addMessageListener(MessageListener listener) {
        listenerManager.addMessageListener(listener);
    }
    @SuppressWarnings("unused")
    public void removeMessageListener(MessageListener listener) {
        listenerManager.removeMessageListener(listener);
    }
    @SuppressWarnings("unused")
    public void addCommandListener(CommandListener listener) {
        listenerManager.addCommandListener(listener);
    }
    @SuppressWarnings("unused")
    public void removeCommandListener(CommandListener listener) {
        listenerManager.removeCommandListener(listener);
    }
    @SuppressWarnings("unused")
    public void addConnectionListener(ConnectionListener listener) {
        listenerManager.addConnectionListener(listener);
    }
    @SuppressWarnings("unused")
    public void removeConnectionListener(ConnectionListener listener) {
        listenerManager.removeConnectionListener(listener);
    }
    @SuppressWarnings("unused")
    public void addEmailListener(EmailListener listener) {
        listenerManager.addEmailListener(listener);
    }
    @SuppressWarnings("unused")
    public void removeEmailListener(EmailListener listener) {
        listenerManager.removeEmailListener(listener);
    }
    @SuppressWarnings("unused")
    public void removeAllListeners() {
        listenerManager.removeAllListeners();
    }

    @Override
    public void run() {
        try {
            while (alive) {
                ClientConnection connection = new ClientConnection(this, serverSocket.accept(), timeout);
                try { threadPool.execute(connection);
                } catch (Exception e) {
                    connection.close();
                    if (!e.getMessage().equals("Socket is closed"))
                        e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        close();
    }

    private EncryptionPacket generateEncryptionPacket(String message, EncryptionPacket.PacketType packetType, PublicKey key) {
        byte iv[] = new byte[SecuredGCMUsage.IV_SIZE];
        SecureRandom secRandom = new SecureRandom();
        secRandom.nextBytes(iv);
        GCMParameterSpec gcmParamSpec = new GCMParameterSpec(SecuredGCMUsage.TAG_BIT_LENGTH, iv);
        String[] encryptedText = EcoCryptography.encrypt(message, key, gcmParamSpec, "eco.echotrace.77".getBytes());
        if (encryptedText == null || encryptedText.length != 2) return null;
        return new EncryptionPacket(encryptedText[1], packetType, gcmParamSpec, encryptedText[0]);
    }

    private String decryptEncryptionPacket(EncryptionPacket packet) throws Exception {
        return EcoCryptography.decrypt(packet, serverKeys.getPrivate(), "eco.echotrace.77".getBytes());
    }

    private String decryptEncryptionPacket(String json) throws Exception {
        EncryptionPacket packet = GSON.fromJson(json, EncryptionPacket.class);
        return EcoCryptography.decrypt(packet, serverKeys.getPrivate(), "eco.echotrace.77".getBytes());
    }

    public void sendText(PrintWriter outgoing, PublicKey key, String data) {
        if (key == null || data == null || outgoing == null || !alive) return;
        EncryptionPacket packet = generateEncryptionPacket(data, EncryptionPacket.PacketType.TEXT, key);
        String json = GSON.toJson(packet);
        if (packet != null) outgoing.println(Base64.encodeBase64String(json.getBytes()));
    }

    public void sendCommand(PrintWriter outgoing, PublicKey key, String command, String arguments) {
        Objects.requireNonNull(outgoing);
        Objects.requireNonNull(key);
        Objects.requireNonNull(command);
        Objects.requireNonNull(arguments);
        String data = GSON.toJson(new CommandPacket(command, arguments));
        EncryptionPacket packet = generateEncryptionPacket(data, EncryptionPacket.PacketType.COMMAND, key);
        String json = GSON.toJson(packet);
        if (packet != null) outgoing.println(Base64.encodeBase64String(json.getBytes()));
    }

    public void sendEmail(PrintWriter outgoing, PublicKey key, Email email) {
        Objects.requireNonNull(outgoing);
        Objects.requireNonNull(key);
        Objects.requireNonNull(email);
        String data = GSON.toJson(new EmailPacket(email));
        EncryptionPacket packet = generateEncryptionPacket(data, EncryptionPacket.PacketType.EMAIL, key);
        String json = GSON.toJson(packet);
        if (packet != null) outgoing.println(Base64.encodeBase64String(json.getBytes()));
    }

    /**
     * Performs a handshake with the client to swap asymmetric public keys and
     * ensure the keys were received.
     *
     * @param   incoming    the BufferedReader representing the input stream of
     *                      the socket the client is connected through.
     * @param   outgoing    The PrintWriter representing the output stream of
     *                      the socket the client is connected through.
     * @return  The PublicKey of the client if the full handshake was
     *          successful, or null if the handshake was unsuccessful.
     */
    private CompletableFuture<PublicKey> exchangePublicKeys(BufferedReader incoming, PrintWriter outgoing) throws ServerException {
        Objects.requireNonNull(incoming,"\"incoming\" cannot be null" );
        Objects.requireNonNull(outgoing, "\"outgoing\" cannot be null");
        String confirmation;
        byte[] keyBytes;
        String message;
        PublicKey key;

        try { // receive client public key
            String firstMessage = incoming.readLine();
            if (firstMessage == null) return null;
            keyBytes = parseStrByteArray(firstMessage);
        } catch (IOException ioe) {
            throw new ServerException("Client connection failure while exchanging public async keys: " + ioe.getMessage());
        }
        // verify that the key's bytes exist before attempting to process them
        if (keyBytes == null || keyBytes.length <= 0)
            throw new ServerException("Unable to retrieve client's public async encryption key");
        try { // construct the public key from the received message
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            key = kf.generatePublic(spec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new ServerException("Failed to construct client's public async encryption key: " + e.getMessage());
        }
        if (key == null) // make sure that the key actually exists
            throw new ServerException("Failed to construct client's public async encryption key");
        // give the client the server public key
        outgoing.println(Arrays.toString(serverKeys.getPublic().getEncoded()));
        try { // await confirmation that the client received the server's public encryption key
            confirmation = incoming.readLine();
            if (confirmation == null) throw new ServerException("Failed to retrieve confirmation from the client after sending the server's public key");
        } catch (IOException e) {
            throw new ServerException("Communication failure while obtaining confirmation from the client that the server's public key was received: " + e.getMessage());
        }
        try { message = decryptEncryptionPacket(new String(Base64.decodeBase64(confirmation)));
        } catch (Exception e) {
            throw new ServerException("Failed to decrypt confirmation message from the client: " + e.getMessage());
        }
        if (message.equals("handshake")) return CompletableFuture.completedFuture(key);
        else throw new ServerException("Received invalid confirmation that the client received the server's public key");
    }

    private byte[] parseStrByteArray(String a) {
        if (a == null) return null;
        String[] parsed = a.replaceFirst("\\[", "").replaceFirst("]", "").trim().split(", ");
        byte[] keyBytes = new byte[parsed.length];
        for (int b=0; b<parsed.length; b++) keyBytes[b] = Byte.valueOf(parsed[b]);
        return keyBytes;
    }

    /**
     * Attempts to gracefully-ish shutdown the server and disconnect all existing client connections
     */
    @Override
    public void close() {
        if (!alive) return; alive = false;
        listenerManager.removeAllListeners();
        executorService.shutdownNow();
        threadPool.shutdownNow();
        try { serverSocket.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }


    public class ClientConnection implements Runnable, AutoCloseable {

        private PublicKey clientPublicKey;
        private BufferedReader incoming;
        private Connection connection;
        private PrintWriter outgoing;
        private TcpServer server;
        private Socket socket;
        private int timeout;

        ClientConnection(TcpServer server, Socket socket, int timeout) throws IOException {
            this.incoming = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.outgoing = new PrintWriter(socket.getOutputStream(), true);
            this.timeout = timeout < 0 ? 0 : timeout;
            this.clientPublicKey = null;
            this.server = server;
            this.socket = socket;
            connection = null;
        }

        @Override
        public void run() {
            try {
                //Logger.log("client", socket.getInetAddress().getHostAddress(), "handshake", "client requesting handshake");
                socket.setSoTimeout(5000);
                clientPublicKey = Objects.requireNonNull(exchangePublicKeys(incoming, outgoing), "client public key was null").get(2, TimeUnit.MINUTES);
                if (clientPublicKey == null) {
                    //Logger.log("client", socket.getInetAddress().getHostAddress(), "handshake", "client failed to complete handshake");
                    close();
                    return;
                }
                socket.setSoTimeout(timeout);
                //Logger.log("client", socket.getInetAddress().getHostAddress(), "handshake", "client completed handshake");
                connection = new Connection(server, socket, clientPublicKey, outgoing);
                listenerManager.raiseConnectionEvent(connection, Connection.event.CONNECTED);
                while (!socket.isClosed()) {
                    String received = incoming.readLine();
                    if (received == null) return;

                    String json = new String(Base64.decodeBase64(received));
                    EncryptionPacket packet = GSON.fromJson(json, EncryptionPacket.class);
                    String message = decryptEncryptionPacket(packet);

                    switch (packet.getPayloadType()) {
                        case TEXT:
                            listenerManager.raiseMessageEvent(new Message(connection, message));
                            break;
                        case COMMAND:
                            CommandPacket cPacket = GSON.fromJson(message, CommandPacket.class);
                            if (cPacket.getCommand().equals("sudo")) {
                                if (cPacket.getArguments().equals("disconnect")) {
                                    socket.close();
                                    break;
                                }
                            } else {
                                listenerManager.raiseCommandEvent(new Command(connection, cPacket));
                            }
                            break;
                        case EMAIL:
                            EmailPacket ePacket = GSON.fromJson(message, EmailPacket.class);
                            Email email = new Email(ePacket);
                            email.setConnection(connection);
                            listenerManager.raiseEmailEvent(email);
                            break;
                    }

                }
            } catch (SocketTimeoutException e) {
                //System.out.println("[SOCKET][" + socket.getInetAddress().getHostAddress() + "](DISCONNECTED) socket connection timed out");
            } catch (SocketException e) {
                if (!(e.getMessage().equals("Connection reset")))
                    e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }

            try { close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            listenerManager.raiseConnectionEvent(connection, Connection.event.REMOVED);
        }

        @Override
        public void close() {
            try { socket.close();
            } catch (IOException ignore) { }
            //Logger.log("socket", socket.getRemoteSocketAddress().toString(), "disconnected", "socket connection closed");
        }
    }

}