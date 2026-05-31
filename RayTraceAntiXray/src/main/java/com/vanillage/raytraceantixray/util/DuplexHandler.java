package com.vanillage.raytraceantixray.util;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelPipeline;
import org.bukkit.entity.Player;

import java.net.InetAddress;
import java.util.NoSuchElementException;
import java.util.Objects;

public class DuplexHandler extends ChannelDuplexHandler {

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
            addBeforeHandler(channel.pipeline());
            this.channel = channel;
        }
    }

    private void addBeforeHandler(ChannelPipeline pipe) {
        while (true) {
            try {
                pipe.addBefore("packet_handler", name, this);
            } catch (NoSuchElementException ignored) {
                // packet handler hasn't been added yet.
                pipe.addLast(name, this);
                // packet_handler might have been added while doing this. If so, remove ourselves and try again.
                if (pipe.get("packet_handler") != null) {
                    // allow this to raise an exception if another thread is messing with this handler.
                    pipe.remove(this);
                    continue;
                }
            }
            break;
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
