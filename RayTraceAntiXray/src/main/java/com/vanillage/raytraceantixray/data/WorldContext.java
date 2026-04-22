package com.vanillage.raytraceantixray.data;

import ca.spottedleaf.concurrentutil.map.concurrent.longs.ConcurrentChainedLong2ObjectHashTable;
import com.vanillage.raytraceantixray.RayTraceAntiXray;
import com.vanillage.raytraceantixray.antixray.ChunkPacketBlockControllerAntiXray;
import com.vanillage.raytraceantixray.tasks.RayTraceCallable;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;

import java.util.Arrays;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This class defines anti-xray information bound to a particular world.
 */
public class WorldContext {

    public static final VectorialLocation[] EMPTY_LOCATIONS = new VectorialLocation[0];
    private final ConcurrentChainedLong2ObjectHashTable<ChunkBlocks> chunks = new ConcurrentChainedLong2ObjectHashTable<>();
    private final Queue<Result> results = new ConcurrentLinkedQueue<>();
    private final World world;
    private final Callable<?> callable;
    private final AtomicReference<VectorialLocation[]> oldLocations = new AtomicReference<>(EMPTY_LOCATIONS);
    private final AtomicBoolean chunksDirty = new AtomicBoolean(true);
    private volatile VectorialLocation[] locations = EMPTY_LOCATIONS;

    public WorldContext(RayTraceAntiXray plugin, World world) {
        this.world = world;
        // the custom controller will be missing from worlds with RTAA disabled
        if (((CraftWorld) world).getHandle().chunkPacketBlockController instanceof ChunkPacketBlockControllerAntiXray controller) {
            this.callable = new RayTraceCallable(plugin, this, controller);
        } else {
            this.callable = () -> null;
        }
    }

    /// the world this context is bound to
    public World getWorld() {
        return world;
    }

    /// current player raytracing locations
    public VectorialLocation[] getLocations() {
        return locations;
    }

    /// sets the current player raytracing locations
    public void setLocations(VectorialLocation[] locations) {
        this.locations = locations;
    }

    /// true if locations have changed and need to be raytraced again
    public boolean areLocationsDirty() {
        return !Arrays.equals(oldLocations.get(), locations);
    }

    /// true if locations have changed
    public boolean setLocationsDirty(boolean dirty) {
        var locations = this.locations;
        var previous = oldLocations.getAndSet(dirty ? EMPTY_LOCATIONS : locations);
        return !Arrays.equals(previous, locations);
    }

    public Collection<ChunkBlocks> getChunks() {
        return chunks.values();
    }

    public ChunkBlocks getChunk(long key) {
        return chunks.get(key);
    }

    public void addChunk(ChunkBlocks blocks) {
        chunks.put(blocks.getKey(), blocks);
        setChunksDirty(true);
    }

    public ChunkBlocks removeChunk(long key) {
        return chunks.remove(key);
    }

    public void clearChunks() {
        chunks.clear();
    }

    /// true if one or more chunks need to be raytraced again
    public boolean areChunksDirty() {
        return chunksDirty.get();
    }

    /// returns the value before the swap
    public boolean setChunksDirty(boolean dirty) {
        return chunksDirty.getAndSet(dirty);
    }

    /// results of raytracing consumed by UpdateBukkitRunnable
    public Queue<Result> getResults() {
        return results;
    }

    /// the callable responsible for raytracing
    public Callable<?> getCallable() {
        return callable;
    }

}
