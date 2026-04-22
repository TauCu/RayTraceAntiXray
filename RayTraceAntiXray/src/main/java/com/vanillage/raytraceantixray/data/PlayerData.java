package com.vanillage.raytraceantixray.data;

import com.vanillage.raytraceantixray.RayTraceAntiXray;
import com.vanillage.raytraceantixray.net.DuplexPacketHandler;
import com.vanillage.raytraceantixray.tasks.UpdateBukkitRunnable;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.World;
import org.bukkit.entity.Player;

public final class PlayerData {

    private final RayTraceAntiXray plugin;
    private final Player player;
    private final DuplexPacketHandler packetHandler;
    private final UpdateBukkitRunnable updateRunnable;
    private ScheduledTask updateTask;
    private volatile WorldContext context;

    public PlayerData(RayTraceAntiXray plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.packetHandler = new DuplexPacketHandler(plugin, this);
        this.updateRunnable = new UpdateBukkitRunnable(plugin, this);
        updateWorldContext(player.getWorld());
    }

    public Player getPlayer() {
        return player;
    }

    public WorldContext getContext() {
        return context;
    }

    public void setContext(WorldContext ctx) {
        this.context = ctx;
    }

    public DuplexPacketHandler getPacketHandler() {
        return packetHandler;
    }

    public UpdateBukkitRunnable getUpdateRunnable() {
        return updateRunnable;
    }

    public ScheduledTask getUpdateTask() {
        return updateTask;
    }

    public synchronized void startUpdateTask() {
        if (updateTask != null && !updateTask.isCancelled())
            return;

        updateTask = player.getScheduler().runAtFixedRate(plugin, updateRunnable, null, plugin.getUpdateTicks(), plugin.getUpdateTicks());
    }

    public synchronized void stopUpdateTask() {
        if (updateTask == null || updateTask.isCancelled())
            return;

        updateTask.cancel();
        updateTask = null;
    }

    public WorldContext updateWorldContext(World world) {
        var newCtx = new WorldContext(plugin, world);
        this.context = newCtx;
        return newCtx;
    }

}
