public class Triplet { //Utilizzata per la gestione coda upload dei peer e per segnare a quale nodo è stata richiesta una risorsa (in modo fallimentare) 

        private String Risorsa;
        private Tuple peer;
    
        public Triplet(String risorsa, Tuple peer) {
            this.Risorsa = risorsa;
            this.peer = peer;
        }
    
        public String getPeer() {
            return this.peer;
        }
    
        public int getRisorsa() {
            return this.risorsa;
        }
    
        public void setRisorsa(String risorsa) {
            this.Risorsa = risorsa;
        }
    
        public void setIP(String IP) {
            this.IP = IP;
        }
    
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Tuple other = (Tuple) obj;
            return Port == other.Port && IP.equals(other.IP);
        }
    
        @Override
        public int hashCode() {
            return IP.hashCode() * 31 + Port;
        }
    
    


}
