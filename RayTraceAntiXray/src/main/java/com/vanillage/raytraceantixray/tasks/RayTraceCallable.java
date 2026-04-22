package com.vanillage.raytraceantixray.tasks;

import com.vanillage.raytraceantixray.RayTraceAntiXray;
import com.vanillage.raytraceantixray.antixray.ChunkPacketBlockControllerAntiXray;
import com.vanillage.raytraceantixray.data.ChunkBlocks;
import com.vanillage.raytraceantixray.data.Result;
import com.vanillage.raytraceantixray.data.VectorialLocation;
import com.vanillage.raytraceantixray.data.WorldContext;
import com.vanillage.raytraceantixray.util.BlockIterator;
import com.vanillage.raytraceantixray.util.BlockOcclusionCulling;
import com.vanillage.raytraceantixray.util.BlockStateUtil;
import com.vanillage.raytraceantixray.util.CachedSectionBlockOcclusionGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.logging.Level;

public class RayTraceCallable implements Callable<Void> {
    private final RayTraceAntiXray plugin;
    private final WorldContext worldContext;
    private final Collection<ChunkBlocks> chunks;
    private final double rayTraceDistance;
    private final double rayTraceDistanceSquared;
    private final boolean rehideBlocks;
    private final double rehideDistanceSquared;
    private final Set<Block> bypassRehideBlocks;
    private final CachedSectionBlockOcclusionGetter cachedSectionBlockOcclusionGetter;
    private final BlockOcclusionCulling blockOcclusionCulling;

    public RayTraceCallable(RayTraceAntiXray plugin, WorldContext worldContext, ChunkPacketBlockControllerAntiXray chunkPacketBlockControllerAntiXray) {
        this.plugin = plugin;
        this.worldContext = worldContext;
        this.chunks = worldContext.getChunks();

        rayTraceDistance = chunkPacketBlockControllerAntiXray.rayTraceDistance;
        rayTraceDistanceSquared = rayTraceDistance * rayTraceDistance;
        rehideBlocks = chunkPacketBlockControllerAntiXray.rehideBlocks;
        double rehideDistance = chunkPacketBlockControllerAntiXray.rehideDistance;
        rehideDistanceSquared = rehideDistance * rehideDistance;
        bypassRehideBlocks = chunkPacketBlockControllerAntiXray.bypassRehideBlocks;

        cachedSectionBlockOcclusionGetter = new CachedSectionBlockOcclusionGetter(worldContext, chunkPacketBlockControllerAntiXray.solidGlobal);
        blockOcclusionCulling = new BlockOcclusionCulling(new BlockIterator(0., 0., 0., 0., 0., 0.)::initializeNormalized, cachedSectionBlockOcclusionGetter, true);
    }

    @Override
    public Void call() {
        boolean locationsDirty = worldContext.setLocationsDirty(false);
        // possible that locations have changed since the above flag, but it doesn't matter
        VectorialLocation[] locations = worldContext.getLocations();
        if (locations.length == 0)
            return null; // shouldn't happen, but just in case

        boolean chunksDirty = worldContext.setChunksDirty(false);
        // only raytrace if locations and or chunks are dirty
        if (!locationsDirty && !chunksDirty)
            return null;

        try {
            rayTrace(locations, locationsDirty);
        } catch (Throwable t) {
            plugin.getLogger().log(Level.SEVERE, "An error occurred on the RayTraceAntiXray tick thread", t);
        }

        return null;
    }

