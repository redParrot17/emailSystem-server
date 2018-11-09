
import server.*;

public class Main {

    public static void main(String... args) throws ServerException {

        TcpServer server = new TcpServer(7070, 0, 5, true);
        System.out.println("Server is alive!");

        new HandleEmails(server);

    }

}
