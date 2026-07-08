package com.vanillage.raytraceantixray.util;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelPipeline;
import org.bukkit.entity.Player;

import java.net.InetAddress;
import java.util.NoSuchElementException;
import java.util.Objects;

public abstract class AttachableDuplexHandler extends ChannelDuplexHandler {

    private final String name;

    private volatile Channel channel;

    public AttachableDuplexHandler(String name) {
        this.name = Objects.requireNonNull(name);
    }

    protected abstract boolean inject(ChannelPipeline pipeline, String name);

    public String getName() {
        return name;
    }

    public Channel getCurrentChannel() {
        return channel;
    }

    public boolean attach(Player player) throws RuntimeException {
        return attach(player.getAddress().getAddress());
    }

    public boolean attach(Object key) throws RuntimeException {
        return attach(NetworkUtil.getChannelOrThrow(NetworkUtil.getServerConnectionOrThrow(key)));
    }

    public boolean attach(Channel channel) {
        synchronized (AttachableDuplexHandler.class) {
            detach();
            detach(channel, name);

            this.channel = channel;
            try {
                if (!inject(channel.pipeline(), name)) {
                    detach();
                    return false;
                }
            } catch (Throwable t) {
                detach();
                throw t;
            }

            return true;
        }
    }

    public boolean detach() throws RuntimeException {
        Channel channel = this.channel;
        if (channel != null)
            return detach(channel, name);
        return false;
    }

    public static boolean detach(Player player, String name) throws RuntimeException {
        return detach(player.getAddress().getAddress(), name);
    }

    public static boolean detach(InetAddress address, String name) throws RuntimeException {
        return detach(NetworkUtil.getChannelOrThrow(NetworkUtil.getServerConnectionOrThrow(address)), name);
    }

    public static synchronized boolean detach(Channel channel, String name) {
        ChannelPipeline pipeline = channel.pipeline();
        if (pipeline.get(name) instanceof AttachableDuplexHandler handler) {
            try {
                pipeline.remove(handler);
            } catch (NoSuchElementException ignored) {}
            handler.channel = null;
            return true;
        }
        return false;
    }

}
