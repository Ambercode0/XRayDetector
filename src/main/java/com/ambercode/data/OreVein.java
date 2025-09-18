package com.ambercode.data;

import java.util.*;

/**
 * OreVein represents a connected component of ore units. It stores unit ids
 * partitioned by chunk to keep memory and iteration efficient.
 */
public final class OreVein {
    public final int id;
    public final int materialId;
    private final Map<ChunkKey, List<Integer>> unitsByChunk = new HashMap<>();

    // aggregates
    public volatile int totalCount = 0;
    public volatile int exposedCount = 0;
    public volatile boolean isExposedToAir = false;
    public volatile long createdAt = System.currentTimeMillis();
    public volatile long lastUpdatedAt = createdAt;

    public OreVein(int id, int materialId) {
        this.id = id;
        this.materialId = materialId;
    }

    public synchronized void addUnit(ChunkKey ck, int unitId, boolean exposed) {
        unitsByChunk.computeIfAbsent(ck, k -> new ArrayList<>()).add(unitId);
        totalCount++;
        if (exposed) exposedCount++;
        if (exposed) isExposedToAir = true;
        lastUpdatedAt = System.currentTimeMillis();
    }

    public synchronized void mergeFrom(OreVein other) {
        for (Map.Entry<ChunkKey, List<Integer>> e : other.unitsByChunk.entrySet()) {
            unitsByChunk.computeIfAbsent(e.getKey(), k -> new ArrayList<>()).addAll(e.getValue());
        }
        totalCount += other.totalCount;
        exposedCount += other.exposedCount;
        isExposedToAir = isExposedToAir || other.isExposedToAir;
        lastUpdatedAt = System.currentTimeMillis();
    }

    /**
     * Retrieves a list of unit IDs associated with the specified chunk key.
     * If no units are associated with the given chunk, an empty list is returned.
     *
     * @param ck the ChunkKey for which to retrieve the associated unit IDs
     * @return a List of Integer IDs of units associated with the specified chunk, or an empty list if none exist
     */
    public synchronized List<Integer> unitsInChunk(ChunkKey ck) {
        return unitsByChunk.getOrDefault(ck, Collections.emptyList());
    }

    /**
     * Retrieves a set of all chunk keys currently associated with their respective unit identifiers in the ore vein.
     * This method provides a snapshot of the chunks containing ore units at the time of invocation.
     *
     * @return a set of ChunkKey objects representing the chunks associated with this ore vein
     */
    public synchronized Set<ChunkKey> getChunks() { return new HashSet<>(unitsByChunk.keySet()); }
}