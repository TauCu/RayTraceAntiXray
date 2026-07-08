package com.vanillage.raytraceantixray.util;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import net.minecraft.network.Connection;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.server.MinecraftServer;
import org.bukkit.Bukkit;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.Callable;

public class NetworkUtil {

    private static Callable<Iterable<Connection>> connectionsGetter;

    @SuppressWarnings("unchecked")
    public static Iterable<Connection> getConnections() {
        if (connectionsGetter == null) {
            if (BukkitUtil.IS_FOLIA) {
                try {
                    ClassLoader scl = Bukkit.getServer().getClass().getClassLoader();
                    Class<?> c_regionizedServer = scl.loadClass("io.papermc.paper.threadedregions.RegionizedServer");
                    Field f_connections = c_regionizedServer.getDeclaredField("connections");
                    f_connections.setAccessible(true);

                    Method m_getInstance = c_regionizedServer.getDeclaredMethod("getInstance");
                    m_getInstance.setAccessible(true);
                    Object instance = m_getInstance.invoke(null);

                    connectionsGetter = () -> ((Iterable<Connection>) f_connections.get(instance));
                } catch (Throwable t) {
                    throw new RuntimeException("Could not resolve regionized server connections field", t);
                }
            } else {
                connectionsGetter = () -> MinecraftServer.getServer().getConnection().getConnections();
            }
        }

        try {
            return connectionsGetter.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Channel getChannelOrThrow(Connection connection) {
        return Objects.requireNonNull(connection.channel, "Channel is null for address: " + connection.getRemoteAddress());
    }

    public static Connection getServerConnectionOrThrow(Object key) throws NullPointerException {
        return Objects.requireNonNull(getServerConnection(key), "Connection not found for key: " + key);
    }

    public static Connection getServerConnection(Object key) {
        for (Connection c : getConnections()) {
            if (c.channel == key) {
                return c;
            } else if (Objects.equals(c.getRemoteAddress(), key)) {
                return c;
            } else if (c.getRemoteAddress() instanceof InetSocketAddress addr) {
                if (addr.getAddress() == key)
                    return c;
            }
        }

        return null;
    }

    /// throws if connection or channel cannot be found
    public static ChannelHandler getHandler(Object key, String name) {
        Channel channel = NetworkUtil.getChannelOrThrow(NetworkUtil.getServerConnectionOrThrow(key));
        return channel.pipeline().get(name);
    }

    public static int resolvePacketId(ProtocolInfo.DetailsProvider template, PacketType<?> type) {
        // java was a mistake
        Integer[] ret = new Integer[1];
        template.details().listPackets((test, id) -> {
            if (ret[0] == null && test.id().equals(type.id()))
                ret[0] = id;
        });

        if (ret[0] == null)
            throw new NoSuchElementException("could not find packet: " + type + " in: " + template);
        return ret[0];
    }

}
