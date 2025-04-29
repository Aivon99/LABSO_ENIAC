import java.util.HashMap;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

public class Master {

    private HashMap<Tuple, List<String>> hashPeer; // IP+Port --> risorsa 
    private HashMap<String, List<Tuple> > hashRisorse; // risorsa --> lista di peer


    public Master() {
        this.hashRisorse = new HashMap<>();
        this.hashPeer = new HashMap<>();
    }    

    public void addPeer(String IP, int Port, List<String> risorse) {
        Tuple peer = new Tuple(IP, Port);
        
        hashPeer.put(peer, risorse);

        for(int i = 0; i < risorse.size(); i++){
        
            String risorsa = risorse.get(i);
            
            if(hashRisorse.containsKey(risorsa)) { //se sono già stati aggiunti nodi con la stessa risorsa, aggiungiamo il peer alla lista
                List<Tuple> peerList = this.hashRisorse.get(risorsa); 
                
                peerList.add(peer); //aggiungiamo il peer alla lista di peer associati alla risorsa
                hashRisorse.put(risorsa, peerList); //aggiorniamo la hasmap con la lista di peer aggiornati
            
            }

            else{ //altrimenti
                List<Tuple> peerList = new ArrayList<>();
                peerList.add(peer); //creiamo una nuova lista di peer e aggiungiamo il peer 
                this.hashRisorse.put(risorsa, peerList); //aggiorniamo la hasmap con la lista di peer aggiornati
            }
            
        
            }
        
        }
    public Tuple getPeerRisorsa(String risorsa){
        if(hashRisorse.containsKey(risorsa)) { //se la risorsa è presente nella hasmap
            List<Tuple> peerList = hashRisorse.get(risorsa);
            return peerList.get(0); //restituisci il primo peer della lista (o a caso in caso cambia)
        }
        else{ //se la risorsa non è presente nella hasmap (altrimenti si fa gestione errore ma non saprei se ha senso)
            System.out.println(risorsa + " non è presente nella hasmap"); 
            return null; //restituisci null 
        }
    }
    
    public void rimuoviPeer(Tuple peer){ 
        
        List<String> risorse = hashPeer.get(peer); 
        
        for (String  risorsa : risorse){ //rimuovi ogni istanza di peer dalla lista di peer associati alla risorsa
            if(hashRisorse.containsKey(risorsa)) { //se la risorsa è presente nella hasmap
                List<Tuple> peerList = hashRisorse.get(risorsa); 
                peerList.remove(peer); //rimuovi il peer dalla lista
                hashRisorse.put(risorsa, peerList); //aggiorna la hasmap con la lista di peer aggiornata
            }

        }
    }





    
    
}
