public class Tuple {
    private int Port;
    private String IP;

    public Tuple(String IP, int Port) {
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
