package dir;

/**
 * Informations réseau d’un OSD enregistré dans le cluster.
 */
public class OSDInfo {

    public final String host;
    public final int port;

    public OSDInfo(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public String toString() {
        return host + ":" + port;
    }
}
