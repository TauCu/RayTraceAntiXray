package com.vanillage.raytraceantixray.util;

import net.minecraft.core.IdMapper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.MissingPaletteEntryException;

public class BlockStateUtil {

    public static final IdMapper<BlockState> BLOCKSTATE_MAP = Block.BLOCK_STATE_REGISTRY;
    public static final MyGlobalPalette<BlockState> BLOCKSTATE_PALETTE = new MyGlobalPalette<>(BLOCKSTATE_MAP);
    public static final BlockState AIR = Blocks.AIR.defaultBlockState();
    public static final int AIR_ID = BLOCKSTATE_PALETTE.idFor(AIR);

    public static BlockState getBlockState(LevelChunkSection section, int x, int y, int z) {
        try {
            return section.getBlockState(x & 15, y & 15, z & 15);
        } catch (MissingPaletteEntryException e) {
            return AIR;
        }
    }

}
