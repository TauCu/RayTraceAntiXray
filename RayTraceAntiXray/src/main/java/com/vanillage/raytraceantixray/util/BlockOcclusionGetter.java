package com.vanillage.raytraceantixray.util;

public interface BlockOcclusionGetter {

    boolean isOccluding(int x, int y, int z);

    default boolean isOccludingRay(int x, int y, int z) {
        return isOccluding(x, y, z);
    }

    default boolean isOccludingNearby(int x, int y, int z) {
        return isOccluding(x, y, z);
    }

}
