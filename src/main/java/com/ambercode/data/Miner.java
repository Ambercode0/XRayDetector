// Enhanced Miner class
package com.ambercode.data;

import org.bukkit.block.Block;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Miner {
    private final List<Block> minedBlocks = new ArrayList<>();
    private final List<OreVein> discoveredOreVeins = new ArrayList<>();
    private final UUID uuid;
    private double suspicionScore = 0.0;

    public Miner(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }

    public List<Block> getMinedBlocks() {
        return minedBlocks;
    }

    public List<OreVein> getDiscoveredOreVeins() {
        return discoveredOreVeins;
    }

    public double getSuspicionScore() {
        return suspicionScore;
    }

    public void setSuspicionScore(double suspicionScore) {
        this.suspicionScore = suspicionScore;
    }

    public void addSuspicionScore(double points) {
        this.suspicionScore = Math.min(100.0, this.suspicionScore + points);
    }
}