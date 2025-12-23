package com.vanillage.raytraceantixray.listeners;

import com.vanillage.raytraceantixray.RayTraceAntiXray;
import com.vanillage.raytraceantixray.antixray.ChunkPacketBlockControllerAntiXray;
import io.papermc.paper.antixray.ChunkPacketBlockController;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class WorldListener implements Listener {
    private final RayTraceAntiXray plugin;

    public WorldListener(RayTraceAntiXray plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onWorldInit(WorldInitEvent event) {
        handleLoad(plugin, event.getWorld());
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent e) {
        handleUnload(plugin, e.getWorld());
    }

    public static void handleLoad(RayTraceAntiXray plugin, World world) {
        if (plugin.isEnabled(world)) {
            FileConfiguration config = plugin.getConfig();
            String worldName = world.getName();
            boolean rayTraceThirdPerson = config.getBoolean(
                    "world-settings." + worldName + ".anti-xray.ray-trace-third-person",
                    config.getBoolean("world-settings.default.anti-xray.ray-trace-third-person"));
            double rayTraceDistance = Math
                    .max(config.getDouble("world-settings." + worldName + ".anti-xray.ray-trace-distance",
                            config.getDouble("world-settings.default.anti-xray.ray-trace-distance")), 0.);
            boolean rehideBlocks = config.getBoolean("world-settings." + worldName + ".anti-xray.rehide-blocks",
                    config.getBoolean("world-settings.default.anti-xray.rehide-blocks"));
            double rehideDistance = Math
                    .max(config.getDouble("world-settings." + worldName + ".anti-xray.rehide-distance",
                            config.getDouble("world-settings.default.anti-xray.rehide-distance")), 0.);
            int maxRayTraceBlockCountPerChunk = Math
                    .max(config.getInt("world-settings." + worldName + ".anti-xray.max-ray-trace-block-count-per-chunk",
                            config.getInt("world-settings.default.anti-xray.max-ray-trace-block-count-per-chunk")), 0);
            List<String> rayTraceBlocks = config
                    .getList("world-settings." + worldName + ".anti-xray.ray-trace-blocks",
                            config.getList("world-settings.default.anti-xray.ray-trace-blocks"))
                    .stream().filter(Objects::nonNull).map(String::valueOf).collect(Collectors.toList());
            List<String> bypassRehideBlocks = config
                    .getList("world-settings." + worldName + ".anti-xray.bypass-rehide-blocks",
                            config.getList("world-settings.default.anti-xray.bypass-rehide-blocks"))
                    .stream().filter(Objects::nonNull).map(String::valueOf).collect(Collectors.toList());
            ServerLevel serverLevel = ((CraftWorld) world).getHandle();
            ChunkPacketBlockControllerAntiXray controller = new ChunkPacketBlockControllerAntiXray(plugin,
                    ((CraftWorld) world).getHandle().chunkPacketBlockController, rayTraceThirdPerson, rayTraceDistance,
                    rehideBlocks, rehideDistance, maxRayTraceBlockCountPerChunk,
                    rayTraceBlocks.isEmpty() ? null : rayTraceBlocks,
                    bypassRehideBlocks.isEmpty() ? null : bypassRehideBlocks, serverLevel,
                    MinecraftServer.getServer().executor);

            try {
                Field field = Level.class.getDeclaredField("chunkPacketBlockController");
                field.setAccessible(true);
                field.set(serverLevel, controller);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void handleUnload(RayTraceAntiXray plugin, World w) {
        if (((CraftWorld) w).getHandle().chunkPacketBlockController instanceof ChunkPacketBlockControllerAntiXray) {
            ChunkPacketBlockController oldController = ((ChunkPacketBlockControllerAntiXray) ((CraftWorld) w)
                    .getHandle().chunkPacketBlockController).getOldController();

            try {
                Field field = Level.class.getDeclaredField("chunkPacketBlockController");
                field.setAccessible(true);
                field.set(((CraftWorld) w).getHandle(), oldController);
            } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }
}
