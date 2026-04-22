package com.vanillage.raytraceantixray.data;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChunkBlocks {

    private final LevelChunk chunk;
    private final Object2BooleanMap<BlockPos> blocks;
    private final AtomicBoolean dirty;

    public ChunkBlocks(LevelChunk chunk, Object2BooleanMap<BlockPos> blocks, boolean dirty) {
        this.chunk = Objects.requireNonNull(chunk);
        this.blocks = Objects.requireNonNull(blocks);
        this.dirty = new AtomicBoolean(dirty);
    }

    public ChunkBlocks(LevelChunk chunk, Object2BooleanMap<BlockPos> blocks) {
        this(chunk, blocks, true);
    }

    public LevelChunk getChunk() {
        return chunk;
    }

    public long getKey() {
        return chunk.getPos().longKey();
    }

    /// only modify if working on a local copy for thread safety
    public Object2BooleanMap<BlockPos> getBlocks() {
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
        return new ChunkBlocks(chunk, new Object2BooleanOpenHashMap<>(blocks));
    }

}
