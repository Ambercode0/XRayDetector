package com.ambercode.data;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;

public class Unit {

    private final BlockFace direction;
    private final Location location;
    private final boolean hasOre;
    private final long minedAtTimestamp;

    public Unit(BlockFace direction, Location location, boolean hasOre, long minedAtTimestamp) {
        this.direction = direction;
        this.location = location;
        this.hasOre = hasOre;
        this.minedAtTimestamp = minedAtTimestamp;
    }

    public BlockFace getDirection() {
        return direction;
    }

    public Location getLocation() {
        return location;
    }

    public boolean isHasOre() {
        return hasOre;
    }

    public long getMinedAtTimestamp() {
        return minedAtTimestamp;
    }
}
