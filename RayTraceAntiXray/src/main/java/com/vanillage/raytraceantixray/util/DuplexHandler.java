package com.vanillage.raytraceantixray.util;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelPipeline;
import org.bukkit.entity.Player;

import java.net.InetAddress;
import java.util.Objects;

public class DuplexHandler extends ChannelDuplexHandler {

    private final String name;

    private volatile Channel channel;

    public DuplexHandler(String name) {
        this.name = Objects.requireNonNull(name);
    }

    public String getAttachedName() {
        return name;
    }

    public Channel getAttachedChannel() {
        if (channel != null && channel.pipeline().get(name) != this)
            channel = null;
        return channel;
    }

    public void attach(Player player) throws RuntimeException {
        attach(player.getAddress().getAddress());
    }

    public void attach(InetAddress address) throws RuntimeException {
        attach(NetworkUtil.getChannelOrThrow(NetworkUtil.getServerConnectionOrThrow(address)));
    }

    public void attach(Channel channel) {
        detach();
        ChannelPipeline pipe = channel.pipeline();
        detach(channel, name);
        if (pipe.get("packet_handler") == null) {
            pipe.addLast(name, this);
        } else {
            pipe.addBefore("packet_handler", name, this);
        }
        this.channel = channel;
    }

    public void detach() throws RuntimeException {
        if (channel != null) {
            detach(channel, name);
            channel = null;
        }
    }

    public static void detach(Player player, String name) throws RuntimeException {
        detach(player.getAddress().getAddress(), name);
    }

    public static void detach(InetAddress address, String name) throws RuntimeException {
        detach(NetworkUtil.getChannelOrThrow(NetworkUtil.getServerConnectionOrThrow(address)), name);
    }

    public static void detach(Channel channel, String name) {
        ChannelPipeline pipeline = channel.pipeline();
        if (pipeline.get(name) != null) {
            if (pipeline.remove(name) instanceof DuplexHandler handler) {
                handler.channel = null;
            }
        }
    }

}
