package com.vanillage.raytraceantixray.net;

import com.vanillage.raytraceantixray.RayTraceAntiXray;
import com.vanillage.raytraceantixray.data.ChunkBlocks;
import com.vanillage.raytraceantixray.data.PlayerData;
import com.vanillage.raytraceantixray.data.WorldContext;
import com.vanillage.raytraceantixray.util.DuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.world.level.chunk.LevelChunk;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;

public class DuplexPacketHandler extends DuplexHandler {

    public static final String NAME = "com.vanillage.raytraceantixray:duplex_handler";

    private final RayTraceAntiXray plugin;
    private final PlayerData playerData;

    public DuplexPacketHandler(RayTraceAntiXray plugin, PlayerData playerData) {
        super(NAME);
        this.plugin = plugin;
        this.playerData = playerData;
    }

    /// Invoked when a packet is written (clientbound)<br>
    /// Whereas {@link #channelRead(ChannelHandlerContext, Object)} is invoked when a packet is received (sevrerbound)
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (handle(ctx, msg, promise)) {
            super.write(ctx, msg, promise);
        }
    }

    /// if this returns false, the packet won't be sent
    public boolean handle(ChannelHandlerContext chanCtx, Object msg, ChannelPromise chanPromise) {
        if (msg instanceof ClientboundLevelChunkWithLightPacket packet) {
            // A xray context instance is always bound to a world and defines what is to be calculated.
            // Short of initial creation, this is the only place that defines the world of a player by renewing the xray context instance.
            // In principle, we could (additionally) renew the xray context instance anywhere else if we detect a world change (e.g. move event, changed world event, ...).
            // However, Anti-Xray specifies what is to be calculated via the chunk packet.
            // Since Anti-Xray is async, chunk packets can be delayed.
            // (The packet order of all packets is still being preserved and consistent.)
            // This could for example lead to the following order of events:
            // (1) Chunk packet event of world A.
            // (2) Changed world event from world A to B.
            // (3) Chunk packet event of world A.
            // (4) Chunk packet event of world B.
            // This would lead to unnecessarily renewing the xray context instance multiple times in the worst case.
            // Therefore, we only renew the xray context instance here.
            // For similar reasons, we also handle chunk unloads via packet events below.
            // Everywhere else we have to check if the player's world still matches the world of the xray context instance before we use it.
            // (See for example the move event.)
            // Get the result from Anti-Xray for the current chunk packet.
            // We can't remove the entry because the same chunk packet can be sent to multiple players.
            // The garbage collector will remove the entry later since we're using a weak key map.
            ChunkBlocks chunkBlocks = plugin.getPacketChunkBlocksCache().get(packet);
            World playerWorld = playerData.getPlayer().getWorld();
            WorldContext context = playerData.getContext();

            if (chunkBlocks == null) {
                // RayTraceAntiXray is probably not enabled in this world (or other plugins bypass Anti-Xray).
                // We can't determine the world from the chunk packet in this case.
                // Thus we use the player's current (more up to date) world instead.
                if (!playerWorld.equals(context.getWorld())) {
                    // Detected a world change.
                    // In the event order listing above, this corresponds to (4) when RayTraceAntiXray is disabled in world B.
                    // The player's current world is world B since (2).
                    playerData.updateWorldContext(playerWorld);
                }

                return true;
            }

            // Get chunk from weak reference.
            LevelChunk chunk = chunkBlocks.getChunk();

            if (chunk == null) {
                // The chunk has already been unloaded and garbage collected.
                // A chunk unload packet will probably follow.
                // We can ignore this chunk packet.
                return true;
            }

            CraftWorld chunkWorld = chunk.getLevel().getWorld();
            if (!chunkWorld.equals(context.getWorld())) {
                // Detected a world change.
                if (!chunkWorld.equals(playerWorld)) {
                    // The player has changed the world again since this chunk packet was sent.
                    // (As described above, packets can be delayed.)
                    // Example event order for this case:
                    // (1) Chunk packet event of world A.
                    // (2) Changed world event from world A to B.
                    // (3) Changed world event from world B to C.
                    // (4) Chunk packet event of world B.
                    // (5) Chunk packet event of world C.
                    // The previous chunk packet was from world A in (1).
                    // The current chunk packet is from world B in (4) but the player is already in world C.
                    // We can ignore this chunk packet and wait until we get a chunk packet from world C in (5).
                    return true;
                }

                // Renew the context.
                context = playerData.updateWorldContext(playerWorld);
            }

            // We need to copy the chunk blocks because the same chunk packet could have been sent to multiple players.
            context.addChunk(chunkBlocks.copyWithDataOnly());
        } else if (msg instanceof ClientboundForgetLevelChunkPacket packet) {
            // Note that chunk unload packets aren't sent on world change and on respawn.
            // World changes are already handled above.
            WorldContext xrayCtx = playerData.getContext();
            xrayCtx.removeChunk(packet.pos().longKey());
        } else if (msg instanceof ClientboundRespawnPacket) {
            // As with world changes, chunk unload packets aren't sent on respawn.
            // All required chunks are (re)sent afterwards.
            // Thus we clear the chunks.
            // If respawning involves a world change, it will be handled in the next chunk packet event.
            WorldContext xrayCtx = playerData.getContext();
            xrayCtx.clearChunks();
        }
        return true;
    }

}
