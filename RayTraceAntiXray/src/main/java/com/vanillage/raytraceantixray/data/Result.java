package com.vanillage.raytraceantixray.data;

import net.minecraft.core.BlockPos;

public record Result(ChunkBlocks chunkBlocks, BlockPos block, boolean visible) {}
