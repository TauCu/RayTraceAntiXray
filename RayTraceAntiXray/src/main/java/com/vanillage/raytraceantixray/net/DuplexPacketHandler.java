package com.vanillage.raytraceantixray.net;

import com.vanillage.raytraceantixray.RayTraceAntiXray;
import com.vanillage.raytraceantixray.data.ChunkBlocks;
import com.vanillage.raytraceantixray.data.LongWrapper;
import com.vanillage.raytraceantixray.data.PlayerData;
import com.vanillage.raytraceantixray.util.DuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.world.level.chunk.LevelChunk;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

public class DuplexPacketHandler extends DuplexHandler {

    public static final String NAME = "com.vanillage.raytraceantixray:duplex_handler";

    private final RayTraceAntiXray plugin;
    private final Player player;

    public DuplexPacketHandler(RayTraceAntiXray plugin, Player player) {
        super(NAME);
        this.plugin = plugin;
        this.player = player;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (handle(ctx, msg, promise)) {
            super.write(ctx, msg, promise);
        }
    }

    public boolean handle(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        if (msg instanceof ClientboundLevelChunkWithLightPacket packet) {
            // A player data instance is always bound to a world and defines what is to be calculated.
            // Apart from the join and quit event, this is the only place that defines the world of a player by renewing the player data instance.
            // In principle, we could (additionally) renew the player data instance anywhere else if we detect a world change (e.g. move event, changed world event, ...).
            // However, Anti-Xray specifies what is to be calculated via the chunk packet.
            // Since Anti-Xray is async, chunk packets can be delayed.
            // (The packet order of all packets is still being preserved and consistent.)
            // This could for example lead to the following order of events:
            // (1) Chunk packet event of world A.
            // (2) Changed world event from world A to B.
            // (3) Chunk packet event of world A.
            // (4) Chunk packet event of world B.
            // This would lead to unnecessarily renewing the player data instance multiple times in the worst case.
            // Therefore, we only renew the player data instance here.
            // For similar reasons, we also handle chunk unloads via packet events below.
            // Everywhere else we have to check if the player's world still matches the world of the player data instance before we use it.
            // (See for example the move event.)
            // Get the result from Anti-Xray for the current chunk packet.
            // We can't remove the entry because the same chunk packet can be sent to multiple players.
            // The garbage collector will remove the entry later since we're using a weak key map.
            ChunkBlocks chunkBlocks = plugin.getPacketChunkBlocksCache().get(packet);
            ConcurrentMap<UUID, PlayerData> playerDataMap = plugin.getPlayerData();
            UUID uniqueId = player.getUniqueId();

            if (chunkBlocks == null) {
                PlayerData playerData = playerDataMap.get(uniqueId);
                if (playerData == null) {
                    return true;
                }

                Location location = player.getEyeLocation();

                if (!location.getWorld().equals(playerData.getLocations()[0].getWorld())) {
                    plugin.createPlayerDataFor(player, location);
                }

                return true;
            }

            LevelChunk chunk = chunkBlocks.getChunk();

            if (chunk == null) {
                return true;
            }

            CraftWorld world = chunk.getLevel().getWorld();
            PlayerData playerData = playerDataMap.get(uniqueId);
            if (playerData == null) {
                return true;
            }
            if (!world.equals(playerData.getLocations()[0].getWorld())) {
                Location location = player.getEyeLocation();

                if (!world.equals(location.getWorld())) {
                    return true;
                }

                playerData = plugin.createPlayerDataFor(player, location);
                if (playerData == null) {
                    return true;
                }
            }

            chunkBlocks = new ChunkBlocks(chunk, new HashMap<>(chunkBlocks.getBlocks()));
            playerData.getChunks().put(chunkBlocks.getKey(), chunkBlocks);
        } else if (msg instanceof ClientboundForgetLevelChunkPacket packet) {
            PlayerData playerData = plugin.getPlayerData().get(player.getUniqueId());
            if (playerData != null) {
                playerData.getChunks().remove(new LongWrapper(packet.pos().toLong()));
            }
        } else if (msg instanceof ClientboundRespawnPacket) {
            PlayerData playerData = plugin.getPlayerData().get(player.getUniqueId());
            if (playerData != null) {
                playerData.getChunks().clear();
            }
        }
        return true;
    }

}
