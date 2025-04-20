package com.vanillage.raytraceantixray;

import com.google.common.base.Throwables;
import com.google.common.collect.MapMaker;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.vanillage.raytraceantixray.antixray.ChunkPacketBlockControllerAntiXray;
import com.vanillage.raytraceantixray.commands.RayTraceAntiXrayTabExecutor;
import com.vanillage.raytraceantixray.data.ChunkBlocks;
import com.vanillage.raytraceantixray.data.PlayerData;
import com.vanillage.raytraceantixray.data.VectorialLocation;
import com.vanillage.raytraceantixray.listeners.PlayerListener;
import com.vanillage.raytraceantixray.listeners.WorldListener;
import com.vanillage.raytraceantixray.net.DuplexHandlerImpl;
import com.vanillage.raytraceantixray.tasks.RayTraceCallable;
import com.vanillage.raytraceantixray.tasks.RayTraceTimerTask;
import com.vanillage.raytraceantixray.tasks.UpdateBukkitRunnable;
import com.vanillage.raytraceantixray.util.BukkitUtil;
import io.papermc.paper.antixray.ChunkPacketBlockController;
import io.papermc.paper.configuration.WorldConfiguration.Anticheat.AntiXray;
import io.papermc.paper.configuration.type.EngineMode;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.Timer;
import java.util.UUID;
import java.util.concurrent.*;

public final class RayTraceAntiXray extends JavaPlugin {
    // private volatile Configuration configuration;
    private volatile boolean running = false;
    private volatile boolean timingsEnabled = false;
    private final ConcurrentMap<ClientboundLevelChunkWithLightPacket, ChunkBlocks> packetChunkBlocksCache = new MapMaker().weakKeys().makeMap();
    private final ConcurrentMap<UUID, PlayerData> playerData = new ConcurrentHashMap<>();
    private ExecutorService executorService;
    private Timer timer;
    private long updateTicks = 1L;

    @Override
    public void onEnable() {
        if (!new File(getDataFolder(), "README.txt").exists()) {
            saveResource("README.txt", false);
        }

        saveDefaultConfig();
        FileConfiguration config = getConfig();
        config.options().copyDefaults(true);
        reloadConfig();

        // saveConfig();
        // configuration = config;
        // Initialize stuff.

        running = true;
        // Use a combination of a tick thread (timer) and a ray trace thread pool.
        // The timer schedules tasks (a task per player) to the thread pool and ensures a common and defined tick start and end time without overlap by waiting for the thread pool to finish all tasks.
        // A scheduled thread pool with a task per player would also be possible but then there's no common tick.
        executorService = Executors.newFixedThreadPool(Math.max(config.getInt("settings.anti-xray.ray-trace-threads"), 1), new ThreadFactoryBuilder().setThreadFactory(Executors.defaultThreadFactory()).setNameFormat("RayTraceAntiXray ray trace thread %d").setDaemon(true).build());
        // Use a timer instead of a single thread scheduled executor because there is no equivalent for the timer's schedule method.
        timer = new Timer("RayTraceAntiXray tick thread", true);
        timer.schedule(new RayTraceTimerTask(this), 0L, Math.max(config.getLong("settings.anti-xray.ms-per-ray-trace-tick"), 1L));
        updateTicks = Math.max(config.getLong("settings.anti-xray.update-ticks"), 1L);

        if (!BukkitUtil.IS_FOLIA) {
            new UpdateBukkitRunnable(this).runTaskTimer(this, 0L, updateTicks);
        }

        // Register events.
        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new WorldListener(this), this);
        pluginManager.registerEvents(new PlayerListener(this), this);

