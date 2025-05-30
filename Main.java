import java.io.IOException;
import java.net.ServerSocket;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("Uso per Master: java Main master <porta>");
            System.out.println("Uso per Peer: java Main peer <IP_Master> <Porta_Master>");
            return;
        }

        if (args[0].equals("master")) {
            if (args.length != 2) {
                System.out.println("Uso per Master: java Main master <porta>");
                return;
            }
            int porta = Integer.parseInt(args[1]);
            Master master = new Master(porta);
            master.startInteractiveSession();
            master.ascoltaPorta();
        } 
        else if (args[0].equals("peer")) {
            if (args.length != 3) {
                System.out.println("Uso per Peer: java Main peer <IP_Master> <Porta_Master>");
                return;
            }
            String masterIP = args[1];
            int masterPort = Integer.parseInt(args[2]);
            
            // Trova una porta disponibile
            int peerPort = findAvailablePort(5000);
            System.out.println("Avvio peer sulla porta: " + peerPort);
            
            // Crea e avvia il peer
            Peer peer = new Peer("127.0.0.1", peerPort, masterIP, masterPort);
            
            // Registra il peer al master
            peer.registratiAMaster();
            
            // Avvia la sessione interattiva
            peer.startInteractiveSession();
            
            // Avvia il listener per le connessioni in entrata
            new Thread(peer::ascoltoPorta).start();
            
            // Avvia il gestore della coda upload
            new Thread(peer::gestisciUploadCoda).start();
        }
        else {
            System.out.println("Modalità non riconosciuta. Usa 'master' o 'peer'");
        }
    }

    private static int findAvailablePort(int startPort) {
        int port = startPort;
        while (port < 65535) {
            try (ServerSocket socket = new ServerSocket(port)) {
                return port;
            } catch (IOException e) {
                port++;
            }
        }
        throw new RuntimeException("Nessuna porta disponibile trovata");
    }
}