package com.vanillage.raytraceantixray.util;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelPipeline;
import org.bukkit.entity.Player;

import java.net.InetAddress;
import java.util.NoSuchElementException;
import java.util.Objects;

public abstract class DuplexHandler extends ChannelDuplexHandler {

    private final String name;

    private volatile Channel channel;

    public DuplexHandler(String name) {
        this.name = Objects.requireNonNull(name);
    }

    public String getName() {
        return name;
    }

    public Channel getCurrentChannel() {
        return channel;
    }

    public void attach(Player player) throws RuntimeException {
        attach(player.getAddress().getAddress());
    }

    public void attach(InetAddress address) throws RuntimeException {
        attach(NetworkUtil.getChannelOrThrow(NetworkUtil.getServerConnectionOrThrow(address)));
    }

    public void attach(Channel channel) {
        synchronized (DuplexHandler.class) {
            detach();
            detach(channel, name);
            NetworkUtil.addBeforeHandler(channel.pipeline(), name, this);
            this.channel = channel;
        }
    }

    public void detach() throws RuntimeException {
        Channel channel = this.channel;
        if (channel != null)
            detach(channel, name);
    }

    public static void detach(Player player, String name) throws RuntimeException {
        detach(player.getAddress().getAddress(), name);
    }

    public static void detach(InetAddress address, String name) throws RuntimeException {
        detach(NetworkUtil.getChannelOrThrow(NetworkUtil.getServerConnectionOrThrow(address)), name);
    }

    public static synchronized void detach(Channel channel, String name) {
        ChannelPipeline pipeline = channel.pipeline();
        if (pipeline.get(name) instanceof DuplexHandler handler) {
            try {
                pipeline.remove(handler);
            } catch (NoSuchElementException ignored) {}
            handler.channel = null;
        }
    }

}
