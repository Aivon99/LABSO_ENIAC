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

public class Peer {
    private HashMap<String, String> hashRisorse; 
      
    private int Port;
    private String IP; //IP del peer
    
    private int PortMaster;
    private String IPMaster; //IP del peer
    
    private ServerSocket serverSocket; // socket del peer per ricevere comunicazioni dal master
    private BlockingQueue<Triplet> codaUpload; 

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
  
    public boolean caricaListaRisorse(String pathLista ){ //Carica la lista risorse da un file di testo, alla hasmap nel costruttore  
        try{
        BufferedReader reader = new BufferedReader(new FileReader(pathLista));    
        String line;


        while ((line = reader.readLine()) != null) { // Legge ogni riga del file
            String[] parts = line.split(","); // Divide la riga in due parti, nome e path, assume siano separati da virgola; 
                                                    // TODO: controlla se separatore va bene e fa prove per vedere se spazi causano problemi  

            this.aggiungiRisorsa(parts[0], parts[1]);;   //aggiunge le due componenti alla hashmap
        }
        

        reader.close();
        }
        catch (FileNotFoundException e){
            System.out.println("File non trovato: " + e.getMessage());
            return false;
        }
        catch (IOException e){
            logger.severe("Errore di I/O: " + e.getMessage());
            return false;
        }
        catch (Exception e){
            logger.severe("Errore: " + e.getMessage());
            return false;
        }   

        return true;
    }
    
    public void ascoltoPorta() { //Metodo per l'ascolto della porta, continuamente runnato in un thread separato, genera dei thread per ogni accept così che più peer possano connettersi

        while (true) {
            try {
                Socket clientSocket = serverSocket.accept(); // blocks until a peer connects
                new Thread(() -> gestisciClient(clientSocket)).start(); // Per gestione parallela
                
            } catch (IOException e) {
                System.err.println("Error accepting connection: " + e.getMessage());
            }
        }
    }   

