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
     * VarHandle for {@code Level.chunkPacketBlockController}.
     *
     * <p>Obtained once at class-load time via {@link MethodHandles#privateLookupIn} so that we can
     * use {@link VarHandle#setRelease} instead of plain {@link Field#set}.  {@code setRelease}
     * provides a <em>release</em> memory fence on the write: any thread that subsequently reads the
     * field through an <em>acquire</em> fence (or through a {@code volatile} / {@code synchronized}
     * construct that itself implies acquire) is guaranteed to see the fully-constructed controller
     * object we published here.  {@code setVolatile} would additionally fence earlier stores too,
     * but that is not needed for our single-writer / ordered-publish pattern, so the lighter
     * {@code setRelease} is preferred.
     */
    private static final VarHandle CHUNK_PACKET_BLOCK_CONTROLLER_HANDLE;

    static {
        try {
            Field field = Level.class.getDeclaredField("chunkPacketBlockController");
            field.setAccessible(true);
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

            // setRelease: all writes performed before this point are visible to any thread that
            // subsequently reads this field through an acquire fence.
            CHUNK_PACKET_BLOCK_CONTROLLER_HANDLE.setRelease(serverLevel, controller);
        }
    }

    public static void handleUnload(RayTraceAntiXray plugin, World w) {
        ServerLevel serverLevel = ((CraftWorld) w).getHandle();
        ChunkPacketBlockController current =
                (ChunkPacketBlockController) CHUNK_PACKET_BLOCK_CONTROLLER_HANDLE.getAcquire(serverLevel);

        if (current instanceof ChunkPacketBlockControllerAntiXray antiXray) {
            ChunkPacketBlockController oldController = antiXray.getOldController();

            try {
                // setRelease: restoring the original controller with a release fence so that any
                // thread observing the restored value also sees the original controller's state.
                CHUNK_PACKET_BLOCK_CONTROLLER_HANDLE.setRelease(serverLevel, oldController);
            } catch (Exception e) {
                BukkitUtil.sneakyThrow(e);
            }
        }
    }
}
