
//TODO DA ORDINARE E SEPARARE PER TIPO
import java.util.logging.Logger;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.Scanner;
import java.util.Map;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.concurrent.LinkedBlockingQueue;
import java.net.ServerSocket;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Semaphore;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;

import java.util.List;
import java.util.ArrayList;

public class Peer {
    private HashMap<String, String> hashRisorse;

    private int Port;
    private String IP; // IP del peer

    private int PortMaster;
    private String IPMaster; // IP del peer

    private ServerSocket serverSocket; // socket del peer per ricevere comunicazioni dal master
    private BlockingQueue<RichiestaUpload> codaUpload; // coda per gestire le richieste di upload in arrivo dai peer,
                                                       // contiene la Triplet con le info ed il socket di riferimento

    private Semaphore uploadSignal; // semaforo per gestire gli upload in coda

    private static final Logger logger = Logger.getLogger(Peer.class.getName());

    private final ResourceManager resourceManager;
    private final Scanner scanner;
    private boolean running;

    public Peer(String IP, int Port, String IPMaster, int PortMaster) {
        this.hashRisorse = new HashMap<>();
        this.IPMaster = IPMaster;
        this.PortMaster = PortMaster;
        this.IP = IP;
        this.Port = Port;
        this.codaUpload = new LinkedBlockingQueue<>();
        this.uploadSignal = new Semaphore(0);
        this.resourceManager = new ResourceManager("resources/" + Port);
        this.scanner = new Scanner(System.in);
        this.running = true;

        try {
            this.serverSocket = new ServerSocket(Port);
            System.out.println("Server socket creato sulla porta " + Port);
        } catch (IOException e) {
            logger.severe("Errore nella creazione del server socket: " + e.getMessage());
            throw new RuntimeException("Impossibile creare il server socket sulla porta " + Port, e);
        }
    }

    public String getIP() {
        return IP;
    }

    public int getPort() {
        return Port;
    }

    public void setPort(int Port) {
        this.Port = Port;
    }

    public void aggiungiRisorsa(String name, String path) {
        logger.info("Aggiunta risorsa: " + name + " con percorso: " + path);
        hashRisorse.put(name, path);
    }

    public String getPath(String name) {
        return hashRisorse.get(name);
    }

    public Set<String> getNomiRisorse() {
        return hashRisorse.keySet();
    }

    public Map<String, String> getTutteRisorse() {
        return hashRisorse;
    }

