package common;

import java.util.List;

public class ChunkInfo {
    public String chunkId;
    public List<SlaveInfo> replicas;

    public ChunkInfo(String chunkId, List<SlaveInfo> replicas) {
        this.chunkId = chunkId;
        this.replicas = replicas;
    }
}
