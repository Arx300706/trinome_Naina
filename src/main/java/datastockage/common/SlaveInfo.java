package common; 
public class SlaveInfo {
    public String id;
    public String host;
    public int port;

    public SlaveInfo(String id, String host, int port) {
        this.id = id;
        this.host = host;
        this.port = port;
    }
}
