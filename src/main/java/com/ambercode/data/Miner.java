// Enhanced Miner class
package com.ambercode.data;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Miner {

    private final UUID uuid;
    private final List<TunnelStructure> createdTunnels = new ArrayList<>();
    private final double suspicionScore = 0.00;

    public Miner(@NotNull UUID uuid) {
        this.uuid = uuid;
    }

    @NotNull
    public UUID getUuid() {
        return uuid;
    }

    @NotNull
    public List<TunnelStructure> getCreatedTunnels() {
        return createdTunnels;
    }

    public double getSuspicionScore() {
        return suspicionScore;
    }

    public int getDiscoveredOreVeins() {
        return +0;
    }

    public int getMinedBlocks() {
        return +0;
    }
}