        // Handle reloads/plugin managers
        for (World w : Bukkit.getWorlds())
            WorldListener.handleLoad(this, w);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!validatePlayer(player))
                continue;

            PlayerData data = new PlayerData(getLocations(player, new VectorialLocation(player.getLocation())));
            data.setCallable(new RayTraceCallable(this, data));
            getPlayerData().put(player.getUniqueId(), data);
            new DuplexHandlerImpl(this, player)
                    .attach(player);
        }

        // registerCommands();
        getCommand("raytraceantixray").setExecutor(new RayTraceAntiXrayTabExecutor(this));
        getLogger().info(getPluginMeta().getDisplayName() + " enabled");
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
        // The server catches all throwables and may continue to run after disabling this plugin.
        // We want to ensure as much as possible that everything is left behind in a clean and defined state.
        // So the goal is to at least attempt to execute all critical sections of code, regardless of what happens before.
        // Considering errors during error handling and JLS 11.1.3. Asynchronous Exceptions, throwables could potentially be thrown anywhere (even between blocks of code or statements?).
        // Thus the only way is to nest try-finally statements like this: try { try { } finally { } } finally { }
        // According to the bytecode of nested try-catch statements in JVMS 3.12, all nested try blocks are entered at the same time.
        // So we either reach the innermost try block, in which case all blocks will be at least attempt to be executed, or no block is entered at all (e.g. in case of a throwable being thrown before).
        // Both outcomes yield a defined state of this plugin.
        // A more intuitive way would be to nest inside of the finally clause like this: try { } finally { try { } finally { } }
        // However, this doesn't provide the same guarantees as described above.
        // Additionally, we can add catch clauses to collect suppressed exceptions and rethrow in the last finally clause.
        Throwable throwable = null;
        try {
            try {
                try {
                    try {
                        // Cleanup stuff.
                        try {
                            for (Player p : Bukkit.getOnlinePlayers()) {
                                if (p.hasMetadata("NPC")) continue;
                                DuplexHandlerImpl.detach(p, DuplexHandlerImpl.NAME);
                            }
                        } catch (Throwable t) {
                            if (throwable == null) {
                                throwable = t;
                            } else {
                                throwable.addSuppressed(t);
                            }
                        }
                    } catch (Throwable t) {
                        if (throwable == null) {
                            throwable = t;
                        } else {
                            throwable.addSuppressed(t);
                        }
                    } finally {
                        running = false;
                        timer.cancel();
                    }
                } catch (Throwable t) {
                    if (throwable == null) {
                        throwable = t;
                    } else {
                        throwable.addSuppressed(t);
                    }
                } finally {
                    executorService.shutdownNow();

                    try {
                        executorService.awaitTermination(1000L, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    } finally {
                        try {
                            for (World w : Bukkit.getWorlds()) {
                                WorldListener.handleUnload(this, w);
                            }
                        } catch (Throwable t) {
                            if (throwable == null) {
                                throwable = t;
                            } else {
                                throwable.addSuppressed(t);
                            }
                        }
                    }
                }
            } catch (Throwable t) {
                if (throwable == null) {
                    throwable = t;
                } else {
                    throwable.addSuppressed(t);
                }
            } finally {
                packetChunkBlocksCache.clear();
                playerData.clear();
            }
        } catch (Throwable t) {
            if (throwable == null) {
                throwable = t;
            } else {
                throwable.addSuppressed(t);
            }
        } finally {
            if (throwable != null) {
                Throwables.throwIfUnchecked(throwable);
                throw new RuntimeException(throwable);
            }
        }

        getLogger().info(getPluginMeta().getDisplayName() + " disabled");
    }

    public void reload() {
        onDisable();
        onEnable();
        getLogger().info(getDescription().getFullName() + " reloaded");
    }

    public void reloadChunks(Iterable<Player> players) {
        for (Player bp : players) {
            PlayerData data = new PlayerData(getLocations(bp, new VectorialLocation(bp.getLocation())));
            data.setCallable(new RayTraceCallable(this, data));
            getPlayerData().put(bp.getUniqueId(), data);

            ServerPlayer p = ((CraftPlayer) bp).getHandle();
            var playerChunkManager = p.serverLevel().moonrise$getPlayerChunkLoader();
            playerChunkManager.removePlayer(p);
            playerChunkManager.addPlayer(p);
        }
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isTimingsEnabled() {
        return timingsEnabled;
    }

    public void setTimingsEnabled(boolean timingsEnabled) {
        this.timingsEnabled = timingsEnabled;
    }

    public ConcurrentMap<ClientboundLevelChunkWithLightPacket, ChunkBlocks> getPacketChunkBlocksCache() {
        return packetChunkBlocksCache;
    }

    public ConcurrentMap<UUID, PlayerData> getPlayerData() {
        return playerData;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public long getUpdateTicks() {
        return updateTicks;
    }

    public boolean isEnabled(World world) {
        AntiXray antiXray = ((CraftWorld) world).getHandle().paperConfig().anticheat.antiXray;

        if (antiXray.enabled && antiXray.engineMode == EngineMode.HIDE) {
            FileConfiguration config = getConfig();
            return config.getBoolean("world-settings." + world.getName() + ".anti-xray.ray-trace", config.getBoolean("world-settings.default.anti-xray.ray-trace"));
        }

        return false;
    }

    public boolean validatePlayer(Player player) {
        return !player.hasMetadata("NPC");
    }

    public static VectorialLocation[] getLocations(Entity entity, VectorialLocation location) {
        World world = entity.getWorld();
        ChunkPacketBlockController chunkPacketBlockController = ((CraftWorld) world).getHandle().chunkPacketBlockController;

        if (chunkPacketBlockController instanceof ChunkPacketBlockControllerAntiXray && ((ChunkPacketBlockControllerAntiXray) chunkPacketBlockController).rayTraceThirdPerson) {
            VectorialLocation thirdPersonFrontLocation = new VectorialLocation(location);
            thirdPersonFrontLocation.getDirection().multiply(-1.);
            return new VectorialLocation[] { location, move(entity, new VectorialLocation(world, location.getVector().clone(), location.getDirection().clone())), move(entity, thirdPersonFrontLocation) };
        }

        return new VectorialLocation[] { location };
    }

    private static VectorialLocation move(Entity entity, VectorialLocation location) {
        location.getVector().subtract(location.getDirection().clone().multiply(getMaxZoom(entity, location, 4.)));
        return location;
    }

    private static double getMaxZoom(Entity entity, VectorialLocation location, double maxZoom) {
        Vector vector = location.getVector();
        Vec3 position = new Vec3(vector.getX(), vector.getY(), vector.getZ());
        double positionX = position.x;
        double positionY = position.y;
        double positionZ = position.z;
        Vector direction = location.getDirection();
        double directionX = direction.getX();
        double directionY = direction.getY();
        double directionZ = direction.getZ();
        ServerLevel serverLevel = ((CraftWorld) location.getWorld()).getHandle();
        net.minecraft.world.entity.Entity handle = ((CraftEntity) entity).getHandle();

        // Logic copied from Minecraft client.
        for (int i = 0; i < 8; i++) {
            float cornerX = (float) ((i & 1) * 2 - 1);
            float cornerY = (float) ((i >> 1 & 1) * 2 - 1);
            float cornerZ = (float) ((i >> 2 & 1) * 2 - 1);
            cornerX *= 0.1f;
            cornerY *= 0.1f;
            cornerZ *= 0.1f;
            Vec3 corner = position.add(cornerX, cornerY, cornerZ);
            Vec3 cornerMoved = new Vec3(positionX - directionX * maxZoom + (double) cornerX, positionY - directionY * maxZoom + (double) cornerY, positionZ - directionZ * maxZoom + (double) cornerZ);
            BlockHitResult result = serverLevel.clip(new ClipContext(corner, cornerMoved, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, handle));

            if (result.getType() != HitResult.Type.MISS) {
                double zoom = result.getLocation().distanceTo(position);

                if (zoom < maxZoom) {
                    maxZoom = zoom;
                }
            }
        }

        return maxZoom;
    }

    public static boolean hasController(World world) {
        return ((CraftWorld) world).getHandle().chunkPacketBlockController instanceof ChunkPacketBlockControllerAntiXray;
    }

}
