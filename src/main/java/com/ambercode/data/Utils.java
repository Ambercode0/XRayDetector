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

package com.ambercode.data;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public interface Utils {

    static boolean isPreciousOre(@NotNull Material material) {
        return material == Material.DIAMOND_ORE || material == Material.DEEPSLATE_DIAMOND_ORE
                || material == Material.ANCIENT_DEBRIS;
    }

    static boolean isCommonOre(@NotNull Material material) {
        return material.name().endsWith("_ORE") && !isPreciousOre(material);
    }

    static int manhattanDistance(@NotNull TunnelUnit unit1, @NotNull TunnelUnit unit2) {
        return Math.abs(unit1.getX() - unit2.getX()) + Math.abs(unit1.getY() - unit2.getY()) + Math.abs(unit1.getZ() - unit2.getZ());
    }
    
    static Set<TunnelUnit> getAllOreVeinUnits(@NotNull Block block, boolean exposed) {
        HashSet<TunnelUnit> units = new HashSet<>();
        Material material = block.getType();
        searchOreVein(block, material, units, exposed);
        return units;
    }

    private static void searchOreVein(@NotNull Block block, @NotNull Material material, @NotNull HashSet<TunnelUnit> units, boolean exposed) {
        if (block.getType() != material) return;

        TunnelUnit unit = new TunnelUnit(block.getX(), block.getY(), block.getZ(),
                block.getType(), exposed, System.currentTimeMillis());

        if (!units.add(unit)) return;

        for (BlockFace face : FACES) {
            searchOreVein(block.getRelative(face), material, units, exposed);
        }
    }

    BlockFace[] FACES = new BlockFace[] {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN};

    static boolean isAdjacent(Block block, TunnelUnit other) {
        return false;
    }
}
