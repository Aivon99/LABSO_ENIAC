import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Set;
import java.util.Map;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;





public class Peer {
    private HashMap<String, String> hashRisorse;
    String listaRisorse; 
    int Port;
    String IP; //IP del peer

    public Peer(String IP, int Port, String listaRisorse) {
        this.hashRisorse = new HashMap<>();
        this.listaRisorse = listaRisorse;  
        this.IP = IP;
        this.Port = Port;   
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
    public Set<String> getNomeRisorsa() {
        return hashRisorse.keySet();
    }
    public Map<String, String> getTutteRisorse() {
        return hashRisorse;
    }
    public boolean caricaListaRisorse(){ //Carica la lista risorse da un file di testo, alla hasmap nel costruttore  
        try{
        BufferedReader reader = new BufferedReader(new FileReader(this.listaRisorse));    
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
    public void registratiaMaster(String IP, int Port) { //Registrazione al master, da implementare
        // TODO Auto-generated method stub
        
    }






}
