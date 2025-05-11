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

    
}
