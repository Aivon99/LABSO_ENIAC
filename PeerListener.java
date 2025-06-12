import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.logging.Logger;

public class PeerListener implements Runnable {
    private final ServerSocket serverSocket;
    private final ConcurrentHashMap<String, Semaphore> activeConnections;
    private final ResourceManager resourceManager;
    private static final Logger logger = Logger.getLogger(PeerListener.class.getName());

    public PeerListener(int porta, ResourceManager resourceManager) throws IOException {
        this.serverSocket = new ServerSocket(porta);
        this.activeConnections = new ConcurrentHashMap<>();
        this.resourceManager = resourceManager;
        System.out.println("Peer in ascolto sulla porta " + porta);
    }

    @Override
    public void run() {
        ExecutorService threadPool = Executors.newFixedThreadPool(10);

        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                String clientId = clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort();
                System.out.println("Nuova connessione da: " + clientId);

                activeConnections.putIfAbsent(clientId, new Semaphore(1));
                threadPool.execute(new PeerHandler(clientSocket, activeConnections.get(clientId), resourceManager));

            } catch (IOException e) {
                logger.severe("Errore durante l'accettazione della connessione: " + e.getMessage());
            }
        }
    }
}

class PeerHandler implements Runnable {
    private final Socket socket;
    private final Semaphore connectionSemaphore;
    private final ResourceManager resourceManager;
    private static final Logger logger = Logger.getLogger(PeerHandler.class.getName());

    public PeerHandler(Socket socket, Semaphore semaphore, ResourceManager resourceManager) {
        this.socket = socket;
        this.connectionSemaphore = semaphore;
        this.resourceManager = resourceManager;
    }

    @Override
    public void run() {
        try {
            connectionSemaphore.acquire();

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            String inputLine = in.readLine();
            if (inputLine != null) {
                System.out.println("Ricevuto: " + inputLine);

                if (inputLine.equals("ping")) {
                    writer.write("pong\n");
                    writer.flush();
                    System.out.println("Server ha risposto: pong");
                } else if (inputLine.startsWith("UPLOAD")) {
                    handleUpload(inputLine, writer);
                } else if (inputLine.startsWith("DOWNLOAD")) {
                    handleDownload(inputLine, writer);
                }
            }

        } catch (IOException | InterruptedException e) {
            logger.severe("Errore nella gestione del peer: " + e.getMessage());
        } finally {
            connectionSemaphore.release();
            try {
                socket.close();
            } catch (IOException e) {
                logger.severe("Errore nella chiusura del socket: " + e.getMessage());
            }
        }
    }

    private void handleUpload(String command, BufferedWriter writer) {
        String[] parts = command.split(",");
        if (parts.length < 2) {
            sendLine(writer, "ERRORE: Formato comando non valido");
            logger.severe("Formato comando non valido ricevuto: " + command);
            return;
        }

        String resourceName = parts[1];
        logger.info("Richiesta di upload per la risorsa: " + resourceName);

        String resourcePath = resourceManager.getResource(resourceName);

        if (resourcePath == null) {
            sendLine(writer, "NONDISPONIBILE");
            logger.info("Risorsa non disponibile: " + resourceName);
            return;
        }

        try {
            sendLine(writer, "SUCCESSO");
            logger.info("Messaggio SUCCESSO inviato per: " + resourceName);

            OutputStream outRaw = socket.getOutputStream();
            logger.info("OutputStream ottenuto. Inizio copia del file: " + resourcePath);
            Files.copy(Paths.get(resourcePath), outRaw);
            outRaw.flush();
            logger.info("Trasferimento completato per la risorsa: " + resourceName);

        } catch (IOException e) {
            logger.severe("Errore durante l'upload di " + resourceName + ": " + e.getMessage());
        }
    }

    private void sendLine(BufferedWriter writer, String message) {
        try {
            writer.write(message + "\n");
            writer.flush();
        } catch (IOException e) {
            logger.severe("Errore durante l'invio della risposta: " + e.getMessage());
        }
    }

    private void handleDownload(String command, BufferedWriter writer) {
        String[] parts = command.split(",");
        if (parts.length < 2) {
            sendLine(writer, "ERRORE: Formato comando non valido");
            return;
        }

        String resourceName = parts[1];
        String content = resourceManager.getResource(resourceName);

        if (content == null) {
            sendLine(writer, "NONDISPONIBILE");
            return;
        }

        sendLine(writer, "SUCCESSO");
        sendLine(writer, content);
    }
}
