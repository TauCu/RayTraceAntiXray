package com.vanillage.raytraceantixray.util;

import org.bukkit.Bukkit;

import java.io.PrintWriter;
import java.io.StringWriter;

public class BukkitUtil {

    public static final boolean IS_FOLIA = isFolia();

    public static String stacktraceToString(Throwable msg) {
        var sw = new StringWriter();
        try (var pw = new PrintWriter(sw)) {
            msg.printStackTrace(pw);
        }
        return sw.toString();
    }

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
