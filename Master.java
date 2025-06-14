import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.net.Socket;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.logging.Logger;
import java.util.HashSet;
import java.util.Set;

public class Master {
    private final HashMap<Tuple, List<String>> hashPeer; // IP+Port --> risorsa
    private final HashMap<String, List<Tuple>> hashRisorse; // risorsa --> lista di peer
    private ServerSocket serverSocket; // socket del master per ricevere le richieste dai peer
    private final Map<String, List<LogEntry>> downloadLogs;
    private final Scanner scanner;
    private boolean running;
    private final Object tableLock = new Object(); // Per la sincronizzazione della tabella
    private static final Logger logger = Logger.getLogger(Master.class.getName());

    public Master(int Port) { // costruttore del master
        this.hashRisorse = new HashMap<>();
        this.hashPeer = new HashMap<>();
        try {
            this.serverSocket = new ServerSocket(Port); // server socket del master
        } catch (Exception e) {
            this.serverSocket = null; // se non riesce a creare il server socket, lo setta a null
            System.out.println("Errore nella creazione del server socket: " + e.getMessage());
        }
        this.downloadLogs = new ConcurrentHashMap<>();
        this.scanner = new Scanner(System.in);
        this.running = true;
    }

    public void addPeer(String IP, int Port, List<String> risorse) {
        synchronized (tableLock) {
            logger.info("Acquisito lock per l'accesso a hashRisorse e hashPeer");
            Tuple peer = new Tuple(IP, Port);
            hashPeer.put(peer, risorse);

            for (String risorsa : risorse) {
                hashRisorse.computeIfAbsent(risorsa, ignora -> new ArrayList<>()).add(peer);
            }
            logger.info("Rilasciato lock per hashRisorse e hashPeer");
        }
    }

    public Tuple getPeerRisorsa(String risorsa) {
        synchronized (tableLock) {
            if (hashRisorse.containsKey(risorsa)) {
                List<Tuple> peerList = hashRisorse.get(risorsa);
                if (!peerList.isEmpty()) {
                    return peerList.get(0);
                }
            }
            return null;
        }
    }

    public void rimuoviPeer(Tuple peer) {
        List<String> risorse = hashPeer.get(peer);

        if (risorse == null) {
            System.out.println("Il peer " + peer.getIP() + ":" + peer.getPort() + " non è registrato.");
            return;
        }

        for (String risorsa : risorse) {
            if (hashRisorse.containsKey(risorsa)) {
                List<Tuple> peerList = hashRisorse.get(risorsa);
                peerList.remove(peer);
                if (peerList.isEmpty()) {
                    hashRisorse.remove(risorsa); // rimuove risorsa se nessuno la possiede più
                } else {
                    hashRisorse.put(risorsa, peerList);
                }
            }
        }

        hashPeer.remove(peer); // pulizia finale
        System.out.println("Peer rimosso: " + peer.getIP() + ":" + peer.getPort());
    }

    public void modificaPeer(Tuple peer, List<String> nuoveRisorse) { // per ora faccio così perchè più comodo se si
                                                                      // vuole si fa differenza tra attuali risorse e
                                                                      // nuove risorse ecc.
        rimuoviPeer(peer); // rimuovi il peer dalla hasmap
        addPeer(peer.getIP(), peer.getPort(), nuoveRisorse); // aggiungi il peer con le nuove risorse

    }

    public void printAllPeers() {
        System.out.println("Peers registrati:");
        for (Tuple peer : hashPeer.keySet()) {
            System.out.println(peer.getIP() + ":" + peer.getPort());
        }
    }

    public void printAllResources() {
        System.out.println("Risorse disponibili:");
        for (String risorsa : hashRisorse.keySet()) {
            System.out.print(risorsa + " -> ");
            for (Tuple t : hashRisorse.get(risorsa)) {
                System.out.print(t.getIP() + ":" + t.getPort() + " ");
            }
            System.out.println();
        }
    }

