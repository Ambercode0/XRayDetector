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

import com.ambercode.data.TunnelUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.util.*;

public class TunnelPathFinder {

    /**
     * Finds the longest path of connected {@code TunnelUnit} objects within a given list.
     * The method determines adjacency based on the Manhattan distance of 1 between the units,
     * and uses a depth-first search to evaluate potential paths.
     *
     * @param tunnelUnits the list of {@code TunnelUnit} objects representing the tunnel system;
     *                    must not be null, but can be empty. An empty list will return an empty result.
     * @return a list of {@code TunnelUnit} objects forming the longest path. If multiple paths of
     *         equal maximum length exist, one of them will be returned. Returns an empty list if
     *         {@code tunnelUnits} is null or empty.
     */
    @NotNull
    public static List<TunnelUnit> findLongestPath(@NotNull List<TunnelUnit> tunnelUnits) {
        if (tunnelUnits.isEmpty()) {
            return new ArrayList<>();
        }

        if (tunnelUnits.size() == 1) {
            return new ArrayList<>(tunnelUnits);
        }

        // Build adjacency map for a quick neighbor lookup
        Map<TunnelUnit, List<TunnelUnit>> adjacencyMap = buildAdjacencyMap(tunnelUnits);

        List<TunnelUnit> longestPath = new ArrayList<>();

        // Try starting from each tunnel unit to find the globally longest path
        for (TunnelUnit start : tunnelUnits) {
            List<TunnelUnit> currentPath = findLongestPathFrom(start, adjacencyMap);
            if (currentPath.size() > longestPath.size()) {
                longestPath = currentPath;
            }
        }

        return longestPath;
    }

    /**
     * Constructs an adjacency map that represents the relationships between all {@code TunnelUnit} objects
     * in the provided list. Two {@code TunnelUnit} objects are considered adjacent if their Manhattan
     * distance is exactly 1.
     *
     * @param tunnelUnits the list of {@code TunnelUnit} objects for which the adjacency map is to be created.
     *                    Each unit is expected to have distinct coordinates in the XZ plane.
     * @return a map where each key is a {@code TunnelUnit}, and its value is a list of {@code TunnelUnit}
     *         objects that are adjacent to the key unit.
     */
    @NotNull
    private static Map<TunnelUnit, List<TunnelUnit>> buildAdjacencyMap(@NotNull List<TunnelUnit> tunnelUnits) {
        Map<TunnelUnit, List<TunnelUnit>> adjacencyMap = new HashMap<>();

        // Initialize adjacency lists
        for (TunnelUnit unit : tunnelUnits) {
            adjacencyMap.put(unit, new ArrayList<>());
        }

        // Build adjacency relationships
        for (int i = 0; i < tunnelUnits.size(); i++) {
            for (int j = i + 1; j < tunnelUnits.size(); j++) {
                TunnelUnit unit1 = tunnelUnits.get(i);
                TunnelUnit unit2 = tunnelUnits.get(j);

                if (manhattanDistance(unit1, unit2) == 1) {
                    adjacencyMap.get(unit1).add(unit2);
                    adjacencyMap.get(unit2).add(unit1);
                }
            }
        }

        return adjacencyMap;
    }

    /**
     * Calculates the Manhattan distance between two TunnelUnit objects.
     * The Manhattan distance is defined as the sum of the absolute differences
     * of their X and Z coordinates.
     *
     * @param unit1 the first TunnelUnit whose coordinates are used in the calculation.
     * @param unit2 the second TunnelUnit whose coordinates are used in the calculation.
     * @return the Manhattan distance between the two TunnelUnits.
     */
    @Range(from = 0, to = Integer.MAX_VALUE)
    private static int manhattanDistance(@NotNull TunnelUnit unit1, @NotNull TunnelUnit unit2) {
        return Math.abs(unit1.getX() - unit2.getX()) + Math.abs(unit1.getZ() - unit2.getZ());
    }

    /**
     * Finds the longest path in a tunnel system, starting from a given tunnel unit.
     * This method uses depth-first search (DFS) to traverse the adjacency graph
     * of the tunnel units and identifies the longest path.
     *
     * @param start the starting TunnelUnit for the pathfinding algorithm; must not be null
     * @param adjacencyMap a map representing the adjacency list of the tunnel system;
     *                     keys are TunnelUnit instances, values are lists of adjacent TunnelUnit instances
     * @return a list of TunnelUnit instances representing the longest path found from the starting point
     */
    private static List<TunnelUnit> findLongestPathFrom(@NotNull TunnelUnit start,
                                                        @NotNull Map<TunnelUnit, List<TunnelUnit>> adjacencyMap) {
        Set<TunnelUnit> visited = new HashSet<>();
        List<TunnelUnit> currentPath = new ArrayList<>();
        List<TunnelUnit> longestPath = new ArrayList<>();

        dfs(start, adjacencyMap, visited, currentPath, longestPath);

        return longestPath;
    }

    /**
     * Performs a depth-first search (DFS) to explore paths in a tunnel system,
     * keeping track of the current path and updating the longest path found.
     *
     * @param current the current TunnelUnit being visited in the DFS traversal
     * @param adjacencyMap a map representing the adjacency list of the tunnel units
     *                     where each TunnelUnit points to its list of neighbors
     * @param visited a set of TunnelUnits that have already been visited during the search
     * @param currentPath a list representing the current path being traversed
     * @param longestPath a list representing the longest path observed during traversal
     */
    private static void dfs(@NotNull TunnelUnit current,
                            @NotNull Map<TunnelUnit, List<TunnelUnit>> adjacencyMap,
                            @NotNull Set<TunnelUnit> visited,
                            @NotNull List<TunnelUnit> currentPath,
                            @NotNull List<TunnelUnit> longestPath) {

        visited.add(current);
        currentPath.add(current);

        // Update the longest path if the current path is longer
        if (currentPath.size() > longestPath.size()) {
            longestPath.clear();
            longestPath.addAll(currentPath);
        }

        // Explore all unvisited neighbors
        for (TunnelUnit neighbor : adjacencyMap.get(current)) {
            if (!visited.contains(neighbor)) {
                dfs(neighbor, adjacencyMap, visited, currentPath, longestPath);
            }
        }

        // Backtrack
        visited.remove(current);
        currentPath.removeLast();
    }

    /**
     * Calculates the Manhattan distance between the first and the last units in a given path.
     * If the path is null or contains fewer than two elements, the distance is considered zero.
     *
     * @param path the list of {@code TunnelUnit} instances representing the path.
     *             The path must contain at least two units to calculate the distance.
     * @return the Manhattan distance between the first and the last units in the path,
     *         or 0 if the path is null or contains fewer than two units.
     */
    @Range(from = 0, to = Integer.MAX_VALUE)
    public static int calculatePathDistance(@NotNull List<TunnelUnit> path) {
        if (path.size() < 2) {
            return 0;
        }

        TunnelUnit start = path.getFirst();
        TunnelUnit end = path.getLast();
        return manhattanDistance(start, end);
    }
}