    public void gestisciClient(Socket clientSocket) { //Assumo i messaggi in entrata siano formati in modo corretto, altrimenti si rischia di avere problemi di parsing, gestione errori da implementare
                    
        try (
                    InputStream inputStream = clientSocket.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));) { 
                    
                        // Leggi il header per determinare il tipo di dati
                    String messaggio = reader.readLine();
                     
                    String[] parti = messaggio.split(",");
                    

                    if (parti[0] != null) {

                        switch (parti[0]) {
                            case "UPLOAD":
                            if(parti.length == 4){
                             // attesa che resto del messaggio sia del tipo: IP prossimaLinea Port prossimaLinea nomeRisorsa

                                Triplet richiestaUpload = new Triplet(parti[1], new Tuple(parti[2], Integer.parseInt(parti[3])));
                                //Aggiungi a coda Upload
                                this.uploadSignal.release();

                                this.codaUpload.add(richiestaUpload);
                                break;           }
                                else{
                                    logger.severe("ERRORE, formato messaggio non valido " + messaggio);
                                    break;
                                }


                            default:
                                System.err.println("ERRORE, header non definito   " + messaggio);
                        }
                    }
                } catch (IOException e) {
                    System.err.println("ERRORE IOEXCEPTION" + e.getMessage()); //migliora
                }
             }            
     

    public Triplet getProssimoInCoda(){ // Restituisce il prossimo elemento in coda, se non ci sono elementi in coda restituisce null
        return this.codaUpload.poll(); 
    }
    public void gestisciUploadCoda(){
        //inventati modo di mettere in standby questo metodo, direi o utilizzare un semaforo (non binario) che riceve 
        //signal da metodo di ascolto  o signal await (equivalente di Java).
        

        while(true){
            try{
                this.uploadSignal.acquire();
                Triplet richiestaUpload = this.getProssimoInCoda(); // Prende il prossimo elemento in coda
                
                if(richiestaUpload != null){ // Se non è null, significa che c'è un upload da gestire
                    this.UpLoad(richiestaUpload); // Esegue tentativo upload della risorsa
                
                }
                else{
                    logger.severe("Errore: Coda upload vuota, controlla funz. semaforo");
                }
                    if((this.uploadSignal.availablePermits() == 0) && (this.codaUpload.peek() != null)){ // PER DEBUGGING, DA RIMUOVERE  SE NON CI SONO PERMESSI MA CI SONO ALTRI ELEMENTI IN CODA, SEGNA A TERMINALE, EVENTUALMENTE CAMBIARE A LOG  
                        logger.severe("Errore: Coda upload non vuota ma non ci sono permessi, controlla funz. semaforo");
                    }
            }

            /*
            catch (IOException e) {
                logger.severe("Errore durante l'upload: " + e.getMessage());
            }
             */
            catch(InterruptedException e){ //DA UTILIZZARE PER GESTIONE GRACEFUL DELLA CHIUSURA DEL THREAD, I.E. SE IL THREAD VIENE INTERROTTO GESTIRE GLI UPLOAD IN CODA, figata    
                logger.severe("Errore durante l'attesa del segnale di upload: " + e.getMessage());
            }


        }
    }
    
    public void UpLoad(Triplet RichiestaUpload) {
        String nomeRisorsa = RichiestaUpload.getRisorsa();
        String IPDestinatario = RichiestaUpload.getPeer().getIP();
        int Port = RichiestaUpload.getPeer().getPort();

        String path = this.hashRisorse.get(nomeRisorsa);
        if (path == null) {
            logger.severe("Risorsa non trovata: " + nomeRisorsa);
            return;
        }
        try (Socket collegamentoUpload = new Socket(IPDestinatario, Port);
             PrintWriter writer = new PrintWriter(collegamentoUpload.getOutputStream(), true);
             OutputStream out = collegamentoUpload.getOutputStream()) {
            
            writer.println("UPLOAD," + nomeRisorsa);
            writer.flush();
            
            // Invia il file
            Files.copy(Paths.get(path), out);
            
        } catch (Exception e) {
            logger.severe("Errore durante l'upload: " + e.getMessage());
        }
    }
 
    public void DownLoad(Triplet richiesta) {
        while (true) {
            String risposta = this.queryMaster(richiesta);
            String[] parti = risposta.split(",");
            
            if (parti[0].equals("FALLIMENTO")) {
                System.out.println("Risorsa non disponibile");
                return;
            }

            richiesta.setPeer(new Tuple(parti[1], Integer.parseInt(parti[2])));
            String path = this.richiestaPeer(richiesta);
            
            if (path.equals("NONDISPONIBILE")) {
                // Notifica al master di rimuovere questo peer dalla lista
                notifyMasterPeerUnavailable(richiesta.getRisorsa(), richiesta.getPeer());
                continue;
            }
            
            // Download completato con successo
            this.aggiungiRisorsa(richiesta.getRisorsa(), path);
            return;
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

            writer.println("QUERY," + richiesta.toString());
            String risposta = reader.readLine();

            return risposta;
        } catch (IOException e) {
            logger.severe("Errore durante la query al master: " + e.getMessage());
            return "FALLIMENTO";
        }
    }
    
    public String richiestaPeer(Triplet richiesta){ //Richiesta al peer per scaricare la risorsa, ritorna il path dove viene salvata, 
        //IMPORTANTE, IL PROF NON SPECIFICA IL TIPO DI RISORSA, IO ASSUMO CHE IL NOME INCLUDA IL FORMATO (PDF, JPG, TXT, ETC)
    
       try (
        Socket socket = new Socket(richiesta.getPeer().getIP(), richiesta.getPeer().getPort());
        PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
    
        BufferedInputStream bis = new BufferedInputStream(socket.getInputStream());
        ByteArrayOutputStream headerBuf = new ByteArrayOutputStream();
        
        
        ) {
        
        writer.println("UPLOAD," + richiesta.toString());
                
       
        int b;
        while ((b = bis.read()) != -1) {
        if (b == '\n') break; // simple header delimiter
        headerBuf.write(b);
        }
        String headerRisposta = headerBuf.toString(StandardCharsets.UTF_8);

        if (headerRisposta.equals("NONDISPONIBILE")) {
            return "NONDISPONIBILE";
        } else if (!headerRisposta.equals("SUCCESSO")) {
            logger.severe("ERRORE: Risposta non valida: " + headerRisposta);
            return null;
        }


        File outputFile = new File("received/" + richiesta.getRisorsa());

        try (FileOutputStream fileOut = new FileOutputStream(outputFile)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = bis.read(buffer)) != -1) {
                fileOut.write(buffer, 0, bytesRead);
                }
        }
        catch (IOException e) {
            logger.severe("ERRORE durante la scrittura/ricezione del file: " +richiesta.getRisorsa() +"/n" + e.getMessage());
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
            if (risorse.isEmpty()) {
                // Se non ci sono risorse, aggiungiamo una risorsa di default per test
                resourceManager.addResource("test.txt", "Contenuto di test");
                risorse = resourceManager.listResources();
            }
            String risorseStr = String.join(";", risorse);
            
            // Debug: stampa il comando di registrazione
            String comando = "REGISTER," + IP + "," + Port + "," + risorseStr;
            System.out.println("Invio comando di registrazione: " + comando);
            
            // Invia il comando di registrazione
            writer.println(comando);
            
            // Attendi la risposta
            String risposta = reader.readLine();
            if (risposta == null) {
                logger.severe("Nessuna risposta ricevuta dal master durante la registrazione");
                return;
            }
            
            if (!risposta.equals("SUCCESSO")) {
                logger.severe("Errore durante la registrazione al master: " + risposta);
            } else {
                System.out.println("Registrazione al master completata con successo");
            }
            
        } catch (IOException e) {
            logger.severe("Errore durante la registrazione al master: " + e.getMessage());
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
        }
    }

    private void notifyMasterResourceChange(String resourceName, boolean isAdded) {
        try (Socket socket = new Socket(IPMaster, PortMaster);
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {
            
            String command = isAdded ? "ADD" : "REMOVE";
            writer.println(command + "," + resourceName + "," + IP + "," + Port);
            
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
            while ((response = reader.readLine()) != null) {
                System.out.println(response);
            }
            
        } catch (IOException e) {
            logger.severe("Errore nella richiesta delle risorse remote: " + e.getMessage());
        }
    }
    
    private void downloadResource(String name) {
        Triplet request = new Triplet(name, null);
        DownLoad(request);
    }

    private void listLocalResources() {
        System.out.println("Risorse locali:");
        for (String resource : resourceManager.listResources()) {
            System.out.println("- " + resource);
        }
    }


    private void addResource(String name, String content) {
        if (resourceManager.addResource(name, content)) {
            notifyMasterResourceChange(name, true);
        }
    }


    private void quit() {
        notifyMasterDisconnection();
        running = false;
    }
}
