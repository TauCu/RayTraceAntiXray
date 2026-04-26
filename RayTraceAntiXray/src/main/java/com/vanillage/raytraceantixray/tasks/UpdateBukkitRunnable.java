package com.vanillage.raytraceantixray.tasks;

import com.vanillage.raytraceantixray.RayTraceAntiXray;
import com.vanillage.raytraceantixray.data.*;
import io.netty.channel.Channel;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.function.Consumer;

/// this is responsible for entity-local/main-thread tasks
public final class UpdateBukkitRunnable implements Consumer<ScheduledTask> {

    private final RayTraceAntiXray plugin;
    private final PlayerData data;

    public UpdateBukkitRunnable(RayTraceAntiXray plugin, PlayerData data) {
        this.plugin = plugin;
        this.data = data;
    }

    @Override
    public void accept(ScheduledTask t) {
        update();
    }

    public void update() {
        Player player = data.getPlayer();
        WorldContext context = data.getContext();
        World world = context.getWorld();
        if (!player.getWorld().equals(world))
            return; // the world has changed, context is renewed in the packet handler

        // define current raytrace locations
        Location loc = player.getEyeLocation();
        context.setLocations(RayTraceAntiXray.getLocations(player, world, new VectorialLocation(loc)));

        Queue<Result> results = context.getResults();
        List<Packet<?>> packetsToSend = new ArrayList<>();
        ServerLevel serverLevel = ((CraftWorld) world).getHandle();
        Environment environment = world.getEnvironment();

        // poll the computed visible blocks and send them to the client
        Result result;
        while ((result = results.poll()) != null) {
            ChunkBlocks chunkBlocks = result.chunkBlocks();

            // Check if the client still has the chunk loaded and if it wasn't resent in the meantime.
            // Note that even if this check passes, the server could have already unloaded or resent the chunk but the corresponding packet is still in the packet queue.
            if (context.getChunk(chunkBlocks.getKey()) != chunkBlocks)
                continue;

            BlockPos block = result.block();

            // No need to update an unloaded chunk
            if (!world.isChunkLoaded(block.getX() >> 4, block.getZ() >> 4))
                continue;

            BlockState blockState;
            BlockEntity blockEntity = null;

            if (result.visible()) {
                blockState = serverLevel.getBlockState(block);

                if (blockState.hasBlockEntity()) {
                    blockEntity = serverLevel.getBlockEntity(block);
                }
            } else if (environment == Environment.NETHER) {
                blockState = Blocks.NETHERRACK.defaultBlockState();
            } else if (environment == Environment.THE_END) {
                blockState = Blocks.END_STONE.defaultBlockState();
            } else if (block.getY() < 0) {
                blockState = Blocks.DEEPSLATE.defaultBlockState();
            } else {
                blockState = Blocks.STONE.defaultBlockState();
            }

            packetsToSend.add(new ClientboundBlockUpdatePacket(block, blockState));

            if (blockEntity != null) {
                var packet = blockEntity.getUpdatePacket();
                if (packet != null)
                    packetsToSend.add(packet);
            }
        }

        sendPackets(player, packetsToSend, false);
    }

    /// callers must not mutate the packets list after passing it to this method
    private static boolean sendPackets(Player player, Collection<Packet<?>> packets, boolean flush) {
        if (packets.isEmpty())
            return false;

        ServerGamePacketListenerImpl connection = ((CraftPlayer) player).getHandle().connection;
        if (connection == null || !connection.connection.isConnected())
            return false;

        Channel channel = connection.connection.channel;

        // wrap whole operation in event loop, otherwise Channel#write will do it for each packet
        channel.eventLoop().execute(() -> {
            for (Packet<?> packet : packets) {
                channel.write(packet);
            }
            if (flush)
                channel.flush();
        });
        return true;
    }
}
