package com.vanillage.raytraceantixray.listeners;

import com.vanillage.raytraceantixray.RayTraceAntiXray;
import com.vanillage.raytraceantixray.data.PlayerData;
import com.vanillage.raytraceantixray.util.BukkitUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.logging.Level;

public final class PlayerListener implements Listener {
    private final RayTraceAntiXray plugin;

    public PlayerListener(RayTraceAntiXray plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (BukkitUtil.IS_FOLIA) {
            // On Folia, PlayerJoinEvent fires on the global-region thread.
            // player.getEyeLocation() and other entity-local state must be
            // accessed from the entity's own scheduler thread.
            player.getScheduler().run(plugin, t -> initPlayer(player), null);
        } else {
            initPlayer(player);
        }
    }

    private void initPlayer(Player player) {
        try {
            PlayerData data = plugin.tryCreatePlayerDataFor(player);
            if (data == null)
                return;

            data.startUpdateTask();
        } catch (Throwable t) {
            player.kick(Component.text("RayTraceAntiXray encountered an error for your connection, please contact server administrators: " + t.getMessage()));
            if (t instanceof Exception) {
                plugin.getLogger().log(Level.SEVERE, "Exception raised while creating data for \"" + player + "\" during player join", t);
            } else {
                BukkitUtil.sneakyThrow(t);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        PlayerData data = plugin.getPlayerData().get(event.getPlayer().getUniqueId());
        if (data != null) {
            data.getPacketHandler().detach();
            plugin.getPlayerData().remove(event.getPlayer().getUniqueId(), data);
        }
    }

}
