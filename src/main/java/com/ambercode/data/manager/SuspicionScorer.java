package com.ambercode.data.manager;

import com.ambercode.data.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Iterator;
import java.util.UUID;

/**
 * Converts miner and vein stats into a normalized suspicion score in [0,1].
 * Uses a linear combination of normalized features; designed for easy tuning.
 */
public final class SuspicionScorer {

    // Feature weights (tune based on your server data)
    private static final double WEIGHT_EXPOSURE = 0.35;
    private static final double WEIGHT_ORE_DENSITY = 0.25;
    private static final double WEIGHT_MEDIAN_INTERVAL = 0.20;
    private static final double WEIGHT_VEIN_COVERAGE = 0.12;
    private static final double WEIGHT_PATH_STRAIGHTNESS = 0.08; // if tunnel analysis performed

    // Normalization parameters
    private static final double EXPOSURE_SUSPICIOUS_THRESHOLD = 0.60; // treat <60% exposure as suspicious
    private static final double ORE_DENSITY_BASELINE = 0.0125;          // 2%
    private static final double ORE_DENSITY_SUSPICIOUS = 0.0875;        // 8,75%
    private static final double INTERVAL_FAST_SECONDS = 5.0;          // 5s -> 1.0
    private static final double INTERVAL_SLOW_SECONDS = 60.0;         // 60s -> 0.0
    private static final int VEIN_COVERAGE_SAMPLE_SIZE = 40;
    private static final double VEIN_COVERAGE_SUSPICIOUS = 0.50;      // <50% coverage suspicious

    private final Registry registry;

    public SuspicionScorer(Registry registry) {
        this.registry = registry;
    }

    /**
     * Compute a score for a miner in the recent window represented by a Miner object.
     * This method computes contributions for each feature and returns a weighted sum.
     */
    public double score(Miner miner) {
        double exposureContribution = normalizeExposure(miner.getExposedRate());
        double densityContribution = normalizeOreDensity(miner.getOreDensity());
        double intervalContribution = normalizeInterval(miner.getLastOreIntervalMs());
        double veinCoverageContribution = normalizeVeinCoverage(miner);
        double pathContribution = 0.0; // path analysis requires structures; leave 0 if unavailable

        double combined =
                WEIGHT_EXPOSURE * exposureContribution
                        + WEIGHT_ORE_DENSITY * densityContribution
                        + WEIGHT_MEDIAN_INTERVAL * intervalContribution
                        + WEIGHT_VEIN_COVERAGE * veinCoverageContribution
                        + WEIGHT_PATH_STRAIGHTNESS * pathContribution;
        
        Player player = Bukkit.getPlayer(miner.playerId);
        if (player != null && player.isOnline()) {
            player.sendMessage("§7[§6XRay§7] §fScore details:");
            player.sendMessage(String.format("§7- Exposure: §f%.2f", exposureContribution));
            player.sendMessage(String.format("§7- Density: §f%.2f", densityContribution));
            player.sendMessage(String.format("§7- Interval: §f%.2f", intervalContribution));
            player.sendMessage(String.format("§7- Vein Coverage: §f%.2f", veinCoverageContribution)); // TODO: implement
            player.sendMessage(String.format("§7- Path: §f%.2f", pathContribution)); // TODO: implement
            player.sendMessage(String.format("§7- Combined: §f%.2f", combined));
        }

        return clamp01(combined);
    }

    /**
     * Exposure normalization: exposed rate close to 1 means safe -> contribution 0.
     * We invert it so low exposure -> 1.0 contribution.
     */
    private double normalizeExposure(double exposedRate) {
        double invertedExposure = 1.0 - exposedRate; // 0 if fully exposed, 1 if never exposed
        return clamp01((invertedExposure - 0.0) / (EXPOSURE_SUSPICIOUS_THRESHOLD - 0.0));
    }

    private double normalizeOreDensity(double density) {
        // assume a reasonable baseline 0.02 (2%), treat >=0.1 (10%) as highly suspicious
        return clamp01((density - ORE_DENSITY_BASELINE) / (ORE_DENSITY_SUSPICIOUS - ORE_DENSITY_BASELINE));
    }

    private double normalizeInterval(double millis) {
        if (Double.isNaN(millis) || millis <= 0) return 0.0; // no ores -> no contribution
        double seconds = millis / 1000.0;
        // faster intervals -> higher contribution. 5s -> 1.0, 60s -> 0.0
        return clamp01((INTERVAL_SLOW_SECONDS - seconds) / (INTERVAL_SLOW_SECONDS - INTERVAL_FAST_SECONDS));
    }

    private double normalizeVeinCoverage(Miner miner) {
        // Compute average vein coverage for the miner's recent ores
        // For brevity, approximate by sampling last N units and checking their vein's coverage
        int checked = 0;
        double accumCoverage = 0.0;

        Iterator<Integer> it = miner.getRecentUnitIds().descendingIterator();
        while (it.hasNext() && checked < VEIN_COVERAGE_SAMPLE_SIZE) {
            TunnelUnit unit = registry.getUnit(it.next());
            if (unit == null) continue;
            if (!miner.isCommonOreMaterial(unit.materialId)) continue;

            OreVein vein = registry.getVein(unit.veinId);
            if (vein == null) continue;

            double minerUnitsInVein = countUnitsMinedInVeinByMiner(vein, miner.playerId);
            double coverage = vein.totalCount == 0 ? 0.0 : minerUnitsInVein / vein.totalCount;

            accumCoverage += coverage;
            checked++;
        }

        if (checked == 0) return 0.0; // no data => not suspicious

        double avgCoverage = accumCoverage / checked; // 0..1 (1 => fully mined)
        // lower coverage -> more suspicious
        return clamp01((VEIN_COVERAGE_SUSPICIOUS - avgCoverage) / VEIN_COVERAGE_SUSPICIOUS);
    }

    /**
     * Counts the number of ore units within a specified vein that were mined by a
     * specific player, identified by their UUID.
     *
     * @param vein the OreVein object representing the vein to analyze
     * @param playerId the UUID of the miner whose mined units are to be counted
     * @return the total count of ore units mined by the specified player in the given vein
     */
    private int countUnitsMinedInVeinByMiner(OreVein vein, UUID playerId) {
        int count = 0;
        for (final ChunkKey chunkKey : vein.getChunks()) {
            for (final int unitId : vein.unitsInChunk(chunkKey)) {
                final TunnelUnit unit = registry.getUnit(unitId);
                if (unit != null && playerId.equals(unit.minerId)) count++;
            }
        }
        return count;
    }

    private static double clamp01(final double x) {
        return Math.max(0.0, Math.min(1.0, x));
    }
}
