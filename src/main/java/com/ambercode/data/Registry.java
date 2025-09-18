package com.ambercode.data;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.ambercode.data.IdGenerator.VEIN_ID_GEN;

// ... existing code ...
public final class Registry {

    private static final int CHUNK_SIZE = 16;

    private final Map<Integer, TunnelUnit> units = new ConcurrentHashMap<>();
    private final Map<UUID, Miner> miners = new ConcurrentHashMap<>();
    private final Map<Integer, OreVein> veins = new ConcurrentHashMap<>();
    private final Map<Integer, TunnelStructure> structures = new ConcurrentHashMap<>();
    private final Map<ChunkKey, ChunkIndex> chunkIndices = new ConcurrentHashMap<>();

    @NotNull
    public TunnelUnit putUnit(int unitId, TunnelUnit unit) {
        getOrCreateChunkByPos(unit.pos).putUnit(unit.pos, unitId, unit.materialId);
        units.put(unitId, unit);
        return unit;
    }

    public Map<Integer, OreVein> getVeins() {
        return veins;
    }

    public TunnelUnit getUnit(int unitId) {
        return units.get(unitId);
    }

    public Miner getOrCreateMiner(UUID playerId) {
        return miners.computeIfAbsent(playerId, Miner::new);
    }

    public OreVein createVein(int materialId) {
        int veinId = VEIN_ID_GEN.next();
        OreVein vein = new OreVein(veinId, materialId);
        veins.put(veinId, vein);
        return vein;
    }

    public OreVein getVein(int veinId) {
        return veins.get(veinId);
    }

    public void registerVeinInChunk(int veinId, ChunkKey ck) {
        chunkIndices.computeIfAbsent(ck, ChunkIndex::new).addVein(veinId);
    }

    public ChunkIndex getOrCreateChunk(ChunkKey ck) {
        return chunkIndices.computeIfAbsent(ck, ChunkIndex::new);
    }

    public static ChunkKey chunkKeyFromPos(PackedBlockPos pos) {
        int x = pos.getX();
        int z = pos.getZ();
        int cx = Math.floorDiv(x, CHUNK_SIZE);
        int cz = Math.floorDiv(z, CHUNK_SIZE);
        return ChunkKey.of(cx, cz);
    }

    // Helper to create/access chunk index based on a block position
    private ChunkIndex getOrCreateChunkByPos(PackedBlockPos pos) {
        return getOrCreateChunk(chunkKeyFromPos(pos));
    }

}