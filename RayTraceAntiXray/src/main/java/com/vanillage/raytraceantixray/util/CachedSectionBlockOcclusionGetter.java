package com.vanillage.raytraceantixray.util;

import com.vanillage.raytraceantixray.data.ChunkBlocks;
import com.vanillage.raytraceantixray.data.WorldContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import static com.vanillage.raytraceantixray.util.BlockStateUtil.*;

public class CachedSectionBlockOcclusionGetter implements BlockOcclusionGetter {

    private static final boolean UNLOADED_OCCLUDING = true;

    private final WorldContext worldContext;
    private final boolean[] occludingIds;

    private LevelChunk cachedChunk;
    private LevelChunkSection cachedSection;
    private int cachedChunkX, cachedSectionY, cachedChunkZ;
    private BlockState cachedState = AIR;
    private int cachedId = AIR_ID;
    private BlockState cachedState2 = cachedState;
    private int cachedId2 = cachedId;
    private BlockState cachedState3 = cachedState2;
    private int cachedId3 = cachedId2;

    public CachedSectionBlockOcclusionGetter(WorldContext worldContext, boolean[] occludingIds) {
        this.worldContext = worldContext;
        this.occludingIds = occludingIds;
        cachedChunkX = cachedSectionY = cachedChunkZ = Integer.MIN_VALUE;
    }

    /// caches a chunk and section (if not already cached) at the specified coordinates, returns false if missing (cachedChunk and or cachedSection will be null)
    public boolean cacheSection(int chunkX, int sectionY, int chunkZ) {
        if (cachedChunkX == chunkX && cachedSectionY == sectionY && cachedChunkZ == chunkZ) // chunk and section are the same
            return cachedSection != null;

        // if chunk has changed
        if (cachedChunkX != chunkX || cachedChunkZ != chunkZ) {
            cachedChunkX = chunkX;
            cachedChunkZ = chunkZ;
            cachedChunk = null;

            long packed = ChunkPos.pack(chunkX, chunkZ);
            ChunkBlocks chunkBlocks = worldContext.getChunk(packed);
            if (chunkBlocks != null)
                cachedChunk = chunkBlocks.getChunk();
        }

        // update the chunk section
        cachedSection = null;
        cachedSectionY = sectionY;

        if (cachedChunk == null)
            return false;

        int minSectionY = cachedChunk.getMinSectionY();
        if (sectionY < minSectionY || sectionY >= cachedChunk.getMaxSectionY())
            return false;

        cachedSection = cachedChunk.getSections()[sectionY - minSectionY];

        return cachedSection != null;
    }

    public int getIdFor(BlockState state) {
        // cached1 matches, return id
        if (cachedState == state)
            return cachedId;

        // cached2 matches, move to front
        if (cachedState2 == state) {
            int matchedId = cachedId2;

            // matchedId = cached2; cached2 = cached1; cached1 = matchedId
            cachedState2 = cachedState;
            cachedId2 = cachedId;
            cachedState = state;
            cachedId = matchedId;

            return matchedId;
        }

        // cached3 matches, move to front
        if (cachedState3 == state) {
            // matchedId = cached3; cached3 = cached2; cached2 = cached1; cached1 = matchedId
            int matchedId = cachedId3;
            cachedState3 = cachedState2;
            cachedId3 = cachedId2;
            cachedState2 = cachedState;
            cachedId2 = cachedId;
            cachedState = state;
            cachedId = matchedId;

            return matchedId;
        }

        // cache miss, shift upwards and lookup state to cached1
        // cached3 = cached2; cached2 = cached1; cached1 = Palette#idFor
        cachedState3 = cachedState2;
        cachedId3 = cachedId2;
        cachedState2 = cachedState;
        cachedId2 = cachedId;
        cachedState = state;
        cachedId = BLOCKSTATE_PALETTE.idFor(state);
        return cachedId;
    }

    @Override
    public boolean isOccluding(int x, int y, int z) {
        if (!cacheSection(x >> 4, y >> 4, z >> 4))
            return UNLOADED_OCCLUDING;

        // Unfortunately, LevelChunkSection#recalcBlockCounts() temporarily resets #nonEmptyBlockCount to 0 due to a Paper optimization.
        if (cachedSection.hasOnlyAir())
            return false;

        BlockState state = getBlockState(cachedSection, x, y, z);
        int stateId = getIdFor(state);

        return occludingIds[stateId];
    }

    @Override
    public boolean isOccludingRay(int x, int y, int z) {
        return isOccluding(x, y, z);
    }

    public void initializeCache(LevelChunk chunk, int chunkX, int sectionY, int chunkZ) {
        this.cachedChunk = chunk;
        this.cachedSection = chunk.getSections()[sectionY - chunk.getMinSectionY()];
        this.cachedChunkX = chunkX;
        this.cachedChunkZ = chunkZ;
        this.cachedSectionY = sectionY;
    }

    public void clearCache() {
        cachedChunk = null;
        cachedSection = null;
        cachedChunkX = cachedChunkZ = cachedSectionY = Integer.MIN_VALUE;
    }

}
