import java.util.HashMap;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

public class Master {

    private HashMap<Tuple, String> hashPeer; //has per memorizzare i peer e le loro risorse; utilizziamo IP+Port (per sicurezza) come chiave 
    private HashMap<String, List<Tuple> > hashRisorse; //has per memorizzare le risorse e i loro peer; 


    public Master() {
        this.hashRisorse = new HashMap<>();
        this.hashPeer = new HashMap<>();
    }    

    public void addPeer(String IP, int Port, List<String> risorse) {
        Tuple peer = new Tuple(IP, Port);
        for(int i = 0; i < risorse.size(); i++){
        
            String risorsa = risorse.get(i);
            
            hashPeer.put(peer, risorsa); //da fare sempre 
 
            if(hashRisorse.containsKey(risorsa)) { //se sono già stati aggiunti nodi con la stessa risorsa, aggiungiamo il peer alla lista
                List<Tuple> peerList = hashRisorse.get(risorsa); 
                this.peerList.add(peer); //aggiungiamo il peer alla lista di peer associati alla risorsa
                hashRisorse.put(risorsa, peerList); //aggiorniamo la hasmap con la lista di peer aggiornati
            
            }


            else{ //altrimenti
                List<Tuple> peerList = new ArrayList<>();
                peerList.add(peer); //creiamo una nuova lista di peer e aggiungiamo il peer 
                this.hashRisorse.put(risorsa, peerList); //aggiorniamo la hasmap con la lista di peer aggiornati
            }
            
        
            }
        
        }
        }








    
    
}
