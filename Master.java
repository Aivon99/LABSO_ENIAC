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

public class Master {

    private HashMap<Tuple, List<String>> hashPeer; // IP+Port --> risorsa 
    private HashMap<String, List<Tuple> > hashRisorse; // risorsa --> lista di peer
    private ServerSocket serverSocket; // socket del master per ricevere le richieste dai peer

    public Master(int Port) { // costruttore del master
        this.hashRisorse = new HashMap<>();
        this.hashPeer = new HashMap<>();
        try {
            this.serverSocket = new ServerSocket(Port); // server socket del master
        } catch (Exception e) {
            this.serverSocket = null; // se non riesce a creare il server socket, lo setta a null
            System.out.println("Errore nella creazione del server socket: " + e.getMessage());
        }

        
    }    

       public void addPeer(String IP, int Port, List<String> risorse) {
        Tuple peer = new Tuple(IP, Port);
        hashPeer.put(peer, risorse);

        for (String risorsa : risorse) {
            if (hashRisorse.containsKey(risorsa)) {
                List<Tuple> peerList = hashRisorse.get(risorsa);
                peerList.add(peer);
                hashRisorse.put(risorsa, peerList);
            } else {
                List<Tuple> peerList = new ArrayList<>();

                peerList.add(peer);
                hashRisorse.put(risorsa, peerList);
            }
        }
      }

    public Tuple getPeerRisorsa(String risorsa) {
        if (hashRisorse.containsKey(risorsa)) {
            List<Tuple> peerList = hashRisorse.get(risorsa);

            return peerList.get(0); //restituisci il primo peer della lista (o a caso in caso cambia)
        }
        else{ //se la risorsa non è presente nella hasmap (altrimenti si fa gestione errore ma non saprei se ha senso)
            System.out.println(risorsa + " non è presente nella hasmap"); 
            return null; //restituisci null 
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

    
    public void modificaPeer(Tuple peer, List<String> nuoveRisorse){ // per ora faccio così perchè più comodo se si vuole si fa differenza tra attuali risorse e nuove risorse ecc.
        rimuoviPeer(peer); //rimuovi il peer dalla hasmap
        addPeer(peer.getIP(), peer.getPort(), nuoveRisorse); //aggiungi il peer con le nuove risorse

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

        System.out.println("Peers e relative risorse:");
        for (Tuple peer : hashPeer.keySet()) {
            System.out.println("Peer " + peer.getIP() + ":" + peer.getPort());
            List<String> risorse = hashPeer.get(peer);
            for (String r : risorse) {
                System.out.println("  - " + r);
            }
        }
    }

    public void ascoltaPorta() {
    ExecutorService threadPool = Executors.newCachedThreadPool(); // or fixed thread pool

    while (true) {
        try {
            Socket clientSocket = serverSocket.accept(); // blocks until a peer connects
            threadPool.execute(() -> handleClient(clientSocket));
        } catch (IOException e) {
            System.err.println("Error accepting connection: " + e.getMessage());
        }
    }
    }   

    private void handleClient(Socket socket) {
        
    }



    
    


    public void mandaMessaggio(Tuple peer){
        int port = peer.getPort(); 
        String IP = peer.getIP(); 

        try{
        ServerSocket serverSocket = new ServerSocket(port);
        Socket clientSocket = serverSocket.accept(); // wait for client
        
        }
        
        catch (Exception e){
            System.out.println("Errore nella creazione del server socket: " + e.getMessage());
        }

    }


    
    
}

    

  

    