    public boolean caricaListaRisorse(String pathLista) {
        try (BufferedReader reader = new BufferedReader(new FileReader(pathLista))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Gestione spazi e separatore
                String[] parts = line.trim().split("\\s*,\\s*");
                if (parts.length == 2) {
                    this.aggiungiRisorsa(parts[0].trim(), parts[1].trim());
                } else {
                    logger.warning("Formato non valido nella riga: " + line);
                }
            }
            return true;
        } catch (Exception e) {
            logger.severe("Errore durante il caricamento delle risorse: " + e.getMessage());
            return false;
        }
    }

    public void ascoltoPorta() { // Metodo per l'ascolto della porta, continuamente runnato in un thread
                                 // separato, genera dei thread per ogni accept così che più peer possano
                                 // connettersi

        while (true) {
            try {
                Socket clientSocket = serverSocket.accept(); // blocks until a peer connects
                new Thread(() -> gestisciClient(clientSocket)).start(); // Per gestione parallela

            } catch (IOException e) {
                System.err.println("Error accepting connection: " + e.getMessage());
            }
        }
    }

    public void gestisciClient(Socket clientSocket) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String messaggio = reader.readLine();
            String[] parti = messaggio.split(",");

            if (parti[0] != null) {

                switch (parti[0]) {
                    case "UPLOAD":
                        if (parti.length == 4) {
                            RichiestaUpload richiestaUpload = new RichiestaUpload(
                                    new Triplet(parti[1], new Tuple(parti[2], Integer.parseInt(parti[3]))),
                                    clientSocket);
                            logger.info("Aggiunta richiesta di upload per la risorsa: "
                                    + richiestaUpload.getRichiesta().getRisorsa());
                            this.codaUpload.add(richiestaUpload);
                            this.uploadSignal.release();
                            logger.info("Rilasciato semaforo per l'upload");
                            break;
                        } else {
                            logger.severe("ERRORE, formato messaggio non valido " + messaggio);
                            break;
                        }

                    default:
                        System.err.println("ERRORE, header non definito   " + messaggio);
                }
            }
        } catch (IOException e) {
            System.err.println("ERRORE IOEXCEPTION" + e.getMessage());
        }
    }

    public RichiestaUpload getProssimoInCoda() { // Restituisce il prossimo elemento in coda, se non ci sono elementi in
                                                 // coda restituisce null
        return this.codaUpload.poll();
    }

    public void gestisciUploadCoda() {
        while (true) {
            try {
                logger.info("Tentativo di acquisire il semaforo per l'upload");
                this.uploadSignal.acquire();
                logger.info("Semaforo acquisito per l'upload");
                RichiestaUpload richiestaUpload = this.getProssimoInCoda(); // Prende il prossimo elemento in coda

                if (richiestaUpload != null) { // Se non è null, significa che c'è un upload da gestire
                    logger.info("Gestione dell'upload per la risorsa: " + richiestaUpload.getRichiesta().getRisorsa());
                    this.UpLoad(richiestaUpload); // Esegue tentativo upload della risorsa

                } else {
                    logger.severe("Errore: Coda upload vuota, controlla funz. semaforo");
                }
                if ((this.uploadSignal.availablePermits() == 0) && (this.codaUpload.peek() != null)) { // PER DEBUGGING,
                    logger.severe("Errore: Coda upload non vuota ma non ci sono permessi, controlla funz. semaforo");
                }
            } catch (InterruptedException e) {
                logger.severe("Errore durante l'attesa del segnale di upload: " + e.getMessage());
            }
        }
    }

    public void UpLoad(RichiestaUpload richiestaUpload) {
        String nomeRisorsa = richiestaUpload.getRichiesta().getRisorsa();
        Socket socketUpload = richiestaUpload.getSocket();

        // Ricarica le risorse da disco
        resourceManager.reload();

        String path = this.hashRisorse.get(nomeRisorsa);
        if (path == null) {
            try (PrintWriter writer = new PrintWriter(socketUpload.getOutputStream(), true)) {
                writer.println("NONDISPONIBILE");
                writer.flush();
                logger.info("Risorsa non disponibile: " + nomeRisorsa);
            } catch (IOException e) {
                logger.severe("Errore durante l'invio della risposta: " + e.getMessage());
            }
            return;
        }

        File file = new File(path);
        if (!file.exists()) {
            logger.severe("Il file non esiste: " + path);
            try (PrintWriter writer = new PrintWriter(socketUpload.getOutputStream(), true)) {
                writer.println("NONDISPONIBILE");
                writer.flush();
                logger.info("Risorsa non trovata: " + nomeRisorsa);
            } catch (IOException e) {
                logger.severe("Errore durante l'invio della risposta: " + e.getMessage());
            }
            return;
        }

        try (
                PrintWriter writer = new PrintWriter(socketUpload.getOutputStream(), true);
                OutputStream out = socketUpload.getOutputStream()) {

            writer.println("SUCCESSO");
            writer.flush();
            logger.info("Inizio trasferimento della risorsa: " + nomeRisorsa);
            // Invia il file
            Files.copy(file.toPath(), out);
            out.flush();
            logger.info("Trasferimento completato per la risorsa: " + nomeRisorsa);

        } catch (Exception e) {
            logger.severe("Errore durante l'upload: " + e.getMessage());
        } finally {
            try {
                socketUpload.close();
            } catch (IOException e) {
                logger.warning("Errore nella chiusura del socket: " + e.getMessage());
            }
        }
    }

    public void DownLoad(Triplet richiesta) {
        int tentativi = 0;
        final int MAX_TENTATIVI = 3;

        while (tentativi < MAX_TENTATIVI) {
            try {
                logger.info("Tentativo di download della risorsa: " + richiesta.getRisorsa() + " (Tentativo "
                        + (tentativi + 1) + ")");
                String risposta = this.queryMaster(richiesta);
                String[] parti = risposta.split(",");

                if (parti[0].equals("FALLIMENTO")) {
                    logger.warning("Risorsa non disponibile sulla rete: " + richiesta.getRisorsa());
                    return;
                }

                // Verifica se ci sono altri peer oltre a noi stessi
                List<Tuple> availablePeers = new ArrayList<>();
                for (int i = 1; i < parti.length; i += 2) {
                    String peerIP = parti[i];
                    int peerPort = Integer.parseInt(parti[i + 1]);
                    Tuple peer = new Tuple(peerIP, peerPort);
                    // Aggiungi il peer solo se non siamo noi stessi
                    if (!(peerIP.equals(this.IP) && peerPort == this.Port)) {
                        availablePeers.add(peer);
                    }
                }

                // Se non ci sono altri peer disponibili oltre a noi stessi
                if (availablePeers.isEmpty()) {
                    logger.info("La risorsa e' disponibile solo su questo peer. Download non necessario.");
                    return;
                }

                // Scegli un peer casuale tra quelli disponibili
                Tuple selectedPeer = availablePeers.get((int) (Math.random() * availablePeers.size()));
                richiesta.setPeer(selectedPeer);
                logger.info("Tentativo di download da: " + selectedPeer.getIP() + ":" + selectedPeer.getPort());

                String path = this.richiestaPeer(richiesta);

                if (path == null || path.equals("NONDISPONIBILE")) {
                    logger.warning("Peer non disponibile o errore nel download, richiedo un altro peer al master...");
                    notifyMasterPeerUnavailable(richiesta.getRisorsa(), selectedPeer);
                    tentativi++;
                    continue;
                }

                String content = Files.readString(Paths.get(path), StandardCharsets.UTF_8);
                resourceManager.addResource(richiesta.getRisorsa(), content);
                this.aggiungiRisorsa(richiesta.getRisorsa(), path);
                notifyMasterResourceChange(richiesta.getRisorsa(), true);
                logger.info("Download completato con successo per la risorsa: " + richiesta.getRisorsa());
                return;

            } catch (Exception e) {
                logger.severe("Errore durante il download: " + e.getMessage());
                tentativi++;
                if (tentativi >= MAX_TENTATIVI) {
                    logger.severe("Numero massimo di tentativi raggiunto per la risorsa: " + richiesta.getRisorsa());
                    return;
                }
                logger.info("Riprovo con un altro peer...");
            }
        }
    }

    private void notifyMasterPeerUnavailable(String risorsa, Tuple peer) {
        try (Socket socket = new Socket(IPMaster, PortMaster);
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {

            writer.println("REMOVE_PEER," + risorsa + "," + peer.getIP() + "," + peer.getPort());

        } catch (IOException e) {
            logger.severe("Errore nella notifica al master: " + e.getMessage());
        }
    }

    public String queryMaster(Triplet richiesta) {
        try (Socket socket = new Socket(IPMaster, PortMaster);
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            writer.println("QUERY," + richiesta.getRisorsa());
            String risposta = reader.readLine();

            if (risposta == null) {
                return "FALLIMENTO";
            }

            return risposta;
        } catch (IOException e) {
            logger.severe("Errore durante la query al master: " + e.getMessage());
            return "FALLIMENTO";
        }
    }

    public String richiestaPeer(Triplet richiesta) {
        try (
                Socket socket = new Socket(richiesta.getPeer().getIP(), richiesta.getPeer().getPort());
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

                BufferedInputStream bis = new BufferedInputStream(socket.getInputStream());
                ByteArrayOutputStream headerBuf = new ByteArrayOutputStream();

        ) {

            writer.println("UPLOAD," + richiesta.toString());
            logger.info("Richiesta di upload inviata a " + richiesta.getPeer().getIP() + ":"
                    + richiesta.getPeer().getPort());

            int b;
            while ((b = bis.read()) != -1) {
                if (b == '\n')
                    break; // simple header delimiter
                headerBuf.write(b);
            }
            String headerRisposta = headerBuf.toString(StandardCharsets.UTF_8).trim();
            logger.info("Risposta ricevuta: " + headerRisposta);
            System.out.println("DEBUG: risposta ricevuta: " + headerRisposta);

            if (headerRisposta.equals("NONDISPONIBILE")) {
                return "NONDISPONIBILE";
            } else if (!headerRisposta.equals("SUCCESSO")) {
                logger.severe("ERRORE: Risposta non valida: " + headerRisposta);
                return null;
            }

            File receivedDir = new File("received/" + this.Port);
            if (!receivedDir.exists()) {
                receivedDir.mkdirs();
            }
            String originalName = richiesta.getRisorsa();
            File outputFile = new File(receivedDir, originalName);

            int count = 1;
            while (outputFile.exists()) {
                String name = originalName;
                String baseName, extension;

                int dotIndex = name.lastIndexOf(".");
                if (dotIndex != -1) {
                    baseName = name.substring(0, dotIndex);
                    extension = name.substring(dotIndex);
                } else {
                    baseName = name;
                    extension = "";
                }

                String newName = baseName + "-dl" + (count > 1 ? "(" + count + ")" : "") + extension;
                outputFile = new File(receivedDir, newName);
                count++;

            }
            richiesta.setRisorsa(outputFile.getName());

            try (FileOutputStream fileOut = new FileOutputStream(outputFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = bis.read(buffer)) != -1) {
                    fileOut.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                logger.severe("ERRORE durante la scrittura/ricezione del file: " + richiesta.getRisorsa() + "\n"
                        + e.getMessage());
                return null;
            }
            return outputFile.getAbsolutePath();

        } catch (IOException e) {
            logger.severe("Errore durante la ricezione del file: " + e.getMessage());
            return null;
        }
    }

    public void registratiAMaster() {
        try (Socket socket = new Socket(IPMaster, PortMaster);
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Costruisci la lista delle risorse come stringa separata da punto e virgola
            Set<String> risorse = resourceManager.listResources();

            String risorseStr = String.join(";", risorse);

            // Debug: stampa il comando di registrazione
            String comando = "REGISTER," + IP + "," + Port + "," + risorseStr;
            System.out.println("Invio comando di registrazione: " + comando);

            // Invia il comando di registrazione
            writer.println(comando);

            // Attendi la risposta
            String risposta = reader.readLine();
            if (risposta == null) {
                throw new IOException("Nessuna risposta ricevuta dal master");
            }

            if (!risposta.equals("SUCCESSO")) {
                throw new IOException("Errore durante la registrazione: " + risposta);
            }

            System.out.println("Registrazione al master completata con successo");

        } catch (IOException e) {
            System.err.println("Errore di connessione al master: " + e.getMessage());
            System.err.println("Impossibile connettersi al master all'indirizzo " + IPMaster + ":" + PortMaster);
            System.exit(1);
        }
    }

    public void startInteractiveSession() {
        new Thread(this::handleUserInput).start();
    }

    private void handleUserInput() {
        while (running) {
            System.out.print("> ");
            String command = scanner.nextLine();
            processCommand(command);
        }
    }

    private void processCommand(String command) {
        String[] parts = command.split(" ");
        switch (parts[0]) {
            case "listdata":
                if (parts.length > 1 && parts[1].equals("local")) {
                    listLocalResources();
                } else if (parts.length > 1 && parts[1].equals("remote")) {
                    listRemoteResources();
                }
                break;
            case "add":
                if (parts.length >= 3) {
                    addResource(parts[1], parts[2]);
                }
                break;
            case "download":
                if (parts.length >= 2) {
                    downloadResource(parts[1]);
                }
                break;
            case "quit":
                quit();
                break;
            case "listpeers":
                requestPeerListFromMaster();
                break;
            case "read":
                if (parts.length >= 2) {
                    readFile(parts[1]);
                } else {
                    System.out.println("Uso corretto: read <nome_file>");
                }
                break;

        }
    }
    // For hashmap che va da Stringa: NomeRisorsa a Stringa: PathRisorsa

    private void readFile(String fileName) {
        String path = hashRisorse.get(fileName);
        if (path == null) {
            System.out.println("Il file '" + fileName + "' non e' presente tra le risorse locali.");
            return;
        }

        File file = new File(path);
        if (!file.exists()) {
            System.out.println("Il file '" + fileName + "' non esiste fisicamente nel percorso: " + path);
            return;
        }

        try {
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            System.out.println("Contenuto di " + fileName + ":");
            System.out.println(content);
        } catch (IOException e) {
            System.out.println("Errore nella lettura del file: " + e.getMessage());
        }
    }

    private void notifyMasterResourceChange(String resourceName, boolean isAdded) {
        try (Socket socket = new Socket(IPMaster, PortMaster);
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {

            writer.println("MODIFICA_PEER," + IP + "," + Port);

            // Invia tutte le risorse attuali
            for (String risorsa : resourceManager.listResources()) {
                writer.println(risorsa);
            }
            writer.println("FINE");

            System.out.println("Notificato al master l'aggiornamento delle risorse");

        } catch (IOException e) {
            logger.severe("Errore nella notifica al master: " + e.getMessage());
        }
    }

    private void notifyMasterDisconnection() {
        try (Socket socket = new Socket(IPMaster, PortMaster);
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {

            writer.println("QUIT," + IP + "," + Port);

        } catch (IOException e) {
            logger.severe("Errore nella notifica di disconnessione al master: " + e.getMessage());
        }
    }

    private void listRemoteResources() {
        try (Socket socket = new Socket(IPMaster, PortMaster);
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            writer.println("LISTDATA");
            String response;
            boolean hasResources = false;

            System.out.println("Risorse remote:");
            while ((response = reader.readLine()) != null) {
                hasResources = true;
                System.out.println("- " + response);
            }

            if (!hasResources) {
                System.out.println("Nessuna risorsa remota disponibile");
            }

        } catch (IOException e) {
            logger.severe("Errore nella richiesta delle risorse remote: " + e.getMessage());
        }
    }

    private void downloadResource(String name) {
        System.out.println("Richiesta download della risorsa: " + name);
        Triplet request = new Triplet(name, null);
        DownLoad(request);
    }

    private void listLocalResources() {
        Set<String> risorse = resourceManager.listResources();
        if (risorse.isEmpty()) {
            System.out.println("Nessuna risorsa locale disponibile");
            return;
        }

        System.out.println("Risorse locali:");
        for (String resource : risorse) {
            System.out.println("- " + resource);
        }
    }

    private void addResource(String name, String content) {
        if (hashRisorse.containsKey(name)) {
            System.out.println("Errore: Possiedi una risorsa uguale chiamata '" + name + "'.");
            return;
        }

        if (resourceManager.addResource(name, content)) {
            this.aggiungiRisorsa(name, resourceManager.getResourcePath(name));
            System.out.println("Risorsa aggiunta: " + name);
            notifyMasterResourceChange(name, true);
        } else {
            System.err.println("Errore durante l'aggiunta della risorsa");
        }
    }

    private void quit() {
        System.out.println("Disconnessione dalla rete...");
        notifyMasterDisconnection();
        running = false;
        System.out.println("Disconnesso con successo");
    }

    private void requestPeerListFromMaster() {
        try (Socket socket = new Socket(IPMaster, PortMaster);
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            writer.println("LISTPEERS");
            String response;
            boolean hasPeers = false;

            System.out.println("Lista dei peer:");
            while ((response = reader.readLine()) != null) {
                hasPeers = true;
                System.out.println("- " + response);
            }

            if (!hasPeers) {
                System.out.println("Nessun peer disponibile");
            }

        } catch (IOException e) {
            logger.severe("Errore nella richiesta della lista dei peer: " + e.getMessage());
        }
    }
}
