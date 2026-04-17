package com.vanillage.raytraceantixray.data;

import com.vanillage.raytraceantixray.net.DuplexPacketHandler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

public final class PlayerData implements Callable<Object> {

    private final ConcurrentMap<LongWrapper, ChunkBlocks> chunks = new ConcurrentHashMap<>();
    private final Queue<Result> results = new ConcurrentLinkedQueue<>();
    private volatile Callable<?> callable;
    private volatile DuplexPacketHandler packetHandler;
    private volatile VectorialLocation[] locations;
    private volatile ScheduledTask updateTask;

    public PlayerData(VectorialLocation[] locations) {
        this.locations = locations;
    }

    public VectorialLocation[] getLocations() {
        return locations;
    }

    public void setLocations(VectorialLocation[] locations) {
        this.locations = locations;
    }

    public ConcurrentMap<LongWrapper, ChunkBlocks> getChunks() {
        return chunks;
    }

    public Queue<Result> getResults() {
        return results;
    }

    public Callable<?> getCallable() {
        return callable;
    }

    public void setCallable(Callable<?> callable) {
        this.callable = callable;
    }

    public DuplexPacketHandler getPacketHandler() {
        return packetHandler;
    }

    public void setPacketHandler(DuplexPacketHandler packetHandler) {
        this.packetHandler = packetHandler;
    }

    public ScheduledTask getUpdateTask() {
        return updateTask;
    }

    public void setUpdateTask(ScheduledTask updateTask) {
        this.updateTask = updateTask;
    }

    @Override
    public Object call() throws Exception {
        Callable<?> c = callable;
        if (c == null) {
            return null;
        }
        return c.call();
    }

}
