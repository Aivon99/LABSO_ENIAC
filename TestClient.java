import java.io.*;
import java.net.*;
import java.util.Scanner;

public class TestClient {
    public static void main(String[] args) {
        try (
                Socket socket = new Socket("127.0.0.1", 9000);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                Scanner scanner = new Scanner(System.in);
        ) {
            System.out.println("Connesso al master. Scrivi comandi:");

            while (true) {
                System.out.print("> ");
                String input = scanner.nextLine();
                out.write(input + "\n");
                out.flush();

                String response = in.readLine();
                if (response == null || response.equalsIgnoreCase("BYE")) {
                    System.out.println("Connessione chiusa dal server.");
                    break;
                }
                System.out.println("Risposta: " + response);

                if (input.equalsIgnoreCase("QUIT")) break;
            }

        } catch (IOException e) {
            System.err.println("Errore nel client: " + e.getMessage());
        }
    }
}
