package com.vanillage.raytraceantixray.util;

import org.bukkit.Bukkit;

public class BukkitUtil {

    public static final boolean IS_FOLIA = isFolia();

    private static boolean isFolia() {
        return classForName(Bukkit.getServer().getClass().getClassLoader(), "io.papermc.paper.threadedregions.RegionizedServer") != null;
    }

    private static Class<?> classForName(ClassLoader ldr, String name) {
        try {
            return ldr.loadClass(name);
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }

}
