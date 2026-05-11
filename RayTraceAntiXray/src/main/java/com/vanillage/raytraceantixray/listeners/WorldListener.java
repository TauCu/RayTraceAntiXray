package com.vanillage.raytraceantixray.listeners;

import com.vanillage.raytraceantixray.RayTraceAntiXray;
import com.vanillage.raytraceantixray.antixray.ChunkPacketBlockControllerAntiXray;
import com.vanillage.raytraceantixray.util.BukkitUtil;
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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class WorldListener implements Listener {
    /**
     * VarHandle for {@code Level.chunkPacketBlockController}, used for {@link VarHandle#getAcquire}
     * reads. A VarHandle obtained for a {@code final} field only supports read access modes, so
     * writes go through {@link #CHUNK_PACKET_BLOCK_CONTROLLER_FIELD} below.
     */
    private static final VarHandle CHUNK_PACKET_BLOCK_CONTROLLER_HANDLE;
    /**
     * Reflective handle on {@code Level.chunkPacketBlockController} used for publishing writes.
     * Paired with a preceding {@link VarHandle#releaseFence()} so that any thread observing the
     * published reference through {@link VarHandle#getAcquire} also observes the fully-constructed
     * controller's stores — the same release/acquire happens-before relationship that
     * {@code setRelease} would have provided, which is unavailable for a {@code final} field.
     */
    private static final Field CHUNK_PACKET_BLOCK_CONTROLLER_FIELD;

    static {
        try {
            Field field = Level.class.getDeclaredField("chunkPacketBlockController");
            field.setAccessible(true);
            CHUNK_PACKET_BLOCK_CONTROLLER_FIELD = field;
            CHUNK_PACKET_BLOCK_CONTROLLER_HANDLE =
                    MethodHandles.privateLookupIn(Level.class, MethodHandles.lookup())
                            .unreflectVarHandle(field);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

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
            boolean rayTraceThirdPerson = config.getBoolean("world-settings." + worldName + ".anti-xray.ray-trace-third-person", config.getBoolean("world-settings.default.anti-xray.ray-trace-third-person"));
            double rayTraceDistance = Math.max(config.getDouble("world-settings." + worldName + ".anti-xray.ray-trace-distance", config.getDouble("world-settings.default.anti-xray.ray-trace-distance")), 0.);
            boolean rehideBlocks = config.getBoolean("world-settings." + worldName + ".anti-xray.rehide-blocks", config.getBoolean("world-settings.default.anti-xray.rehide-blocks"));
            double rehideDistance = Math.max(config.getDouble("world-settings." + worldName + ".anti-xray.rehide-distance", config.getDouble("world-settings.default.anti-xray.rehide-distance")), 0.);
            int maxRayTraceBlockCountPerChunk = Math.max(config.getInt("world-settings." + worldName + ".anti-xray.max-ray-trace-block-count-per-chunk", config.getInt("world-settings.default.anti-xray.max-ray-trace-block-count-per-chunk")), 0);
            List<String> rayTraceBlocks = config.getList("world-settings." + worldName + ".anti-xray.ray-trace-blocks", config.getList("world-settings.default.anti-xray.ray-trace-blocks")).stream().filter(Objects::nonNull).map(String::valueOf).collect(Collectors.toList());
            List<String> bypassRehideBlocks = config
                    .getList("world-settings." + worldName + ".anti-xray.bypass-rehide-blocks",
                            config.getList("world-settings.default.anti-xray.bypass-rehide-blocks"))
                    .stream().filter(Objects::nonNull).map(String::valueOf).collect(Collectors.toList());
            ServerLevel serverLevel = ((CraftWorld) world).getHandle();
            ChunkPacketBlockControllerAntiXray controller = new ChunkPacketBlockControllerAntiXray(
                    plugin,
                    (ChunkPacketBlockController) CHUNK_PACKET_BLOCK_CONTROLLER_HANDLE.getAcquire(serverLevel),
                    rayTraceThirdPerson,
                    rayTraceDistance,
                    rehideBlocks,
                    rehideDistance,
                    maxRayTraceBlockCountPerChunk,
                    rayTraceBlocks.isEmpty() ? null : rayTraceBlocks,
                    bypassRehideBlocks.isEmpty() ? null : bypassRehideBlocks,
                    serverLevel,
                    MinecraftServer.getServer().executor
            );

            try {
                // Release fence + plain store: pairs with the reader's getAcquire so the
                // controller's constructor stores are visible alongside the published reference.
                // setRelease is unavailable here because the target field is declared final.
                VarHandle.releaseFence();
                CHUNK_PACKET_BLOCK_CONTROLLER_FIELD.set(serverLevel, controller);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void handleUnload(RayTraceAntiXray plugin, World w) {
        ServerLevel serverLevel = ((CraftWorld) w).getHandle();
        ChunkPacketBlockController current =
                (ChunkPacketBlockController) CHUNK_PACKET_BLOCK_CONTROLLER_HANDLE.getAcquire(serverLevel);

        if (current instanceof ChunkPacketBlockControllerAntiXray antiXray) {
            ChunkPacketBlockController oldController = antiXray.getOldController();

            try {
                // Release fence + plain store: same pattern as handleLoad. setRelease is
                // unavailable because the target field is declared final.
                VarHandle.releaseFence();
                CHUNK_PACKET_BLOCK_CONTROLLER_FIELD.set(serverLevel, oldController);
            } catch (Exception e) {
                BukkitUtil.sneakyThrow(e);
            }
        }
    }
}
