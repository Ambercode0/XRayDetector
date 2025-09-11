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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

        for (final TunnelUnit tempUnit : tunnelStructure.getMainTunnelPath().getUnits())
            if (Utils.manhattanDistance2D(tempUnit, tunnelUnit) == 1 && tempUnit.isExposedToAir() && tempUnit.isOre())
                return true;

        return false;
    }

    /**
     * Calculates the average ore density within a given tunnel structure.
     * This is determined by dividing the number of ore-containing units
     * by the total number of units in the main tunnel path.
     *
     * @param structure The tunnel structure to analyze. Must not be null.
     * @return The average ore density, calculated as the ratio of ore-containing
     *         units to the total number of units in the main tunnel path.
     */
    public static double averageOreDensity(@NotNull TunnelStructure structure) {
        int oreCount = 0;
        for (TunnelUnit unit : structure.getMainTunnelPath().getUnits()) {
            if (unit.isOre()) {
                oreCount++;
            }
        }
        return (double) oreCount / structure.getMainTunnelPath().getUnits().size();
    }
    
    

    /**
     * Calculates the average time (in milliseconds) between finding ores in a tunnel structure.
     * This is computed by finding time differences between consecutive ore discoveries
     * and calculating their average.
     *
     * @param structure The tunnel structure to analyze. Must not be null.
     * @return The average time in milliseconds between ore discoveries. Returns 0 if less than 2 ores are found.
     * Use {@link #formatTimeDifference(long)} to convert to human-readable format.
     */
    public static long averageTimePerOreFound(@NotNull TunnelStructure structure) {
        List<TunnelUnit> units = structure.getMainTunnelPath().getUnits();
        List<Long> oreTimes = new ArrayList<>();

        for (TunnelUnit unit : units)
            if (unit.isOre())
                oreTimes.add(unit.getMinedAt());

        if (oreTimes.size() < 2)
            return 0;

        Collections.sort(oreTimes);
        long totalTimeDiff = 0;
        for (int i = 1; i < oreTimes.size(); i++)
            totalTimeDiff += oreTimes.get(i) - oreTimes.get(i - 1);

        return totalTimeDiff / (oreTimes.size() - 1);
    }

    /**
     * Converts a time difference in milliseconds to a human-readable format.
     * Format examples: "5m 11s", "1h 30m", "45s"
     *
     * @param milliseconds The time difference in milliseconds
     * @return A formatted string representing the time difference
     */
    @NotNull
    public static String formatTimeDifference(long milliseconds) {
        if (milliseconds == 0) return "0s";

        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        seconds = seconds % 60;
        minutes = minutes % 60;

        StringBuilder result = new StringBuilder();
        if (hours > 0) {
            result.append(hours).append("h ");
        }
        if (minutes > 0) {
            result.append(minutes).append("m ");
        }
        if (seconds > 0 || (hours == 0 && minutes == 0)) {
            result.append(seconds).append("s");
        }

        return result.toString().trim();
    }

    /**
     * Calculates how straight a tunnel path is by analyzing the directional changes
     * between consecutive units. A perfectly straight path (score 1.0) would have all
     * units in a single line (either X or Z direction). A completely random path would
     * have a score closer to 0.0.
     * <p>
     * The calculation is based on:
     * 1. Computing direction changes between consecutive units
     * 2. Analyzing the consistency of these directions
     * 3. Normalizing the result to a 0-1 scale
     *
     * @param units List of TunnelUnit representing the tunnel path. Must contain at least 2 units.
     * @return A double between 0 and 1, where 1 represents a perfectly straight path
     * and 0 represents a path with maximum directional changes.
     * Returns 0 if the list contains fewer than 2 units.
     */
    public static double computePathStraightness(@NotNull List<TunnelUnit> units) {
        if (units.size() < 2) return 0.0;

        int directionChanges = 0;
        int prevDx = 0;
        int prevDz = 0;

        for (int i = 1; i < units.size(); i++) {
            int dx = units.get(i).getX() - units.get(i - 1).getX();
            int dz = units.get(i).getZ() - units.get(i - 1).getZ();

            if (i > 1 && (dx != prevDx || dz != prevDz)) {
                directionChanges++;
            }

            prevDx = dx;
            prevDz = dz;
        }

        double maxPossibleChanges = units.size() - 2.0;
        if (maxPossibleChanges <= 0) return 1.0;

        return Math.max(0.0, 1.0 - (directionChanges / maxPossibleChanges));
    }

    /**
     * Computes the straightness of the main tunnel path within a given tunnel structure.
     * A path's straightness is evaluated based on the directional changes between consecutive
     * TunnelUnits in the main tunnel path, where a perfectly straight path scores 1.0 and a highly
     * irregular or random path scores closer to 0.0.
     * <p>
     * Delegates the computation to a method handling the unit list extracted from the tunnel structure.
     *
     * @param structure The tunnel structure whose main tunnel path straightness is to be computed. Must not be null.
     * @return A double between 0 and 1, where 1 indicates a perfectly straight path, and 0 indicates a maximally irregular path.
     */
    public static double computePathStraightness(@NotNull TunnelStructure structure) {
        return computePathStraightness(structure.getMainTunnelPath().getUnits());
    }

    private static final double DIAMOND_DENSITY_WEIGHT = 0.3;
    private static final double TIME_PATTERN_WEIGHT = 0.25;
    private static final double EXPOSURE_WEIGHT = 0.25;
    private static final double PATH_CHARACTERISTICS_WEIGHT = 0.2;

    private static final double SUSPICIOUS_DIAMOND_RATIO = 0.015; // ~1.50% is suspicious
    private static final long SUSPICIOUS_TIME_INTERVAL = 20000; // 20 seconds
    private static final double SUSPICIOUS_UNEXPOSED_RATIO = 0.7; // 70% unexposed is suspicious
    private static final double SUSPICIOUS_PATH_STRAIGHTNESS = 0.8; // Very straight paths are suspicious

    /**
     * Calculates an overall suspicion score for potential X-Ray usage based on multiple factors:
     * - Diamond density relative to stone/deepslate mined
     * - Time patterns between diamond discoveries
     * - Ratio of unexposed diamonds
     * - Path characteristics (straightness vs. natural mining patterns)
     *
     * @param structure The tunnel structure to analyze
     * @return A suspicion score between 0 and 1, where higher values indicate more suspicious behavior
     */
    public static double calculateXRaySuspicionScore(@NotNull TunnelStructure structure) {
        double diamondDensityScore = calculateDiamondDensityScore(structure);
        double timePatternScore = calculateTimePatternScore(structure);
        double exposureScore = calculateExposureScore(structure);
        double pathScore = calculatePathScore(structure);

        return (diamondDensityScore * DIAMOND_DENSITY_WEIGHT) +
                (timePatternScore * TIME_PATTERN_WEIGHT) +
                (exposureScore * EXPOSURE_WEIGHT) +
                (pathScore * PATH_CHARACTERISTICS_WEIGHT);
    }

    /**
     * Calculates the diamond density score for the given TunnelStructure. This score is determined
     * by analyzing the ratio of diamond-related blocks (diamond ore and deepslate diamond ore)
     * to the total number of relevant tunnel blocks considered (stone, deepslate, diamond ore,
     * deepslate diamond ore). The score is normalized and capped at 1.0 using a predefined
     * suspicious diamond ratio.
     *
     * @param structure the TunnelStructure to analyze. Must not be null.
     * @return the diamond density score as a double value between 0.0 and 1.0.
     */
    private static double calculateDiamondDensityScore(@NotNull TunnelStructure structure) {
        long totalBlocks = 0;
        long diamondCount = 0;

        for (TunnelUnit unit : structure.getMainTunnelPath().getUnits()) {
            Material material = unit.getMaterial();
            if (material == Material.STONE || material == Material.DEEPSLATE ||
                    material == Material.DIAMOND_ORE || material == Material.DEEPSLATE_DIAMOND_ORE) {
                totalBlocks++;
                if (material == Material.DIAMOND_ORE || material == Material.DEEPSLATE_DIAMOND_ORE) {
                    diamondCount++;
                }
            }
        }

        if (totalBlocks == 0) return 0;
        double ratio = (double) diamondCount / totalBlocks;
        return Math.min(1.0, ratio / SUSPICIOUS_DIAMOND_RATIO);
    }

    /**
     * Calculates a score representing the suspiciousness of time patterns for mining diamond ores
     * within a given tunnel structure. The score is based on the time intervals between consecutive
     * diamond ore discoveries, where a higher ratio of short intervals is considered more suspicious.
     *
     * @param structure The tunnel structure containing the main tunnel path to analyze. Must not be null.
     * @return A score between 0.0 and 1.0, representing the fraction of suspicious time intervals
     *         relative to the total number of intervals between diamond discoveries.
     *         Returns 0.0 if fewer than two diamond ores are found.
     */
    private static double calculateTimePatternScore(@NotNull TunnelStructure structure) {
        List<Long> diamondTimes = new ArrayList<>();

        for (TunnelUnit unit : structure.getMainTunnelPath().getUnits()) {
            if (unit.getMaterial() == Material.DIAMOND_ORE ||
                    unit.getMaterial() == Material.DEEPSLATE_DIAMOND_ORE) {
                diamondTimes.add(unit.getMinedAt());
            }
        }

        if (diamondTimes.size() < 2) return 0;

        Collections.sort(diamondTimes);
        int suspiciousIntervals = 0;
        for (int i = 1; i < diamondTimes.size(); i++) {
            if (diamondTimes.get(i) - diamondTimes.get(i - 1) < SUSPICIOUS_TIME_INTERVAL) {
                suspiciousIntervals++;
            }
        }

        return (double) suspiciousIntervals / (diamondTimes.size() - 1);
    }

    /**
     * Calculates the exposure score for diamond ores in a given tunnel structure.
     * The score is based on the ratio of unexposed diamond ores to the total diamond ores
     * in the main tunnel path, normalized against a predefined suspicious unexposed ratio.
     *
     * @param structure The tunnel structure to analyze. Must not be null.
     * @return A score between 0 and 1, where 1 indicates that the unexposed ratio
     *         meets or exceeds the suspicious threshold, and lower values represent
     *         proportionally lower unexposed ratios.
     */
    private static double calculateExposureScore(@NotNull TunnelStructure structure) {
        int totalDiamonds = 0;
        int unexposedDiamonds = 0;

        for (TunnelUnit unit : structure.getMainTunnelPath().getUnits()) {
            if (unit.getMaterial() == Material.DIAMOND_ORE ||
                    unit.getMaterial() == Material.DEEPSLATE_DIAMOND_ORE) {
                totalDiamonds++;
                if (!unit.isExposedToAir()) {
                    unexposedDiamonds++;
                }
            }
        }

        if (totalDiamonds == 0) return 0;
        double unexposedRatio = (double) unexposedDiamonds / totalDiamonds;
        return unexposedRatio >= SUSPICIOUS_UNEXPOSED_RATIO ? 1.0 :
                unexposedRatio / SUSPICIOUS_UNEXPOSED_RATIO;
    }

    /**
     * Calculates a score that represents the straightness of a tunnel path within a given tunnel structure.
     * A higher score indicates a straighter path, which may be considered more suspicious if exceeding a certain threshold.
     *
     * @param structure The tunnel structure whose path straightness is being evaluated. Must not be null.
     * @return A double value between 0 and 1. A score of 1.0 indicates a path with maximum straightness,
     *         while a score closer to 0 indicates a less straight path.
     */
    private static double calculatePathScore(@NotNull TunnelStructure structure) {
        double straightness = computePathStraightness(structure);
        return straightness >= SUSPICIOUS_PATH_STRAIGHTNESS ? 1.0 :
                straightness / SUSPICIOUS_PATH_STRAIGHTNESS;
    }


}
