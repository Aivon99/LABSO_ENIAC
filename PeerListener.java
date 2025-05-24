import java.io.*;
import java.net.*;

public class PeerListener implements Runnable {
    private final ServerSocket serverSocket;

    public PeerListener(int porta) throws IOException {
        this.serverSocket = new ServerSocket(porta);
        System.out.println("Peer in ascolto sulla porta " + porta);
    }

    @Override
    public void run() {
        while (true) {
            try {
                Socket clientSocket = serverSocket.accept(); // accetta una nuova connessione
                System.out.println("Nuova connessione da: " + clientSocket.getRemoteSocketAddress());

                // Crea un nuovo thread per gestire la connessione con il peer
                new Thread(new PeerHandler(clientSocket)).start();

            } catch (IOException e) {
                System.err.println("Errore durante l'accettazione della connessione: " + e.getMessage());
            }
        }
    }
}

// Questa classe gestisce una singola connessione con un peer
class PeerHandler implements Runnable {
    private final Socket socket;

    public PeerHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        ) {
            // Thread di scrittura automatica
            new Thread(() -> {
                try {
                    while (true) {
                        out.println("[SERVER] Scrittura automatica ogni 5 secondi");
                        Thread.sleep(5000);
                    }
                } catch (Exception e) {
                    System.err.println("Errore nel thread di scrittura: " + e.getMessage());
                }
            }).start();

            // Lettura da client
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                System.out.println("Ricevuto: " + inputLine);

                if (inputLine.startsWith("upload")) {
                    // gestisci upload file
                } else if (inputLine.equals("ping")) {
                    out.println("pong");
                }
            }
        } catch (IOException e) {
            System.err.println("Errore nella gestione del peer: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Errore nella chiusura del socket: " + e.getMessage());
            }
        }
    }
}
