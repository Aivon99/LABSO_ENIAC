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
            
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    System.out.println("Ricevuto: " + inputLine);
                    
                    if (inputLine.equals("ping")) {
                        System.out.println("Server ha risposto: pong");
                        out.println("pong");
                    } else if (inputLine.startsWith("UPLOAD")) {
                        handleUpload(inputLine, out);
                    } else if (inputLine.startsWith("DOWNLOAD")) {
                        handleDownload(inputLine, out);
                    }
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

    private void handleUpload(String command, PrintWriter out) {
        String[] parts = command.split(",");
        if (parts.length < 2) {
            out.println("ERRORE: Formato comando non valido");
            return;
        }

        String resourceName = parts[1];
        String resourcePath = resourceManager.getResource(resourceName);
        
        if (resourcePath == null) {
            out.println("NONDISPONIBILE");
            return;
        }

        out.println("SUCCESSO");
        try {
            Files.copy(Paths.get(resourcePath), socket.getOutputStream());
        } catch (IOException e) {
            logger.severe("Errore durante l'upload: " + e.getMessage());
        }
    }

    private void handleDownload(String command, PrintWriter out) {
        String[] parts = command.split(",");
        if (parts.length < 2) {
            out.println("ERRORE: Formato comando non valido");
            return;
        }

        String resourceName = parts[1];
        String content = resourceManager.getResource(resourceName);
        
        if (content == null) {
            out.println("NONDISPONIBILE");
            return;
        }

        out.println("SUCCESSO");
        out.println(content);
    }
}