    public void inspectNodes() {
        if (hashPeer.isEmpty()) {
            System.out.println("Nessun peer registrato.");
            return;
        }

        System.out.println("Peers:");
        for (Tuple peer : hashPeer.keySet()) {
            System.out.println(peer.getIP() + ":" + peer.getPort() + ":");
            // Ottieni tutte le risorse associate a questo peer
            Set<String> peerResources = new HashSet<>();
            for (Map.Entry<String, List<Tuple>> entry : hashRisorse.entrySet()) {
                if (entry.getValue().contains(peer)) {
                    peerResources.add(entry.getKey());
                }
            }
            // Stampa le risorse del peer
            for (String resource : peerResources) {
                System.out.println("  - " + resource);
            }
        }
    }

    public void ascoltaPorta() {
        ExecutorService threadPool = Executors.newCachedThreadPool(); // or fixed thread pool

        while (running) {
            try {
                Socket clientSocket = serverSocket.accept(); // blocks until a peer connects
                threadPool.execute(() -> handleClient(clientSocket));
            } catch (IOException e) {
                if (!running) {
                    // If the server is not running, break the loop
                    break;
                }
                System.err.println("Error accepting connection: " + e.getMessage());
            }
        }
        threadPool.shutdown();
    }

    private void handleClient(Socket socket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {

            String request = reader.readLine();
            String[] parti = request.split(",");

            switch (parti[0]) {
                case "REGISTER":
                    handleRegister(parti, writer);
                    break;
                case "QUERY":
                    handleQuery(parti, writer);
                    break;
                case "DOWNLOAD":
                    handleDownload(parti, writer);
                    break;
                case "LISTDATA":
                    handleListData(writer);
                    break;
                case "QUIT":
                    handleQuit(parti, writer);
                    break;
                case "MODIFICA_PEER":
                    Tuple nodo = new Tuple(parti[1], Integer.parseInt(parti[2]));
                    modificaRisorsePeer(writer, reader, nodo);

                    break;
                case "REMOVE_PEER":
                    handleRemovePeer(parti, writer);
                    break;
                case "LISTPEERS":
                    handleListPeers(writer);
                    break;
                case "RESOURCE_UPDATE":
                    handleResourceUpdate(parti, writer);
                    break;
                case "PEER_UNAVAILABLE":
                    handlePeerUnavailable(parti, writer);
                    break;
                case "UNREGISTER":
                    handleUnregister(parti);
                    writer.println("OK");
                    break;
                default:
                    writer.println("ERROR");
            }
        } catch (IOException e) {
            logger.severe("Errore nella gestione del client: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                logger.severe("Errore nella chiusura del socket: " + e.getMessage());
            }
        }
    }

    private void modificaRisorsePeer(PrintWriter writer, BufferedReader reader, Tuple nodo) {
        try {
            List<String> nuoveRisorse = new ArrayList<>();
            String linea;

            while ((linea = reader.readLine()) != null) {
                if (linea.equals("FINE")) {
                    break;
                }
                nuoveRisorse.add(linea.trim());
            }

            synchronized (tableLock) {
                logger.info("Acquisito lock per l'accesso a hashRisorse e hashPeer");
                // Rimuovi il peer esistente
                rimuoviPeer(nodo);
                // Aggiungi il peer con le nuove risorse
                addPeer(nodo.getIP(), nodo.getPort(), nuoveRisorse);
                logger.info("Rilasciato lock per hashRisorse e hashPeer");
            }

            writer.println("SUCCESSO");
            System.out.println("Aggiornate risorse per il peer " + nodo.getIP() + ":" + nodo.getPort());
            System.out.println("Nuove risorse: " + String.join(", ", nuoveRisorse));

        } catch (IOException e) {
            logger.severe("Errore nella modifica delle risorse del peer: " + e.getMessage());
            writer.println("ERRORE: " + e.getMessage());
        }
    }

    private void handleRegister(String[] parti, PrintWriter writer) {
        try {
            System.out.println("Ricevuta richiesta di registrazione: " + String.join(",", parti));

            if (parti.length < 3) {
                System.out.println("ERRORE: Formato comando non valido. Parti ricevute: " + parti.length);
                writer.println("ERRORE: Formato comando non valido");
                return;
            }

            String IP = parti[1];
            int Port = Integer.parseInt(parti[2]);

            List<String> risorse = new ArrayList<>();
            if (parti.length >= 4 && !parti[3].isEmpty()) {
                risorse = Arrays.asList(parti[3].split(";"));
            }

            System.out.println("Registrazione nuovo peer: " + IP + ":" + Port);
            System.out.println("Risorse: " + String.join(", ", risorse));

            synchronized (tableLock) {
                addPeer(IP, Port, risorse);
            }

            writer.println("SUCCESSO");
            System.out.println("Registrazione completata con successo");

        } catch (Exception e) {
            logger.severe("Errore durante la registrazione del peer: " + e.getMessage());
            writer.println("ERRORE: " + e.getMessage());
        }
    }

