package com.vanillage.raytraceantixray;

import com.google.common.collect.MapMaker;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.vanillage.raytraceantixray.antixray.ChunkPacketBlockControllerAntiXray;
import com.vanillage.raytraceantixray.commands.RayTraceAntiXrayTabExecutor;
import com.vanillage.raytraceantixray.data.ChunkBlocks;
import com.vanillage.raytraceantixray.data.PlayerData;
import com.vanillage.raytraceantixray.data.VectorialLocation;
import com.vanillage.raytraceantixray.listeners.PlayerListener;
import com.vanillage.raytraceantixray.listeners.WorldListener;
import com.vanillage.raytraceantixray.tasks.RayTraceTimerTask;
import com.vanillage.raytraceantixray.util.BukkitUtil;
import io.papermc.paper.antixray.ChunkPacketBlockController;
import io.papermc.paper.configuration.WorldConfiguration.Anticheat.AntiXray;
import io.papermc.paper.configuration.type.EngineMode;
import net.kyori.adventure.text.Component;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;

public final class RayTraceAntiXray extends JavaPlugin {

    private final ConcurrentMap<ClientboundLevelChunkWithLightPacket, ChunkBlocks> packetChunkBlocksCache = new MapMaker().weakKeys().makeMap();
    private final ConcurrentMap<UUID, PlayerData> playerData = new ConcurrentHashMap<>();
    // Set of messages and errors this plugin instance has nagged about.
    // Used to prevent console spam.
    private final Set<String> nagged = Collections.synchronizedSet(new HashSet<>());

    private ExecutorService executorService;
    private Timer timer;
    private long updateTicks = 1L;
    private boolean debug;
    private volatile boolean running = false;
    private volatile boolean timingsEnabled = false;

    public static RayTraceAntiXray getInstance() {
        return JavaPlugin.getPlugin(RayTraceAntiXray.class);
    }

    @Override
    public void onEnable() {
        if (!new File(getDataFolder(), "README.txt").exists()) {
            saveResource("README.txt", false);
        }

        // Setup and load config
        saveDefaultConfig();
        FileConfiguration config = getConfig();
        config.options().copyDefaults(true);
        reloadConfig();

        // Register commands
        getCommand("raytraceantixray").setExecutor(new RayTraceAntiXrayTabExecutor(this));

        //
        // Initialize
        //
        running = true;

        // Start raytrace executor & timer
        // Use a combination of a tick thread (timer) and a ray trace thread pool.
        // The timer schedules tasks (a task per player) to the thread pool and ensures a common and defined tick start and end time without overlap by waiting for the thread pool to finish all tasks.
        // A scheduled thread pool with a task per player would also be possible but then there's no common tick.
        executorService = Executors.newFixedThreadPool(
                Math.max(config.getInt("settings.anti-xray.ray-trace-threads"), 1),
                new ThreadFactoryBuilder().setThreadFactory(Executors.defaultThreadFactory())
                        .setNameFormat("RayTraceAntiXray worker thread %d")
                        .setDaemon(true)
                        .build()
        );
        // Use a timer instead of a single thread scheduled executor because there is no equivalent for the timer's schedule method.
        timer = new Timer("RayTraceAntiXray tick thread", true);
        timer.schedule(new RayTraceTimerTask(this), 0L, Math.max(config.getLong("settings.anti-xray.ms-per-ray-trace-tick"), 1L));

        // Register events.
        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new WorldListener(this), this);
        pluginManager.registerEvents(new PlayerListener(this), this);

        // Replace the xray chunk controller with our own for all worlds.
        // Subsequent worlds are handled by WorldListener
        for (World w : Bukkit.getWorlds())
            WorldListener.handleLoad(this, w);

