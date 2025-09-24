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

import java.util.Deque;
import java.util.HashSet;
import java.util.List;
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
    
    static Set<TunnelUnit> getAllOreVeinUnits(@NotNull Block block, @NotNull Miner miner) {
        HashSet<TunnelUnit> units = new HashSet<>();
        Material material = block.getType();
        searchOreVein(block, material, units, Utils.hasAdjacentAir(block, miner));
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

    private static boolean hasAdjacentAir(@NotNull Block block, @NotNull Miner miner) {
        Deque<TunnelUnit> units = miner.getTunnelQueue();
        for (BlockFace face : FACES) {
            Block relative = block.getRelative(face);
            boolean relativeAirWasMinedBlock = units.stream().anyMatch(tu -> {
                boolean isSameX = tu.getX() == relative.getX();
                boolean isSameY = tu.getY() == relative.getY();
                boolean isSameZ = tu.getZ() == relative.getZ();
                return isSameX && isSameY && isSameZ;
            });
            if (relative.getType() == Material.AIR && !relativeAirWasMinedBlock) return true;
        }
        return false;
    }

    static boolean isExposed(@NotNull Block block, @NotNull Miner miner) {
        Deque<TunnelUnit> units = miner.getTunnelQueue();

        // If no mining history, check if the block has adjacent air
        if (units.isEmpty()) return hasAdjacentAir(block, miner);

        // Only check exposure for precious ores
        if (!isPreciousOre(block.getType())) return false;

        Deque<OreVein> veins = miner.getVeinDeque();

        // Check if this block is part of any known vein
        for (OreVein vein : veins) {
            List<TunnelUnit> tus = vein.getTunnelUnits();
            // If a block is in this vein, return the vein's exposed status
            if (tus.stream().anyMatch(t -> t.isSame(block))) {
                // The vein is exposed if ANY block in it is exposed
                return tus.stream().anyMatch(TunnelUnit::isExposed);
            }
        }

        // Block not in any known vein yet, check if it has adjacent air
        return hasAdjacentAir(block, miner);
    }

    static double[] getVeinCenter(@NotNull OreVein vein) {
        if (vein.getTunnelUnits().isEmpty()) {
            throw new IllegalArgumentException("Vein blocks set cannot be empty");
        }

        double sumX = 0, sumY = 0, sumZ = 0;
        for (TunnelUnit unit : vein.getTunnelUnits()) {
            sumX += unit.getX();
            sumY += unit.getY();
            sumZ += unit.getZ();
        }

        int size = vein.getTunnelUnits().size();
        return new double[] {sumX / size, sumY / size, sumZ / size};
    }

    static double calculateEuclideanDistance(double @NotNull [] vector1, double @NotNull [] vector2) {
        if (vector1.length != 3 || vector2.length != 3) {
            throw new IllegalArgumentException("Vectors must have exactly 3 components");
        }

        double sumOfSquares = 0.0;
        for (int i = 0; i < 3; i++) {
            double diff = vector1[i] - vector2[i];
            sumOfSquares += diff * diff;
        }

        return Math.sqrt(sumOfSquares);
    }

}
