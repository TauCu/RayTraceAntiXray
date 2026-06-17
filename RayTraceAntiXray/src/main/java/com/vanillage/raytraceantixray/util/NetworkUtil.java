package com.vanillage.raytraceantixray.util;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import org.bukkit.Bukkit;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.NoSuchElementException;
import java.util.Objects;

public class NetworkUtil {

    private static Field field_serverGamePacketListenerImpl_connection = null;

    private static Field field_regionizedServer_connections = null;
    private static Object regionizedServerInstance = null;

    @SuppressWarnings("unchecked")
    public static Iterable<Connection> getConnections() {
        if (BukkitUtil.IS_FOLIA) {
            try {
                if (field_regionizedServer_connections == null || regionizedServerInstance == null) {
                    ClassLoader scl = Bukkit.getServer().getClass().getClassLoader();
                    Class<?> regionizedServerClazz = scl.loadClass("io.papermc.paper.threadedregions.RegionizedServer");
                    Field connectionsField = regionizedServerClazz.getDeclaredField("connections");
                    connectionsField.setAccessible(true);

                    Method getInstanceMethod = regionizedServerClazz.getDeclaredMethod("getInstance");
                    getInstanceMethod.setAccessible(true);
                    Object instance = getInstanceMethod.invoke(null);

                    field_regionizedServer_connections = connectionsField;
                    regionizedServerInstance = instance;
                }
                return (Iterable<Connection>) field_regionizedServer_connections.get(regionizedServerInstance);
            } catch (Exception e) {
                throw new RuntimeException("Could not resolve regionized server connections field", e);
            }
        } else {
            return MinecraftServer.getServer().getConnection().getConnections();
        }
    }

    public static Channel getChannelOrThrow(Connection connection) {
        return Objects.requireNonNull(connection.channel, "Channel is null for address: " + connection.getRemoteAddress());
    }

    public static Connection getServerConnectionOrThrow(InetAddress address) {
        return Objects.requireNonNull(getServerConnection(address), "Connection not found for address: " + address);
    }

    public static Connection getServerConnection(InetAddress address) {
        for (Connection c : getConnections()) {
            if (c.getRemoteAddress() instanceof InetSocketAddress addr && addr.getAddress() == address) {
                return c;
            }
        }
        return null;
    }

    protected static void addBeforeHandler(ChannelPipeline pipe, String name, ChannelHandler handler) {
        while (true) {
            try {
                pipe.addBefore("packet_handler", name, handler);
            } catch (NoSuchElementException ignored) {
                // packet handler hasn't been added yet.
                pipe.addLast(name, handler);
                // packet_handler might have been added while doing this. If so, remove ourselves and try again.
                if (pipe.get("packet_handler") != null) {
                    // allow this to raise an exception if another thread is messing with this handler.
                    pipe.remove(handler);
                    continue;
                }
            }
            break;
        }
    }

}
