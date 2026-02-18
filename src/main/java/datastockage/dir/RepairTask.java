package dir;

/**
 * Représente une tâche de ré-réplication en attente.
 */
public class RepairTask {

    public final String   fileName;
    public final String   chunkId;
    public final int      currentReplicas;
    public final int      targetReplicas;
    public final long     createdAt;

    public RepairTask(String fileName, String chunkId,
                      int currentReplicas, int targetReplicas) {
        this.fileName        = fileName;
        this.chunkId         = chunkId;
        this.currentReplicas = currentReplicas;
        this.targetReplicas  = targetReplicas;
        this.createdAt       = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return "RepairTask{" + chunkId + " replicas=" + currentReplicas
                + "/" + targetReplicas + "}";
    }
}
