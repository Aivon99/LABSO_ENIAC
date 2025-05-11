import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.Map;
import java.io.IOException;
import java.net.ServerSocket;
import java.io.BufferedReader;
import java.io.FileReader;
import java.net.Socket;
import java.util.Queue; 



public class Peer {
    private HashMap<String, String> hashRisorse;
     
    private int Port;
    private String IP; //IP del peer
    private ServerSocket serverSocket; // socket del peer per ricevere comunicazioni dal master
    private BlockingQueue<Triplet> codaUpload; 
     

    public Peer(String IP, int Port) {

        this.hashRisorse = new HashMap<>();
         
        this.IP = IP;
        this.Port = Port;
        this.codaUpload = new LinkedBlockingQueue<>();
        
         
        /* 
         NON sono sicuro che rendere la porta persistente sia una buona idea, più che altro per possibili problemi ed implicazioni lato master.
        
        
        try {
            this.serverSocket = new ServerSocket(Port); // server socket del master
        } catch (Exception e) {
            this.serverSocket = null; // se non riesce a creare il server socket, lo setta a null
            System.out.println("Errore nella creazione del server socket: " + e.getMessage());
        }
         */

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
            System.out.println("Errore di I/O: " + e.getMessage());
            return false;
        }
        catch (Exception e){
            System.out.println("Errore: " + e.getMessage());
            return false;
        }   

        return true;
    }
    
    public void ascoltoPorta() {
    
    while (true) {
        try {
            Socket clientSocket = serverSocket.accept(); // blocks until a peer connects
            
            
        } catch (IOException e) {
            System.err.println("Error accepting connection: " + e.getMessage());
        }
    }
    }   
    
    public Triplet getProssimoInCoda(){
        return this.codaUpload.poll(); 
    }

    public void gestisciUploadCoda(){
        //inventati modo di mettere in standby questo metodo, direi o utilizzare un semaforo (non binario) che riceve 
        //signal da metodo di ascolto  o signal await (equivalente di Java).


    }

    public void UpLoad(String nomeRisorsa, String IPDestinatario, int Port){
        String path = this.hashRisorse.get(nomeRisorsa);
        //Fabbrica ed aggiungi metodo lettura  
        
        try{
        
            Socket collegamentoUpload = new Socket(IPDestinatario, Port) ;
            
            // Fabbrica ed aggiungi metodo upload 
            
            
            collegamentoUpload.close();
        }
        catch(Exception e){

        }

    }



    public void registratiAMaster(String IP, int Port) { //Registrazione al master, da implementare
        // TODO Auto-generated method stub
        
    }

    




}
