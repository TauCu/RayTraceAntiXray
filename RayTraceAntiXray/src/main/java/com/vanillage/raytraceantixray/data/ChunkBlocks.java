package com.vanillage.raytraceantixray.data;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChunkBlocks {

    private final LevelChunk chunk;
    private final ConcurrentMap<BlockPos, Boolean> blocks;
    private final AtomicBoolean dirty;

    public ChunkBlocks(LevelChunk chunk, Map<BlockPos, Boolean> blocks, boolean dirty) {
        this.chunk = Objects.requireNonNull(chunk);
        this.blocks = new ConcurrentHashMap<>(blocks);
        this.dirty = new AtomicBoolean(dirty);
    }

    public ChunkBlocks(LevelChunk chunk, Map<BlockPos, Boolean> blocks) {
        this(chunk, blocks, true);
    }

    public LevelChunk getChunk() {
        return chunk;
    }

    public long getKey() {
        return chunk.getPos().longKey();
    }

    public ConcurrentMap<BlockPos, Boolean> getBlocks() {
        return blocks;
    }

    /// returns the dirty flag for this object
    public boolean isDirty() {
        return dirty.get();
    }

    /// sets the dirty flag of this object and returns the original value
    public boolean setDirty(boolean dirty) {
        return this.dirty.getAndSet(dirty);
    }

    /// copies data without transient values such as the dirty value
    public ChunkBlocks copyWithDataOnly() {
        return new ChunkBlocks(chunk, blocks);
    }

}
