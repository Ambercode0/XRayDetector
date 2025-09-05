package com.ambercode.data;

import org.bukkit.Location;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

public class OreVein {
    private final List<Location> oreBlocks = new ArrayList<>();
    private final Material oreType;
    private final long discoveredAt;
    private final Location centerLocation;

    public OreVein(Location firstOre, Material oreType, long discoveredAt) {
        this.oreType = oreType;
        this.discoveredAt = discoveredAt;
        this.centerLocation = firstOre.clone();
        this.oreBlocks.add(firstOre);
    }

    public void addOreBlock(Location oreLocation) {
        oreBlocks.add(oreLocation);
        updateCenterLocation();
    }

    private void updateCenterLocation() {
        if (oreBlocks.isEmpty()) return;

        double totalX = 0, totalZ = 0;
        for (Location loc : oreBlocks) {
            totalX += loc.getBlockX();
            totalZ += loc.getBlockZ();
        }

        centerLocation.setX(totalX / oreBlocks.size());
        centerLocation.setZ(totalZ / oreBlocks.size());
    }

    public Location getCenterLocation() {
        return centerLocation;
    }

    public List<Location> getOreBlocks() {
        return oreBlocks;
    }

    public Material getOreType() {
        return oreType;
    }

    public long getDiscoveredAt() {
        return discoveredAt;
    }

    public int getVeinSize() {
        return oreBlocks.size();
    }
}