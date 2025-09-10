package com.ambercode.utils;

import com.ambercode.data.TunnelStructure;
import com.ambercode.data.TunnelUnit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    /**
     * Computes the Manhattan distance between two locations (sum of absolute differences
     * of coordinates). This represents the distance when you can only move along lines
     * parallel to the coordinate axes, like navigating city blocks.
     * @param a The first location
     * @param b The second location
     * @return the Manhattan distance between the two locations
     */
    public static int manhattanDistance2D(TunnelUnit a, TunnelUnit b) {
        return Math.abs(a.getX() - b.getX()) + Math.abs(a.getZ() - b.getZ());
    }

    public static boolean isOre(Material material) {
        return material == Material.DIAMOND_ORE || material == Material.DEEPSLATE_DIAMOND_ORE || material == Material.EMERALD_ORE || material == Material.DEEPSLATE_EMERALD_ORE || material == Material.ANCIENT_DEBRIS;
    }

    public static boolean isOre(TunnelUnit unit) {
        return isOre(unit.getMaterial());
    }

    public static final BlockFace[] ADJACENT_DIRECTIONS = new BlockFace[]{
            BlockFace.DOWN, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP
    };

    public static boolean isExposedToAir(@NotNull TunnelUnit tunnelUnit, @Nullable TunnelStructure tunnelStructure, @NotNull Block minedBlock) {

        if (tunnelStructure == null) {
            for (BlockFace face : ADJACENT_DIRECTIONS) {
                Material relativeMaterial = minedBlock.getRelative(face).getType();
                if (relativeMaterial == Material.AIR || relativeMaterial == Material.CAVE_AIR) {
                    return true;
                }
            }
            return false;
        }

        for (final TunnelUnit tempUnit : tunnelStructure.getMainTunnelPath().getUnits()) {
            if (Utils.manhattanDistance2D(tempUnit, tunnelUnit) == 1 && tempUnit.isExposedToAir() && tempUnit.isOre()) {
                return true;
            }
        }
        return false;

    }
}