    private void handleQuery(String[] parti, PrintWriter writer) {
        String risorsa = parti[1];
        synchronized (tableLock) {
            if (hashRisorse.containsKey(risorsa)) {
                List<Tuple> peerList = hashRisorse.get(risorsa);
                if (!peerList.isEmpty()) {
                    String response = "SUCCESSO";
                    for (Tuple peer : peerList) {
                        response += "," + peer.getIP() + "," + peer.getPort();
                    }
                    writer.println(response);
                    return;
                }
            }
            writer.println("FALLIMENTO");
        }
    }

    private void handleDownload(String[] parti, PrintWriter writer) {
        if (parti.length != 6) {
            writer.println("ERRORE: Formato comando non valido");
            return;
        }

        String risorsa = parti[1];
        String fromIP = parti[2];
        int fromPort = Integer.parseInt(parti[3]);
        String toIP = parti[4];
        int toPort = Integer.parseInt(parti[5]);

        // Crea una nuova entry nel log
        LogEntry entry = new LogEntry(risorsa, fromIP + ":" + fromPort, toIP + ":" + toPort, true);
        
        synchronized (downloadLogs) {
            downloadLogs.computeIfAbsent(risorsa, k -> new ArrayList<>()).add(entry);
            logger.info("Aggiunto log per il download di " + risorsa + " da " + fromIP + ":" + fromPort + " a " + toIP + ":" + toPort);
        }
        
        writer.println("SUCCESSO");
    }

    private void handleListData(PrintWriter writer) {
        synchronized (tableLock) {
            for (Map.Entry<String, List<Tuple>> entry : hashRisorse.entrySet()) {
                writer.println(entry.getKey() + ": " +
                        entry.getValue().stream()
                                .map(t -> t.getIP() + ":" + t.getPort())
                                .collect(Collectors.joining(", ")));
            }
        }
    }

    private void handleQuit(String[] parti, PrintWriter writer) {
        System.out.println("Arresto del master...");
        running = false;
        try {
            serverSocket.close();
        } catch (IOException e) {
            logger.severe("Errore durante la chiusura del server: " + e.getMessage());
        }
        System.out.println("Master arrestato con successo");
    }

    private void handleRemovePeer(String[] parti, PrintWriter writer) {
        synchronized (tableLock) {
            String risorsa = parti[1];
            String IP = parti[2];
            int Port = Integer.parseInt(parti[3]);
            Tuple peer = new Tuple(IP, Port);

            if (hashRisorse.containsKey(risorsa)) {
                List<Tuple> peerList = hashRisorse.get(risorsa);
                peerList.remove(peer);
                if (peerList.isEmpty()) {
                    hashRisorse.remove(risorsa);
                }
            }
            writer.println("SUCCESSO");
        }
    }

    private void handleListPeers(PrintWriter writer) {
        synchronized (tableLock) {
            for (Tuple peer : hashPeer.keySet()) {
                writer.println(peer.getIP() + ":" + peer.getPort());

            }
        }
    }

    private void handleResourceUpdate(String[] parts, PrintWriter writer) {
        if (parts.length != 5) {
            writer.println("ERRORE");
            return;
        }

        String peerIP = parts[1];
        int peerPort = Integer.parseInt(parts[2]);
        boolean isAvailable = parts[3].equals("1");
        String resourceName = parts[4];

        synchronized (tableLock) {
            Tuple peer = new Tuple(peerIP, peerPort);
            if (isAvailable) {
                if (!hashRisorse.containsKey(resourceName)) {
                    hashRisorse.put(resourceName, new ArrayList<>());
                }
                // Verifica se il peer è già presente nella lista
                List<Tuple> peerList = hashRisorse.get(resourceName);
                if (!peerList.contains(peer)) {
                    peerList.add(peer);
                    logger.info("Aggiunta risorsa " + resourceName + " al peer " + peerIP + ":" + peerPort);
                }
            } else {
                if (hashRisorse.containsKey(resourceName)) {
                    List<Tuple> peerList = hashRisorse.get(resourceName);
                    peerList.remove(peer);
                    if (peerList.isEmpty()) {
                        hashRisorse.remove(resourceName);
                    }
                    logger.info("Rimossa risorsa " + resourceName + " dal peer " + peerIP + ":" + peerPort);
                }
            }
        }
        writer.println("OK");
    }

