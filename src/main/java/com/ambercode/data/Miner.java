package com.ambercode.data;

import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Per-player state for sliding-window features and aggregates.
 * This keeps only IDs (not full Unit objects) to reduce memory.
 */
public final class Miner {
    private static final double EWMA_ALPHA = 0.25; // weight for new interval samples
    private static final int MIN_ORE_MATERIAL_ID = 1000; // example threshold for ore materials

    public final UUID playerId;
    // time-ordered deque of recent unit IDs (trim by time or size)
    private final Deque<Integer> recentUnitIds = new ConcurrentLinkedDeque<>();
    // material counts for quick density calculations
    private final Map<Integer, Integer> materialCounts = new HashMap<>();
    private final AtomicInteger totalBlocks = new AtomicInteger(0);
    private volatile long lastUpdateMs = System.currentTimeMillis();

    // derived features cached for quick scoring
    volatile double lastOreIntervalMs = Double.NaN; // average/median candidate
    volatile long lastOreTimestamp = -1L;
    volatile int oresMined = 0;
    volatile int exposedOres = 0;

    public Miner(UUID playerId) {
        this.playerId = playerId;
    }

    /**
     * Record a newly mined unit (id) with material and whether it was exposed.
     * This method is designed to be cheap; trimming performed by caller or periodic task.
     */
    public void recordUnit(int unitId, int materialId, boolean isOre, boolean exposed, long minedAt) {
        recentUnitIds.addLast(unitId);
        totalBlocks.incrementAndGet();
        materialCounts.merge(materialId, 1, Integer::sum);
        if (isOre) {
            updateOreStats(exposed, minedAt);
        }
        lastUpdateMs = System.currentTimeMillis();
    }

    public double getOreDensity() {
        int total = totalBlocks.get();
        if (total == 0) return 0.0;
        return oresMined / (double) total;
    }

    // Prefer this clearer name; keep old accessor for compatibility.
    public double getExposureRate() {
        int ores = oresMined;
        if (ores == 0) return 1.0; // if none ores, consider as non-suspicious for exposure
        return exposedOres / (double) ores;
    }

    // ... existing code ...
    public double getExposedRate() {
        return getExposureRate();
    }

    public int getTotalBlocks() {
        return totalBlocks.get();
    }

    public void trimOlderThan(long cutoffMs, Registry registry) {
        // Remove units older than cutoff (simple approach: pop from left while older)
        while (!recentUnitIds.isEmpty()) {
            final Integer id = recentUnitIds.peekFirst();
            if (id == null) break;
            TunnelUnit u = registry.getUnit(id);
            if (u == null) {
                recentUnitIds.removeFirst();
                continue;
            }
            if (u.minedAt < cutoffMs) {
                recentUnitIds.removeFirst();
                decrementCountsFor(u);
            } else break;
        }
    }

    public double getLastOreIntervalMs() {
        return lastOreIntervalMs;
    }

    private static boolean isNullOrNonPositive(Integer v) {
        return v == null || v <= 0;
    }

    public Deque<Integer> getRecentUnitIds() {
        return recentUnitIds;
    }

    // Example helper; replace it with your plugin's ore ID set.
    public boolean isOreMaterial(int materialId) {
        return materialId >= MIN_ORE_MATERIAL_ID;
    }

    // ------- Extracted helpers for clarity and reuse -------

    private void updateOreStats(boolean exposed, long minedAt) {
        oresMined++;
        if (exposed) exposedOres++;
        if (lastOreTimestamp > 0) {
            long interval = minedAt - lastOreTimestamp;
            lastOreIntervalMs = computeEwma(lastOreIntervalMs, interval, EWMA_ALPHA);
        }
        lastOreTimestamp = minedAt;
    }

    private static double computeEwma(double current, long sample, double alpha) {
        double s = (double) sample;
        if (Double.isNaN(current)) {
            return s;
        }
        // EWMA: new = (1 - alpha) * current + alpha * sample
        return (1.0d - alpha) * current + alpha * s;
    }

    private void decrementCountsFor(TunnelUnit u) {
        totalBlocks.decrementAndGet();
        materialCounts.merge(u.materialId, -1, Integer::sum);
        if (isNullOrNonPositive(materialCounts.get(u.materialId))) materialCounts.remove(u.materialId);
        if (isOreMaterial(u.materialId)) {
            oresMined = Math.max(0, oresMined - 1);
            if (u.isExposedToAir) exposedOres = Math.max(0, exposedOres - 1);
        }
    }
}