        // Initialize all online players
        // This is required due to reloads/plugin managers
        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                PlayerData data = tryCreatePlayerDataFor(player);
                if (data == null)
                    continue;
                data.startUpdateTask();
                reloadChunks0(player);
            } catch (Throwable e) {
                getLogger().log(Level.SEVERE, "Exception raised while initializing for \"" + player + "\" during plugin load", e);
            }
        }

        getLogger().info(getPluginMeta().getDisplayName() + " enabled");
    }

    @Override
    public void onDisable() {
        running = false;

        // unregister all event handlers
        HandlerList.unregisterAll(this);

        // cancel any global tasks
        Bukkit.getScheduler().cancelTasks(this);
        Bukkit.getGlobalRegionScheduler().cancelTasks(this);

        // unhook from players
        for (PlayerData data : playerData.values()) {
            try {
                // detach network handler
                var handler = data.getPacketHandler();
                if (handler != null)
                    handler.detach();
            } catch (Throwable t) {
                getLogger().log(Level.SEVERE, "Error while detaching network handler from: " + data.getPlayer(), t);
            }
            try {
                // stop the update task
                data.stopUpdateTask();
            } catch (Throwable t) {
                getLogger().log(Level.SEVERE, "Error while stopping update task for: " + data.getPlayer(), t);
            }
        }

        // stop timer and scheduler
        timer.cancel();
        executorService.shutdownNow();
        try {
            if (!executorService.awaitTermination(5000L, TimeUnit.MILLISECONDS))
                getLogger().log(Level.WARNING, "Timed out while waiting for executor to shutdown!");
        } catch (InterruptedException e) {
            Thread.interrupted();
            getLogger().log(Level.SEVERE, "Interrupted while shutting down executor", e);
        }

        // unhook xray chunk controller from all worlds
        for (World world : Bukkit.getWorlds()) {
            try {
                WorldListener.handleUnload(this, world);
            } catch (Throwable t) {
                getLogger().log(Level.SEVERE, "Error while unhooking from world: " + world.getName());
            }
        }

        // clear collections
        playerData.clear();
        packetChunkBlocksCache.clear();
        nagged.clear();

        getLogger().info(getPluginMeta().getDisplayName() + " disabled");
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        updateTicks = Math.max(getConfig().getLong("settings.anti-xray.update-ticks"), 1L);
        debug = getConfig().getBoolean("settings.debug", false);
    }

    public void reload() {
        onDisable();
        onEnable();
        getLogger().info(getPluginMeta().getName() + " reloaded");
    }

    public void reloadChunks(Iterable<Player> players) {
        for (Player player : players) {
            PlayerData data = getPlayerData().get(player.getUniqueId());
            if (data == null)
                continue; // probably npc
            try {
                // clear existing xray context
                data.updateWorldContext(player.getWorld());
                // resend all chunks
                reloadChunks0(player);
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Failed to reloadChunks for: " + player, e);
            }
        }
    }

    private void reloadChunks0(Player player) {
        if (BukkitUtil.IS_FOLIA && !Bukkit.isOwnedByCurrentRegion(player)) {
            player.getScheduler().run(this, t -> reloadChunks0(player), null);
            return;
        }
        ServerPlayer sp = ((CraftPlayer) player).getHandle();
        var playerChunkManager = sp.level().moonrise$getPlayerChunkLoader();
        try {
            playerChunkManager.removePlayer(sp);
            playerChunkManager.addPlayer(sp);
        } catch (Throwable t) {
            // if it failed to add/remove, kick the player to keep the server in the most consistent state practicable
            player.kick(Component.text("Internal Error. Please contact an Administrator."));
            throw t;
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

    public PlayerData tryCreatePlayerDataFor(Player player) {
        if (!validatePlayer(player))
            return null;

        // create playerdata
        PlayerData playerData = new PlayerData(this, player);

        // define current locations
        Location loc = player.getEyeLocation();
        playerData.getContext().setLocations(RayTraceAntiXray.getLocations(player, playerData.getContext().getWorld(), new VectorialLocation(loc)));

        // add to map
        if (getPlayerData().putIfAbsent(player.getUniqueId(), playerData) != null)
            throw new IllegalStateException("PlayerData already exists for " + player);

        try {
            // attach network handler after playerdata is added to the map
            playerData.getPacketHandler().attach(player);
        } catch (Exception e) {
            getPlayerData().remove(player.getUniqueId(), playerData);
            throw new RuntimeException("Failed to attach packet handler to: " + player, e);
        }

        return playerData;
    }

    public static VectorialLocation[] getLocations(Entity entity, World world, VectorialLocation location) {
        ChunkPacketBlockController chunkPacketBlockController = ((CraftWorld) world).getHandle().chunkPacketBlockController;

        if (chunkPacketBlockController instanceof ChunkPacketBlockControllerAntiXray && ((ChunkPacketBlockControllerAntiXray) chunkPacketBlockController).rayTraceThirdPerson) {
            VectorialLocation thirdPersonFrontLocation = new VectorialLocation(location);
            thirdPersonFrontLocation.direction().multiply(-1.);
            return new VectorialLocation[] { location, move(entity, new VectorialLocation(location.position().clone(), location.direction().clone())), move(entity, thirdPersonFrontLocation) };
        }

        return new VectorialLocation[] { location };
    }

    private static VectorialLocation move(Entity entity, VectorialLocation location) {
        location.position().subtract(location.direction().clone().multiply(getMaxZoom(entity, location, 4.)));
        return location;
    }

    private static double getMaxZoom(Entity entity, VectorialLocation location, double maxZoom) {
        Vector vector = location.position();
        Vec3 position = new Vec3(vector.getX(), vector.getY(), vector.getZ());
        double positionX = position.x;
        double positionY = position.y;
        double positionZ = position.z;
        Vector direction = location.direction();
        double directionX = direction.getX();
        double directionY = direction.getY();
        double directionZ = direction.getZ();
        ServerLevel serverLevel = ((CraftWorld) entity.getWorld()).getHandle();
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

    public boolean isDebug(){
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public void logDebugNagReminder() {
        if (!isDebug()) {
            getLogger().log(Level.WARNING, """
                Duplicate errors will be ignored. Performance and efficacy might be affected if this error persists.
                Consider enabling debugging to see every error like this.""");
        }
    }

    // These methods are for preventing console spam from less than critical errors.
    public boolean hasNagged(String msg) {
        return nagged.contains(msg);
    }

    public boolean handleNag(String nag) {
        return nagged.add(nag) || isDebug();
    }

    public static boolean hasController(World world) {
        return ((CraftWorld) world).getHandle().chunkPacketBlockController instanceof ChunkPacketBlockControllerAntiXray;
    }
}
