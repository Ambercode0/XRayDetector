/*
 *     XRayDetector - An advanced automatic detector to prevent X-Ray in your server
 *     Copyright (C) 2025 'AmberCode'
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
     * Calculates the Manhattan distance between two TunnelUnits on a 2D plane.
     * The Manhattan distance is the sum of the absolute differences of their X and Z coordinates.
     * If the TunnelUnits are in different worlds, the method returns -1.
     *
     * @param a The first TunnelUnit, representing a specific location in the tunnel system.
     * @param b The second TunnelUnit, representing another location in the tunnel system.
     * @return the Manhattan distance between the two TunnelUnits if they are in the same world,
     *         otherwise -1.
     */
    public static int manhattanDistance2D(TunnelUnit a, TunnelUnit b) {
        return a.getWorldName().equals(b.getWorldName()) ? Math.abs(a.getX() - b.getX()) + Math.abs(a.getZ() - b.getZ()) : -1;
    }

    /**
     * Determines whether the given material is classified as an ore.
     *
     * @param material The material to check.
     * @return true if the material is one of the specified ores (e.g., diamond ore, deepslate diamond ore, emerald ore,
     *         deepslate emerald ore, or ancient debris); false otherwise.
     */
    public static boolean isOre(Material material) {
        return material == Material.DIAMOND_ORE || material == Material.DEEPSLATE_DIAMOND_ORE || material == Material.EMERALD_ORE || material == Material.DEEPSLATE_EMERALD_ORE || material == Material.ANCIENT_DEBRIS;
    }

    /**
     * Determines whether the material of the given TunnelUnit is classified as an ore.
     *
     * @param unit The TunnelUnit whose material is to be checked.
     * @return true if the material of the TunnelUnit is classified as an ore; false otherwise.
     */
    public static boolean isOre(TunnelUnit unit) {
        return isOre(unit.getMaterial());
    }

    /**
     * Represents an array of block directions adjacent to a specific block.
     * This includes all six cardinal directions in a three-dimensional space:
     * down, up, north, south, east, and west.
     * <p>
     * This array can be used for operations that require traversal or inspection
     * of neighboring blocks, such as pathfinding, adjacency checks, or interaction
     * with nearby blocks in a voxel-based environment.
     */
    public static final BlockFace[] ADJACENT_DIRECTIONS = new BlockFace[]{
            BlockFace.DOWN, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP
    };

    /**
     * Determines whether a given tunnel unit or block is exposed to air. If a structure is
     * specified, it evaluates the units in the tunnel structure for direct air adjacency.
     * If no structure is provided, it evaluates the mined block's surrounding blocks.
     *
     * @param tunnelUnit The target tunnel unit being evaluated. Must not be null.
     * @param tunnelStructure The tunnel structure to assess adjacency or null
     *                        if the structure is not to be considered.
     * @param minedBlock The block that has been mined. Must not be null.
     * @return true if the provided tunnel unit or block is exposed to air, false otherwise.
     */
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
