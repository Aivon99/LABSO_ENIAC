
//TODO DA ORDINARE E SEPARARE PER TIPO
import java.util.logging.Logger;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.Map;
import java.io.IOException;
import java.net.ServerSocket;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
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


    public Peer(String IP, int Port, String IPMaster, int PortMaster) {
                
     //TODO modificare metodo scelta IP e porta (specifiche non li danno come input)
        
        this.hashRisorse = new HashMap<>();
        
        this.IPMaster = IPMaster;
        this.PortMaster = PortMaster;
         
        this.IP = IP;
        this.Port = Port;

        this.codaUpload = new LinkedBlockingQueue<>();
        
        this.uploadSignal = new Semaphore(0);
        
        try {
            this.serverSocket = new ServerSocket(Port); // server socket 
        } catch (Exception e) {
            this.serverSocket = null; // se non riesce a creare il server socket, lo setta a null
            logger.severe("Errore nella creazione del server socket: " + e.getMessage());
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
    
    public void UpLoad(Triplet RichiestaUpload){
               //preferenza personnale, trovo più comodo passare la tripletta ed estrarre i dati solo quando necessario
        
        String nomeRisorsa = RichiestaUpload.getRisorsa();
        String IPDestinatario = RichiestaUpload.getPeer().getIP();
        int Port = RichiestaUpload.getPeer().getPort();

        
        String path = this.hashRisorse.get(nomeRisorsa);
        //Fabbrica ed aggiungi metodo lettura  
        
        try{
            Socket collegamentoUpload = new Socket(IPDestinatario, Port) ;
            
            // TODO Fabbrica ed aggiungi metodo upload 
            
            
            collegamentoUpload.close();
        }
        catch(Exception e){
            System.out.println("Placeholder generico per gestione dell'errore durante prove, stack segue" + e.getMessage()); //CAMBIAMI
        }

        }
 
    public void DownLoad(Triplet richiesta){ 
        //La triplet viene inizialmente inizializzata come null nel peer e stringa nomerisorsa valido, il metodo è poi ricorsivo, qualora un tentativo fallisca
        // il tentativo di download viene ripetuto, con il peer appena tentato (che il master elimina dalla lista della risorsa in questione)  
       
       //manda richiesta al master per ottenere la lista peer con risorsa
        
        String risposta = this.queryMaster(richiesta); //ATTESO che master risponda con SUCCESSO o FALLIMENTO, nel primo caso il peer è disponibile ed il resto della stringa è la triplet 
        

        //Valuta risposta (ho messo dei .trim() per eventualmente riparare a spazi messi per sbaglio alle estremità)
        
        String[] parti = risposta.split(",", -1); 
        if(parti[0].trim().equals("FALLIMENTO")){ //se il master non ha trovato la risorsa termina tentativi
            System.out.println("Risorsa non disponibile");
            return;
        }
            else if(!parti[0].trim().equals("SUCCESSO")){ //se la risposta non è coerente 
                logger.severe("Errore: risposta del master non valida");
                return;
            }

        //  manda richiesta al peer indicato    
        try{
        richiesta.setPeer(new Tuple(parti[1].trim(), Integer.parseInt(parti[2].trim()))); //setta il peer da contattare, il resto della stringa è la triplet
        }
        catch(Exception e){
            logger.severe("Errore: formato indirizzo peer non conforme " + e.getMessage());
            System.out.println("IP: " + parti[1]);
            System.out.println("Port: " + parti[2]);
        }
       //se esito positivo scarica file e aggiunge risorsa a lista
        String path = this.richiestaPeer(richiesta); 
       if(path.equals("NONDISPONIBILE")){ //se il peer non è disponibile, ripete la richiesta al master con specifiche peer provato
            //System.out.println("Peer non disponibile, rimuovo peer dalla lista e ripeto la richiesta");    
        DownLoad(richiesta);
       }
       else{ 
        //altrimenti aggiunge alla lista delle risorse/path
        this.aggiungiRisorsa(richiesta.getRisorsa(), path);
       }
    }


    public String queryMaster(Triplet richiesta){ //Richiesta al master per ottenere la lista dei peer con la risorsa richiesta

        try (
         Socket socket = new Socket(IPMaster, PortMaster);
         PrintWriter writer = new PrintWriter( socket.getOutputStream(), true);
         BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            writer.println(("QUERY,"+ richiesta.toString()).getBytes()); // Invia richiesta al master   (E' stato utilizzato questo perchè il master ha un BufferedReader che legge)      
            String risposta = reader.readLine();
            
            return risposta; 
        }
        
            catch (IOException e) {
            logger.severe("Errore durante la query al master: " + e.getMessage());
        }
        return null;
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
                
        /* 
        try (
        Socket socket = new Socket(richiesta.getPeer().getIP(), richiesta.getPeer().getPort());
        PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        InputStream inputStream = socket.getInputStream()
        ) {
        writer.println("UPLOAD," + richiesta.toString());

            

        String responseHeader = reader.readLine().trim();
        if (responseHeader.equals("NONDISPONIBILE")) {
            return "NONDISPONIBILE";
        } else if (!responseHeader.equals("SUCCESSO")) {
            System.out.println("Risposta non valida: " + responseHeader);
            return null;
        }
        */

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


    public void registratiAMaster() { //Registrazione al master, da implementare
        try 
            {Socket socket = new Socket(IPMaster, PortMaster);
            OutputStream outputStream = socket.getOutputStream();
            
            outputStream.write(("REGISTRAZIONE" + "\n").getBytes()); // Invia scopo del resto del messaggio
            outputStream.write((this.IP + "\n").getBytes()); // invia IP del peer
            outputStream.write((this.Port + "\n").getBytes()); // invia port del peer
                //per ottenere la tabella si potrebbe unire questa funzione a caricaRisorse ? tnato vengono eseguite alla dichiarazione del peer....


            socket.close();
        }
        catch (IOException e) {
            logger.severe("Errore durante la registrazione al master: " + e.getMessage());
        }
    }

    




}
