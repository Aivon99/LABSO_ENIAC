import java.net.Socket;
import java.io.*;
import java.util.*;

public class MasterHandler implements Runnable {
    private Socket socket;
    private Master master;

    public MasterHandler(Socket socket, Master master) {
        this.socket = socket;
        this.master = master;
    }

    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        ) {
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("Ricevuto: " + line);

                // Semplice protocollo testuale: REGISTER, GET, ADD, QUIT
                String[] parts = line.split(" ");

                switch (parts[0]) {
                    case "REGISTER": {
                        // es: REGISTER 127.0.0.1 5000 R0,R1,R2
                        String ip = parts[1];
                        int port = Integer.parseInt(parts[2]);
                        List<String> risorse = Arrays.asList(parts[3].split(","));
                        synchronized (master) {
                            master.addPeer(ip, port, risorse);
                        }
                        out.write("OK Registrato\n");
                        out.flush();
                        break;
                    }
                    case "GET": {
                        // es: GET R1
                        String risorsa = parts[1];
                        Tuple peer;
                        synchronized (master) {
                            peer = master.getPeerRisorsa(risorsa);
                        }
                        if (peer != null) {
                            out.write("FOUND " + peer.getIP() + " " + peer.getPort() + "\n");
                        } else {
                            out.write("NOTFOUND\n");
                        }
                        out.flush();
                        break;
                    }
                    case "REMOVE": {
                        // es: REMOVE 127.0.0.1 5000
                        String ip = parts[1];
                        int port = Integer.parseInt(parts[2]);
                        Tuple peer = new Tuple(ip, port);
                        synchronized (master) {
                            master.rimuoviPeer(peer);
                        }
                        out.write("OK Rimosso\n");
                        out.flush();
                        break;
                    }
                    case "QUIT": {
                        out.write("BYE\n");
                        out.flush();
                        socket.close();
                        return;
                    }
                    default:
                        out.write("Comando non riconosciuto\n");
                        out.flush();
                        break;
                }
            }
        } catch (IOException e) {
            System.err.println("Errore nella connessione: " + e.getMessage());
        }
    }
}
