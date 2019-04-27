package com.vexsoftware.votifier.bungee;

import com.google.gson.JsonObject;
import com.imaginarycode.minecraft.redisbungee.RedisBungee;
import com.imaginarycode.minecraft.redisbungee.events.PubSubMessageEvent;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.support.forwarding.AbstractPluginMessagingForwardingSource;
import com.vexsoftware.votifier.support.forwarding.ServerFilter;
import com.vexsoftware.votifier.support.forwarding.cache.VoteCache;
import com.vexsoftware.votifier.util.GsonInst;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class RedisBungeeMessageForwardingService extends AbstractPluginMessagingForwardingSource implements Listener {

    private final OnlineForwardPluginMessagingForwardingSource perProxyForwarder;
    private final String fallBackProxy;

    protected RedisBungeeMessageForwardingService(String channel, NuVotifier nuVotifier, ServerFilter serverFilter,
                                                  VoteCache cache, String fallBackProxy, String fallbackServer, int dumpRate) {
        super(channel, serverFilter, nuVotifier, cache, dumpRate);
        this.fallBackProxy = fallBackProxy;
        this.perProxyForwarder = new OnlineForwardPluginMessagingForwardingSource(channel, nuVotifier, serverFilter, cache, fallbackServer, dumpRate);

        ProxyServer.getInstance().getPluginManager().registerListener(nuVotifier, this);
        RedisBungee.getApi().registerPubSubChannels(channel + "-" + RedisBungee.getApi().getServerId());
    }

    @Override
    public void forward(Vote v) {
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(v.getUsername());
        if (player != null && player.getServer() != null) {
            perProxyForwarder.forward(v);
            return;
        }

        RedisBungee.getApi().sendChannelMessage(channel + "-" + fallBackProxy, v.serialize().toString());
    }

    @Override
    public void halt() {
        RedisBungee.getApi().unregisterPubSubChannels(channel + "-" + fallBackProxy);
    }

    @EventHandler
    public void onRedisPubSub(PubSubMessageEvent event) {
        if (event.getChannel().equals(channel + "-" + RedisBungee.getApi().getServerId())) {
            JsonObject obj = GsonInst.gson.fromJson(event.getMessage(), JsonObject.class);
            Vote vote = new Vote(obj);
            plugin.getPluginLogger().info("Vote received from RedisBungee: " + vote.toString() + " on channel " + event.getChannel());
            this.forward(vote);
        }
    }
}
