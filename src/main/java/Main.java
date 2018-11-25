
import email_system.EmailServer2;
import server.*;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Main {

    public static void main(String... args) throws ServerException, UnknownHostException {

        TcpServer server = new TcpServer(7070, 0, 5, InetAddress.getLocalHost(), true);
        System.out.println("Server is alive at " + server.getServerAddress().getHostAddress());

        new EmailServer2(server).start();

    }

}
