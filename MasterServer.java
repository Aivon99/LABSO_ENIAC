import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.util.Scanner;

public class MasterServer {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Uso: java MasterServer <porta>");
            return;
        }

        int port = Integer.parseInt(args[0]);
        Master master = new Master();

        // Thread per la console interattiva
        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.print("MASTER> ");
                String cmd = scanner.nextLine().trim();

                switch (cmd.toLowerCase()) {
                    case "listdata":
                        synchronized (master) {
                            master.printAllResources();
                        }
                        break;
                    case "listpeers":
                        synchronized (master) {
                            master.printAllPeers();
                        }
                        break;
                    case "log":
                        synchronized (master) {
                            master.stampaLog();
                        }
                        break;
                    case "quit":
                        System.out.println("Chiusura del server...");
                        System.exit(0);
                    default:
                        System.out.println("Comandi disponibili: listdata, listpeers, log, quit");
                        break;
                }

            }
        }).start();

        // Socket server
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
