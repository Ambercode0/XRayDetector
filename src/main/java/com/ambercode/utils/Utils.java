package com.ambercode.utils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

public final class Utils {

    public Utils() {
        throw new RuntimeException("This class is not enabled for instantiation");
    }

    /**
     * Computes the Manhattan distance between two locations (sum of absolute differences
     * of coordinates). This represents the distance when you can only move along lines
     * parallel to the coordinate axes, like navigating city blocks.
     * @param a The first location
     * @param b The second location
     * @return the Manhattan distance between the two locations
     */
    public static int manhattanDistance2D(Location a, Location b) {
        return Math.abs(a.getBlockX() - b.getBlockX()) + Math.abs(a.getBlockZ() - b.getBlockZ());
    }

    public static boolean isOre(Block block) {
        Material material = block.getType();
        return material == Material.DIAMOND_ORE || material == Material.DEEPSLATE_DIAMOND_ORE || material == Material.EMERALD_ORE || material == Material.DEEPSLATE_EMERALD_ORE || material == Material.ANCIENT_DEBRIS;
    }
}
