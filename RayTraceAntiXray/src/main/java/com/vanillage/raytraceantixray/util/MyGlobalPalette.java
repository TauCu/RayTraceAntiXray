package com.vanillage.raytraceantixray.util;

import net.minecraft.core.IdMapper;
import net.minecraft.world.level.chunk.GlobalPalette;
import net.minecraft.world.level.chunk.PaletteResize;

public class MyGlobalPalette<T> extends GlobalPalette<T> {

    private final PaletteResize<T> resizeHandler = PaletteResize.noResizeExpected();

    public MyGlobalPalette(IdMapper<T> mapper) {
        super(mapper);
    }

    public int idFor(T value) {
        return idFor(value, resizeHandler);
    }

}
