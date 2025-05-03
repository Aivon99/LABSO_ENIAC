import java.util.HashMap;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class Master {

    private HashMap<Tuple, List<String>> hashPeer; // IP+Port --> risorse
    private HashMap<String, List<Tuple>> hashRisorse; // risorsa --> lista di peer

    public Master() {
        this.hashRisorse = new HashMap<>();
        this.hashPeer = new HashMap<>();
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
            if (!peerList.isEmpty()) {
                return peerList.get(0); // puoi fare anche random, se vuoi bilanciare il carico
            }
        }
        System.out.println("La risorsa " + risorsa + " non è disponibile.");
        return null;
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

    private List<String> downloadLog = new ArrayList<>();

    public void registraDownload(String risorsa, Tuple da, Tuple a) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String record = timestamp + " - " + risorsa + " da: " + da.getIP() + ":" + da.getPort() +
                " a: " + a.getIP() + ":" + a.getPort();
        downloadLog.add(record);
        System.out.println("LOG: " + record); // stampa anche live, utile per debug
    }

    public void stampaLog() {
        System.out.println("Log dei download:");
        if (downloadLog.isEmpty()) {
            System.out.println("Nessun download registrato.");
        } else {
            for (String entry : downloadLog) {
                System.out.println(entry);
            }
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


}
