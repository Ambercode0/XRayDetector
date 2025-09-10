package com.ambercode.utils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.EnumSet;
import java.util.Set;

public final class TunnelUtils {

    private static final Set<Material> ORE_MATERIALS = EnumSet.of(Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE, Material.ANCIENT_DEBRIS);

    public static boolean isOre(Block block) {
        return ORE_MATERIALS.contains(block.getType());
    }

    public static double manhattanDistance2D(Location loc1, Location loc2) {
        return Math.abs(loc1.getX() - loc2.getX()) + Math.abs(loc1.getZ() - loc2.getZ());
    }

    public static boolean isAdjacent(Location loc1, Location loc2) {
        int dx = Math.abs(loc1.getBlockX() - loc2.getBlockX());
        int dz = Math.abs(loc1.getBlockZ() - loc2.getBlockZ());
        return (dx == 1 && dz == 0) || (dx == 0 && dz == 1);
    }
}