package com.ambercode.utils;

import com.ambercode.data.TunnelUnit;

import java.util.*;

public class TunnelPathFinder {

    public static List<TunnelUnit> findLongestPath(List<TunnelUnit> tunnelUnits) {
        if (tunnelUnits == null || tunnelUnits.isEmpty()) {
            return new ArrayList<>();
        }

        if (tunnelUnits.size() == 1) {
            return new ArrayList<>(tunnelUnits);
        }

        // Build adjacency map for quick neighbor lookup
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

    private static Map<TunnelUnit, List<TunnelUnit>> buildAdjacencyMap(List<TunnelUnit> tunnelUnits) {
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

    private static int manhattanDistance(TunnelUnit unit1, TunnelUnit unit2) {
        return Math.abs(unit1.getX() - unit2.getX()) + Math.abs(unit1.getZ() - unit2.getZ());
    }

    private static List<TunnelUnit> findLongestPathFrom(TunnelUnit start,
                                                        Map<TunnelUnit, List<TunnelUnit>> adjacencyMap) {
        Set<TunnelUnit> visited = new HashSet<>();
        List<TunnelUnit> currentPath = new ArrayList<>();
        List<TunnelUnit> longestPath = new ArrayList<>();

        dfs(start, adjacencyMap, visited, currentPath, longestPath);

        return longestPath;
    }

    private static void dfs(TunnelUnit current,
                            Map<TunnelUnit, List<TunnelUnit>> adjacencyMap,
                            Set<TunnelUnit> visited,
                            List<TunnelUnit> currentPath,
                            List<TunnelUnit> longestPath) {

        visited.add(current);
        currentPath.add(current);

        // Update the longest path if current path is longer
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

    // Utility method to calculate the Manhattan distance of a path
    public static int calculatePathDistance(List<TunnelUnit> path) {
        if (path == null || path.size() < 2) {
            return 0;
        }

        TunnelUnit start = path.getFirst();
        TunnelUnit end = path.getLast();
        return manhattanDistance(start, end);
    }
}
