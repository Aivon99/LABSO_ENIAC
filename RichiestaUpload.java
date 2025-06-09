import java.net.Socket;

public class RichiestaUpload {
    private Triplet richiesta;
    private Socket socketRif;
    
    public RichiestaUpload(Triplet richiesta, Socket socketRif) {
        this.richiesta = richiesta;
        this.socketRif = socketRif;
    }
    public Triplet getRichiesta() {
        return richiesta;
    }
    public Socket getSocketRif() {
        return socketRif;
    }

}
