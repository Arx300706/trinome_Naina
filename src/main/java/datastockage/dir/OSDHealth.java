package dir;

/**
 * Représente l'état de santé d'un OSD à un instant T.
 */
public class OSDHealth {

    public enum Status { UP, SUSPECTED, DOWN }

    public final String osdId;
    public volatile Status status = Status.DOWN; // DOWN jusqu'au premier heartbeat
    public volatile long   lastHeartbeat = -1L;  // -1 = jamais reçu
    public volatile int    missedBeats   = 0;
    public volatile int    chunkCount    = 0;

    static final long SUSPECT_AFTER_MS = 8_000L;
    static final long DOWN_AFTER_MS    = 15_000L;

    public OSDHealth(String osdId) {
        this.osdId = osdId;
    }

    public void beat() {
        this.lastHeartbeat = System.currentTimeMillis();
        this.missedBeats   = 0;
        this.status        = Status.UP;
    }

    public void evaluate() {
        // Jamais reçu de heartbeat → DOWN, pas besoin de calculer
        if (lastHeartbeat < 0) {
            status = Status.DOWN;
            return;
        }
        long silence = System.currentTimeMillis() - lastHeartbeat;
        if      (silence >= DOWN_AFTER_MS)    status = Status.DOWN;
        else if (silence >= SUSPECT_AFTER_MS) status = Status.SUSPECTED;
        else                                   status = Status.UP;
    }

    public boolean isAlive() {
        return status == Status.UP || status == Status.SUSPECTED;
    }

    /** Silence en secondes, -1 si jamais contacté */
    public long silenceSeconds() {
        if (lastHeartbeat < 0) return -1;
        return (System.currentTimeMillis() - lastHeartbeat) / 1000;
    }

    @Override
    public String toString() {
        return osdId + "[" + status + " silence=" + silenceSeconds() + "s]";
    }
}
