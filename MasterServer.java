import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;

public class MasterServer {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Uso: java MasterServer <porta>");
            return;
        }

        int port = Integer.parseInt(args[0]);
        Master master = new Master();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Master in ascolto sulla porta " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Connessione accettata da: " + socket.getInetAddress());
                new Thread(new MasterHandler(socket, master)).start();
            }
        } catch (IOException e) {
            System.err.println("Errore nel server: " + e.getMessage());
        }
    }
}
