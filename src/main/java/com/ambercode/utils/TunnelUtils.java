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