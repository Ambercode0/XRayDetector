package com.ambercode.data;

import org.jetbrains.annotations.NotNull;

import java.util.Deque;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;

public final class Miner {

    private final Deque<TunnelUnit> tunnelQueue = new ConcurrentLinkedDeque<>();
    private final Deque<OreVein> veinQueue = new ConcurrentLinkedDeque<>();
    private final UUID uuid;

    public Miner(@NotNull UUID uuid) {
        this.uuid = uuid;
    }

    @NotNull
    public UUID getUuid() {
        return uuid;
    }

    public void addUnit(@NotNull TunnelUnit tunnel) {
        this.tunnelQueue.addLast(tunnel);
    }

    public boolean containsUnit(@NotNull TunnelUnit tunnel) {
        return this.tunnelQueue.contains(tunnel);
    }

    public Deque<OreVein> getVeinQueue() {
        return veinQueue;
    }

    public Deque<TunnelUnit> getTunnelQueue() {
        return tunnelQueue;
    }
}