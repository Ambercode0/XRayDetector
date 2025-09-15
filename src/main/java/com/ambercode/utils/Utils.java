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

import com.ambercode.XRayDetector;
import com.ambercode.data.Miner;
import com.ambercode.data.TunnelPath;
import com.ambercode.data.TunnelStructure;
import com.ambercode.data.TunnelUnit;
import com.ambercode.logging.FileLogger;
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
            for (final BlockFace face : ADJACENT_DIRECTIONS) {
                final Material relativeMaterial = minedBlock.getRelative(face).getType();
                if (relativeMaterial == Material.AIR || relativeMaterial == Material.CAVE_AIR) {
                    return true;
                }
            }
            return false;
        }

        for (final TunnelUnit tempUnit : tunnelStructure.getMainTunnelPath().getUnits())
            if (tunnelUnit.isOre() && Utils.manhattanDistance2D(tempUnit, tunnelUnit) == 1 && tempUnit.isExposedToAir() && tempUnit.isOre())
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
        for (final TunnelUnit unit : structure.getMainTunnelPath().getUnits()) {
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

    /**
     * Finds the next TunnelUnit in the given list that is classified as an ore,
     * starting the search from a specified index.
     *
     * @param units The list of TunnelUnit objects to search through. Must not be null.
     * @param startIndex The index in the list to start searching from. The search will begin
     *                   at startIndex + 1 and proceed to the end of the list.
     * @return The next TunnelUnit that is classified as an ore if found, otherwise null.
     */
    private static TunnelUnit findNextOreUnit(@NotNull List<TunnelUnit> units, int startIndex) {
        for (int i = startIndex + 1; i < units.size(); i++)
            if (units.get(i).isOre())
                return units.get(i);
        return null;
    }

    /**
     * Calculates the change in direction between three consecutive TunnelUnits using their coordinates.
     * The calculation is based on the angles formed between the vectors of the start-to-middle
     * and middle-to-end segments.
     *
     * @param start The starting TunnelUnit. Must not be null.
     * @param middle The middle TunnelUnit, acting as the pivot. Must not be null.
     * @param end The ending TunnelUnit. Must not be null.
     * @return The smallest change in direction in radians, between 0 and π.
     */
    private static double calculateDirectionChange(@NotNull TunnelUnit start, @NotNull TunnelUnit middle, @NotNull TunnelUnit end) {
        final double angle1 = Math.atan2(middle.getZ() - start.getZ(), middle.getX() - start.getX());
        final double angle2 = Math.atan2(end.getZ() - middle.getZ(), end.getX() - middle.getX());
        final double change = Math.abs(angle2 - angle1);
        return Math.min(change, 2 * Math.PI - change);
    }

    /**
     * Computes the straightness of a path in a tunnel based on ore unit direction changes.
     * The method analyzes sequences of three consecutive ore units to determine the changes
     * in direction and calculates an average direction change. The result is normalized
     * to a range of 0 to 1, where 1 indicates a perfectly straight path (no direction change),
     * and 0 indicates maximum direction changes (PI radians).
     *
     * @param units the list of TunnelUnit objects representing the path in the tunnel.
     *              Each TunnelUnit denotes a segment of the tunnel. Units with ores are
     *              taken into consideration for computing direction changes.
     *              Must not be null.
     *
     * @return a double value representing the path's straightness, normalized between 0 and 1.
     *         Specific error codes are returned as:
     *         - {@code TUNNEL_TOO_SMALL.errorNumber} if the list size is less than 3.
     *         - {@code NO_ORES.errorNumber} if no TunnelUnit containing ore is found.
     *         - {@code ONLY_ONE_ORE.errorNumber} if only one TunnelUnit containing ore is found.
     */
    public static double computePathStraightness(@NotNull List<TunnelUnit> units) {
        if (units.size() < 3) return ErrorComputeReturnCode.TUNNEL_TOO_SMALL.errorNumber;

        final List<Double> directionChanges = new ArrayList<>();
        TunnelUnit current = null;

        // Find the first ore unit
        for (final TunnelUnit unit : units) {
            if (unit.isOre()) {
                current = unit;
                break;
            }
        }

        if (current == null) return ErrorComputeReturnCode.NO_ORES.errorNumber;

        // Analyze direction changes between sequences of three ore units
        while (true) {
            final TunnelUnit next = findNextOreUnit(units, units.indexOf(current));
            if (next == null) break;

            final TunnelUnit afterNext = findNextOreUnit(units, units.indexOf(next));
            if (afterNext == null) break;

            directionChanges.add(calculateDirectionChange(current, next, afterNext));
            current = next;
        }

        if (directionChanges.isEmpty()) return ErrorComputeReturnCode.ONLY_ONE_ORE.errorNumber;

        // Calculate average direction change and normalize to 0-1 range
        // where 1 means perfectly straight (0 direction change)
        // and 0 means maximum direction changes (PI radians)
        double avgDirectionChange = directionChanges.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        return 1.0 - (avgDirectionChange / Math.PI);
    }

    private static final double DIAMOND_DENSITY_WEIGHT = 0.3;
    private static final double TIME_PATTERN_WEIGHT = 0.25;
    private static final double EXPOSURE_WEIGHT = 0.25;
    private static final double PATH_CHARACTERISTICS_WEIGHT = 0.2;

    // Overall analysis weights
    private static final double RECENT_TUNNELS_WEIGHT = 0.6;
    private static final double HISTORICAL_TUNNELS_WEIGHT = 0.4;
    private static final double TUNNEL_COUNT_THRESHOLD = 3;
    private static final long RECENT_TUNNEL_THRESHOLD = 3600000; // 1 hour in milliseconds

    private static final double SUSPICIOUS_DIAMOND_RATIO = 0.0147; // ~1.47% is suspicious
    private static final long SUSPICIOUS_TIME_INTERVAL = 20000; // 20 seconds
    private static final double SUSPICIOUS_UNEXPOSED_RATIO = 0.75; // 70% unexposed is suspicious
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
    public static double calculateXRaySuspicionScore(@NotNull TunnelStructure structure, @NotNull XRayDetector plugin) {
        final double diamondDensityScore = calculateDiamondDensityScore(structure);
        final double timePatternScore = calculateTimePatternScore(structure);
        final double exposureScore = calculateExposureScore(structure);
        final double pathScore = calculatePathScore(structure);

        FileLogger fileLogger = plugin.getFileLogger();
        fileLogger.addLogMessage(String.format("Tunnel %s scores - Diamond Density: %.2f, Time Pattern: %.2f, Exposure: %.2f, Path: %.2f",
                structure.getUuid(), diamondDensityScore, timePatternScore, exposureScore, pathScore));

        if (diamondDensityScore < 0) return ErrorComputeReturnCode.getErrorByNumber((int) diamondDensityScore).errorNumber;

        if (timePatternScore < 0) return ErrorComputeReturnCode.getErrorByNumber((int) timePatternScore).errorNumber;

        if (exposureScore < 0) return ErrorComputeReturnCode.getErrorByNumber((int) exposureScore).errorNumber;

        if (pathScore < 0) return ErrorComputeReturnCode.getErrorByNumber((int) pathScore).errorNumber;

        if (isTunnelMiningExposedOreVein(structure)) return ErrorComputeReturnCode.IS_EXPOSED_VEIN_SHORT_TUNNEL.errorNumber;

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

        for (final TunnelUnit unit : structure.getMainTunnelPath().getUnits()) {
            final Material material = unit.getMaterial();
            if (material == Material.STONE || material == Material.DEEPSLATE ||
                    material == Material.DIAMOND_ORE || material == Material.DEEPSLATE_DIAMOND_ORE) {
                totalBlocks++;
                if (material == Material.DIAMOND_ORE || material == Material.DEEPSLATE_DIAMOND_ORE) {
                    diamondCount++;
                }
            }
        }

        if (totalBlocks == 0) return 0;
        if (diamondCount == 0) return ErrorComputeReturnCode.NO_ORES.errorNumber;
        if (diamondCount <= 2) return ErrorComputeReturnCode.ONLY_ONE_ORE.errorNumber;

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
        final List<Long> diamondTimes = new ArrayList<>();

        for (final TunnelUnit unit : structure.getMainTunnelPath().getUnits()) {
            if (unit.getMaterial() == Material.DIAMOND_ORE ||
                    unit.getMaterial() == Material.DEEPSLATE_DIAMOND_ORE) {
                diamondTimes.add(unit.getMinedAt());
            }
        }

        if (diamondTimes.isEmpty()) return ErrorComputeReturnCode.NO_ORES.errorNumber;
        if (diamondTimes.size() <= 2) return ErrorComputeReturnCode.ONLY_ONE_ORE.errorNumber;

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

        for (final TunnelUnit unit : structure.getMainTunnelPath().getUnits()) {
            if (unit.getMaterial() == Material.DIAMOND_ORE ||
                    unit.getMaterial() == Material.DEEPSLATE_DIAMOND_ORE) {
                totalDiamonds++;
                if (!unit.isExposedToAir()) {
                    unexposedDiamonds++;
                }
            }
        }

        if (totalDiamonds == 0) return ErrorComputeReturnCode.NO_ORES.errorNumber;
        if (totalDiamonds <= 2) return ErrorComputeReturnCode.ONLY_ONE_ORE.errorNumber;
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
        return straightness >= SUSPICIOUS_PATH_STRAIGHTNESS ? 1.0 : straightness / SUSPICIOUS_PATH_STRAIGHTNESS;
    }

    /**
     * Determines if a tunnel mining operation has exposed a significant portion of an ore vein.
     * <p>
     * This method evaluates the main tunnel path within the provided tunnel structure and determines if the exposed ore
     * vein meets the criteria. The criteria include the tunnel path having fewer than or equal to 10 units
     * and at least 60% of those units being ore that is exposed to air.
     *
     * @param structure the tunnel structure to analyze, which contains the main tunnel path and its units.
     *                  Must not be null.
     * @return true if the tunnel path has 10 or fewer units and at least 60% of those units are exposed ores;
     *         false otherwise.
     */
    public static boolean isTunnelMiningExposedOreVein(@NotNull TunnelStructure structure) {
        TunnelPath tunnelPath = structure.getMainTunnelPath();
        int size = tunnelPath.getUnits().size();
        int ores = (int) tunnelPath.getUnits().stream().filter(TunnelUnit::isOre).count();
        int oresAndExposed = (int) tunnelPath.getUnits().stream().filter(TunnelUnit::isOre).filter(TunnelUnit::isExposedToAir).count();
        return size <= 10 && (double) oresAndExposed / size >= 0.6f;
    }

    /**
     * Calculates an overall suspicion score for a miner by analyzing all their tunnel structures.
     * The analysis weighs recent tunnels more heavily than historical ones and considers:
     * - Individual tunnel suspicion scores
     * - Patterns across multiple tunnels
     * - Time-based analysis of mining behavior
     *
     * @param miner  The miner whose tunnels are to be analyzed
     * @param plugin The XRayDetector plugin instance
     * @return A suspicion score between 0 and 1, where higher values indicate more suspicious behavior
     */
    public static double calculateOverallMinerSuspicionScore(@NotNull Miner miner, @NotNull XRayDetector plugin) {
        final List<TunnelStructure> tunnels = miner.getCreatedTunnels();
        if (tunnels.size() < TUNNEL_COUNT_THRESHOLD) {
            return ErrorComputeReturnCode.NOT_ENOUGH_DATA.errorNumber;
        }

        long currentTime = System.currentTimeMillis();
        final List<Double> recentScores = new ArrayList<>();
        final List<Double> historicalScores = new ArrayList<>();

        // Categorize and calculate scores for each tunnel
        for (final TunnelStructure tunnel : tunnels) {
            double score = calculateXRaySuspicionScore(tunnel, plugin);
            if (score < 0) continue; // Skip invalid scores

            if (currentTime - tunnel.getMainTunnelPath().getUnits().getFirst().getMinedAt() < RECENT_TUNNEL_THRESHOLD) {
                recentScores.add(score);
            } else {
                historicalScores.add(score);
            }
        }

        // Calculate weighted average of recent and historical scores
        double recentAverage = recentScores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double historicalAverage = historicalScores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        return (recentAverage * RECENT_TUNNELS_WEIGHT) + (historicalAverage * HISTORICAL_TUNNELS_WEIGHT);
    }

    /**
     * Retrieves a list of connected ore blocks starting from the specified block and forms a tunnel-like structure of ores.
     *
     * @param startBlock the starting block from which the ore vein will be evaluated; must not be null.
     * @return a list of TunnelUnit objects representing the continuous ore vein, or an empty list if no ore vein is found.
     */
    @NotNull
    public List<TunnelUnit> getOreVein(@NotNull Block startBlock) {
        final ArrayList<TunnelUnit> vein = new ArrayList<>();
        final TunnelUnit startUnit = new TunnelUnit(
                startBlock.getX(),
                startBlock.getZ(),
                startBlock.getType(),
                System.currentTimeMillis(),
                startBlock.getWorld().getName()
        );

        if (startUnit.isOre()) {
            getAdjacentOresHelper(startUnit, startBlock, vein);
        }

        return vein;
    }

    /**
     * Recursive helper method that identifies and collects all adjacent ore blocks
     * starting from a given block. This method explores all the adjacent blocks
     * to find ores, adds them to the vein list, and continues the recursion until
     * all connected ore blocks are identified.
     *
     * @param currentUnit the current tunnel unit representing the ore block being inspected
     * @param currentBlock the block corresponding to the current tunnel unit
     * @param vein a list that accumulates all the connected ore blocks encountered
     */
    private void getAdjacentOresHelper(@NotNull TunnelUnit currentUnit, @NotNull Block currentBlock, @NotNull List<TunnelUnit> vein) {
        // Add current ore to the vein
        vein.add(currentUnit);

        // Check all adjacent blocks
        for (final BlockFace face : ADJACENT_DIRECTIONS) {
            final Block adjacent = currentBlock.getRelative(face);
            final TunnelUnit adjacentUnit = new TunnelUnit(
                    adjacent.getX(),
                    adjacent.getZ(),
                    adjacent.getType(),
                    System.currentTimeMillis(),
                    adjacent.getWorld().getName()
            );

            // Only recurse if it's an ore and not already in the vein
            if (adjacentUnit.isOre() && !vein.contains(adjacentUnit)) {
                getAdjacentOresHelper(adjacentUnit, adjacent, vein);
            }
        }
    }
}