    private void handlePeerUnavailable(String[] parts, PrintWriter writer) {
        if (parts.length != 3) {
            writer.println("ERRORE");
            return;
        }

        String peerIP = parts[1];
        int peerPort = Integer.parseInt(parts[2]);

        synchronized (tableLock) {
            Tuple peer = new Tuple(peerIP, peerPort);
            if (hashRisorse.containsKey(peerIP)) {
                List<Tuple> peerList = hashRisorse.get(peerIP);
                peerList.remove(peer);
                if (peerList.isEmpty()) {
                    hashRisorse.remove(peerIP);
                }
            }
            logger.info("Peer " + peerIP + ":" + peerPort + " marcato come non disponibile");
        }
        writer.println("OK");
    }

    private void handleUnregister(String[] parts) {
        if (parts.length != 3) {
            return;
        }
        
        String peerIP = parts[1];
        int peerPort = Integer.parseInt(parts[2]);
        Tuple peer = new Tuple(peerIP, peerPort);
        
        synchronized (tableLock) {
            // Rimuovi il peer dalla lista
            rimuoviPeer(peer);
            
            // Rimuovi tutte le risorse associate al peer
            hashRisorse.entrySet().removeIf(entry -> {
                List<Tuple> peers = entry.getValue();
                return peers.remove(peer) && peers.isEmpty();
            });
            
            logger.info("Peer " + peerIP + ":" + peerPort + " disconnesso. Risorse rimosse.");
        }
    }

    public void startInteractiveSession() {
        new Thread(this::handleUserInput).start();
        System.out.println("Master avviato sulla porta " + serverSocket.getLocalPort());
        System.out.println("Comandi disponibili:");
        System.out.println("- listdata: mostra tutte le risorse disponibili");
        System.out.println("- inspectNodes: mostra dettagli di tutti i peer");
        System.out.println("- log: mostra lo storico dei download");
        System.out.println("- quit: arresta il master");
    }

    private void handleUserInput() {
        while (running) {
            System.out.print("> ");
            String command = scanner.nextLine().trim();
            if (!command.isEmpty()) {
                processCommand(command);
            }
        }
    }

    private void processCommand(String command) {
        String[] parts = command.split(" ");
        switch (parts[0]) {
            case "listdata":
                listAllResources();
                break;
            case "inspectNodes":
                inspectNodes();
                break;
            case "log":
                showLogs();
                break;
            case "quit":
                quit();
                break;
            default:
                System.out.println("Comando non riconosciuto");
        }
    }

    private void listAllResources() {
        synchronized (tableLock) {
            System.out.println("Risorse disponibili:");
            for (Map.Entry<String, List<Tuple>> entry : hashRisorse.entrySet()) {
                System.out.print(entry.getKey() + ": ");
                System.out.println(entry.getValue().stream()
                        .map(t -> t.getIP() + ":" + t.getPort())
                        .collect(Collectors.joining(", ")));
            }
        }
    }

    private void showLogs() {
        if (downloadLogs.isEmpty()) {
            System.out.println("Nessun download registrato.");
            return;
        }

        System.out.println("Risorse scaricate:");
        synchronized (downloadLogs) {
            for (List<LogEntry> entries : downloadLogs.values()) {
                for (LogEntry entry : entries) {
                    System.out.println(entry);
                }
            }
        }
    }

    private void quit() {
        System.out.println("Arresto del master...");
        running = false;
        try {
            serverSocket.close();
        } catch (IOException e) {
            logger.severe("Errore durante la chiusura del server: " + e.getMessage());
        }
        System.out.println("Master arrestato con successo");
    }
}