    private void rayTrace(VectorialLocation[] locations, boolean forceRaytrace) {
        Vector playerVector = locations[0].position();
        double playerX = playerVector.getX();
        double playerY = playerVector.getY();
        double playerZ = playerVector.getZ();
        playerVector.setX(playerX - rayTraceDistance);
        playerVector.setZ(playerZ - rayTraceDistance);
        int chunkXMin = playerVector.getBlockX() >> 4;
        int chunkZMin = playerVector.getBlockZ() >> 4;
        playerVector.setX(playerX + rayTraceDistance);
        playerVector.setZ(playerZ + rayTraceDistance);
        int chunkXMax = playerVector.getBlockX() >> 4;
        int chunkZMax = playerVector.getBlockZ() >> 4;
        playerVector.setX(playerX);
        playerVector.setZ(playerZ);
        Queue<Result> results = worldContext.getResults();

        for (ChunkBlocks chunkBlocks : this.chunks) {
            // only raytrace this chunk if locations have changed or if the chunk itself is dirty (not yet traced)
            if (!chunkBlocks.setDirty(false) && !forceRaytrace)
                continue; // nothing is dirty here

            LevelChunk chunk = chunkBlocks.getChunk();
            ChunkPos chunkPos = chunk.getPos();

            int chunkX = chunkPos.x();
            if (chunkX < chunkXMin || chunkX > chunkXMax) {
                continue;
            }

            int chunkZ = chunkPos.z();
            if (chunkZ < chunkZMin || chunkZ > chunkZMax) {
                continue;
            }

            var iterator = chunkBlocks.getBlocks().object2BooleanEntrySet().iterator();
            while (iterator.hasNext()) {
                var blockHidden = iterator.next();
                BlockPos block = blockHidden.getKey();
                boolean hidden = blockHidden.getBooleanValue();

                int x = block.getX();
                int y = block.getY();
                int z = block.getZ();
                double centerX = x + 0.5;
                double centerY = y + 0.5;
                double centerZ = z + 0.5;
                double differenceX = playerX - centerX;
                double differenceY = playerY - centerY;
                double differenceZ = playerZ - centerZ;
                double distanceSquared = differenceX * differenceX + differenceY * differenceY + differenceZ * differenceZ;

                if (!(distanceSquared <= rayTraceDistanceSquared)) {
                    continue;
                }

                boolean visible = false;

                if (distanceSquared < rehideDistanceSquared) {
                    int sectionY = y >> 4;

                    for (int i = 0; i < locations.length; i++) {
                        VectorialLocation location = locations[i];
                        Vector direction = location.direction();
                        double directionX = direction.getX();
                        double directionY = direction.getY();
                        double directionZ = direction.getZ();
                        cachedSectionBlockOcclusionGetter.initializeCache(chunk, chunkX, sectionY, chunkZ);

                        if (i == 0) {
                            if (blockOcclusionCulling.isVisible(x, y, z, centerX, centerY, centerZ, differenceX, differenceY, differenceZ, distanceSquared, directionX, directionY, directionZ)) {
                                visible = true;
                                break;
                            }
                        } else {
                            Vector vector = location.position();
                            double vectorDifferenceX = vector.getX() - centerX;
                            double vectorDifferenceY = vector.getY() - centerY;
                            double vectorDifferenceZ = vector.getZ() - centerZ;

                            if (blockOcclusionCulling.isVisible(x, y, z, centerX, centerY, centerZ, vectorDifferenceX, vectorDifferenceY, vectorDifferenceZ, vectorDifferenceX * vectorDifferenceX + vectorDifferenceY * vectorDifferenceY + vectorDifferenceZ * vectorDifferenceZ, directionX, directionY, directionZ)) {
                                visible = true;
                                break;
                            }
                        }
                    }
                }

                if (visible) {
                    if (hidden) {
                        results.add(new Result(chunkBlocks, block, true));

                        if (rehideBlocks) {
                            boolean bypass = false;

                            if (bypassRehideBlocks != null) {
                                LevelChunkSection section = chunk.getSections()[(y >> 4) - chunk.getMinSectionY()];

                                if (section != null && !section.hasOnlyAir()
                                        && bypassRehideBlocks.contains(BlockStateUtil.getBlockState(section, x, y, z).getBlock())) {
                                    bypass = true;
                                }
                            }

                            if (bypass) {
                                iterator.remove();
                            } else {
                                blockHidden.setValue(false);
                            }
                        } else {
                            iterator.remove();
                        }
                    }
                } else if (!hidden) {
                    results.add(new Result(chunkBlocks, block, false));
                    blockHidden.setValue(true);
                }
            }
        }

        cachedSectionBlockOcclusionGetter.clearCache();
    }

}
