public class Triplet  { //Utilizzata per la gestione coda upload dei peer e per segnare a quale nodo è stata richiesta una risorsa (in modo fallimentare) 
                        //NOTA, TODO valuta se randerlo estensione di Tuple, per rendere più veloce recupero di ip e porta (altrimenti, come ora, si fa pipe .getPeer().getIP())
        private String Risorsa;
        private Tuple peer;
    
        public Triplet(String risorsa, Tuple peer) {
            this.Risorsa = risorsa;
            this.peer = peer;
        }
    
        public Tuple getPeer() {
            return this.peer;
        }
    
        public String getRisorsa() {
            return this.Risorsa;
        }
        public String toString() {
            if (peer == null) {
                return Risorsa;
            }
            return Risorsa + "," + peer.getIP() + "," + peer.getPort();
        }
        public void setRisorsa(String risorsa) {
            this.Risorsa = risorsa;
        }
    
        public void setPeer(Tuple peer) {
            this.peer = peer;
        }
            
}
