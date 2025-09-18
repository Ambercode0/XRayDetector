package com.ambercode.data.manager;

import com.ambercode.data.*;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public final class VeinManager {
    private static final int NEIGHBOR_RADIUS = 1;

    private final Registry registry;

    public VeinManager(Registry registry) {
        this.registry = registry;
    }

    /**
     * Assign the provided unit to a vein, creating or merging veins as needed.
     * Returns the final vein id.
     */
    public int assignUnitToVein(TunnelUnit unit) {
        ChunkKey chunkKey = Registry.chunkKeyFromPos(unit.pos);
        Set<Integer> neighboringVeinIds = collectNeighboringVeinIds(chunkKey, unit.materialId);

        int assignedVeinId;
        if (neighboringVeinIds.isEmpty()) {
            assignedVeinId = createAndInitializeVein(unit, chunkKey);
        } else {
            Iterator<Integer> veinIdIterator = neighboringVeinIds.iterator();
            int primaryVeinId = veinIdIterator.next();
            OreVein primaryVein = registry.getVein(primaryVeinId);
            attachUnitToVein(primaryVein, chunkKey, unit);
            registry.registerVeinInChunk(primaryVeinId, chunkKey);
            assignedVeinId = primaryVeinId;

            mergeVeinsIntoPrimary(primaryVeinId, neighboringVeinIds);
        }

        unit.veinId = assignedVeinId;
        return assignedVeinId;
    }

    private Set<Integer> collectNeighboringVeinIds(ChunkKey center, int materialId) {
        Set<Integer> result = new HashSet<>();
        for (int dx = -NEIGHBOR_RADIUS; dx <= NEIGHBOR_RADIUS; dx++) {
            for (int dz = -NEIGHBOR_RADIUS; dz <= NEIGHBOR_RADIUS; dz++) {
                ChunkKey neighborKey = ChunkKey.of(center.cx + dx, center.cz + dz);
                ChunkIndex index = registry.getOrCreateChunk(neighborKey);
                for (Integer vid : index.getVeinSet()) {
                    OreVein vein = registry.getVein(vid);
                    if (vein != null && vein.materialId == materialId) {
                        result.add(vid);
                    }
                }
            }
        }
        return result;
    }

    private int createAndInitializeVein(TunnelUnit unit, ChunkKey chunkKey) {
        OreVein newVein = registry.createVein(unit.materialId);
        registry.registerVeinInChunk(newVein.id, chunkKey);
        attachUnitToVein(newVein, chunkKey, unit);
        return newVein.id;
    }

    private void attachUnitToVein(OreVein vein, ChunkKey chunkKey, TunnelUnit unit) {
        vein.addUnit(chunkKey, unit.id, unit.isExposedToAir);
    }

    private void mergeVeinsIntoPrimary(int primaryVeinId, Set<Integer> neighboringVeinIds) {
        OreVein primary = registry.getVein(primaryVeinId);
        if (primary == null) return;

        for (Integer otherVeinId : neighboringVeinIds) {
            if (otherVeinId == primaryVeinId) continue;

            OreVein other = registry.getVein(otherVeinId);
            if (other == null) continue;

            primary.mergeFrom(other);
            reassignUnitsToVein(other, primaryVeinId);
            registry.getVeins().remove(otherVeinId);
            registerPrimaryVeinInChunks(primaryVeinId, other);
        }
    }

    private void reassignUnitsToVein(OreVein fromVein, int targetVeinId) {
        for (ChunkKey ck : fromVein.getChunks()) {
            for (int unitId : fromVein.unitsInChunk(ck)) {
                TunnelUnit tu = registry.getUnit(unitId);
                if (tu != null) {
                    tu.veinId = targetVeinId;
                }
            }
        }
    }

    private void registerPrimaryVeinInChunks(int primaryVeinId, OreVein mergedVein) {
        for (ChunkKey ck : mergedVein.getChunks()) {
            registry.registerVeinInChunk(primaryVeinId, ck);
        }
    }

}