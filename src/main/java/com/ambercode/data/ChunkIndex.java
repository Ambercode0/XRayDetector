package com.ambercode.data;

import java.util.*;

public final class ChunkIndex {
    public final ChunkKey key;

    // map from packed position to unitId for O(1) neighbor lookups
    private final Map<PackedBlockPos, Integer> unitPosMap = new HashMap<>();
    // set of vein ids intersecting chunk
    private final Set<Integer> veinSet = new HashSet<>();
    // material counts for quick heuristics (materialId -> count)
    private final Map<Integer, Integer> materialCounts = new HashMap<>();

    public ChunkIndex(ChunkKey key) { this.key = key; }

    public void putUnit(PackedBlockPos pos, int unitId, int materialId) {
        unitPosMap.put(pos, unitId);
        materialCounts.merge(materialId, 1, Integer::sum);
    }

    public Integer getUnitAt(PackedBlockPos pos) { return unitPosMap.get(pos); }

    public void removeUnit(PackedBlockPos pos) {
        Integer id = unitPosMap.remove(pos);
        // caller should handle materialCounts decrement if desired
    }

    public Set<Integer> getVeinSet() {
        return veinSet;
    }

    public void addVein(int veinId) { veinSet.add(veinId); }
    public void removeVein(int veinId) { veinSet.remove(veinId); }

    public Map<Integer, Integer> getMaterialCounts() { return Collections.unmodifiableMap(materialCounts); }
}