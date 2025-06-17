public class Tuple {
    private int Port;
    private String IP;
    private String id;  // Aggiungiamo un ID univoco

    public Tuple(String IP, int Port) {
        this.IP = IP;
        this.Port = Port;
        this.id = IP + ":" + Port;  // ID univoco basato su IP e porta  per identificare in maniera univoca il peer
    }

    public String getIP() {
        return IP;
    }

    public int getPort() {
        return Port;
    }

    public String getId() {
        return id;
    }

    public void setPort(int Port) {
        this.Port = Port;
    }

    public void setIP(String IP) {
        this.IP = IP;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tuple tuple = (Tuple) o;
        return id.equals(tuple.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}