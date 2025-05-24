import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        int porta = 9000;
        new Thread(new PeerListener(porta)).start();
    }
}