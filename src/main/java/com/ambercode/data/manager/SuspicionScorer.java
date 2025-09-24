package com.ambercode.data.manager;

import com.ambercode.data.*;
import org.jetbrains.annotations.NotNull;

import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class SuspicionScorer {


    private static final double SUSPICIOUS_TIME_INTERVAL_THRESHOLD = 30_000;
    private static final double SUSPICIOUS_TIME_INTERVAL_WEIGHT = 0.60;

    public static double computeTimeIntervalScore(@NotNull Miner miner) {
        long timeDiffSum = 0L;
        Deque<OreVein> veins = miner.getVeinDeque();

        if (veins.size() <= 10) return ScoreErrorCodes.NOT_ENOUGH_DATA.getCode();

        final List<OreVein> sortedVeins = veins.stream()
                .filter(o -> !o.getTunnelUnits().getFirst().isExposed())
                .sorted((a,b) -> {
            TunnelUnit aUnit = a.getTunnelUnits().getFirst();
            TunnelUnit bUnit = b.getTunnelUnits().getFirst();
            long aUnitTime = aUnit.getMinedAt();
            long bUnitTime = bUnit.getMinedAt();
            return Long.compare(aUnitTime, bUnitTime);
        }).collect(Collectors.toUnmodifiableList());

        if (sortedVeins.size() <= 10) return ScoreErrorCodes.NOT_ENOUGH_DATA.getCode();

        Iterator<OreVein> it = sortedVeins.iterator();
        OreVein prevVein = it.next();
        while (it.hasNext()) {
            OreVein currVein = it.next();
            long currVeinTime = currVein.getTunnelUnits().getFirst().getMinedAt();
            long prevVeinTime = prevVein.getTunnelUnits().getFirst().getMinedAt();
            long timeDiff = (long) Math.max(0.0, currVeinTime - prevVeinTime);
            timeDiffSum += timeDiff;
            prevVein = currVein;
        }

        double avgTimeDiff = (double) timeDiffSum / (sortedVeins.size()-1);

        return 1.0 - Math.min(1.0, avgTimeDiff / SUSPICIOUS_TIME_INTERVAL_THRESHOLD);
    }

    private static final double SUSPICIOUS_DENSITY_WEIGHT = 0.40;
    private static final double SUSPICIOUS_DENSITY_THRESHOLD = 0.0125;

    public static double computeDensityScore(@NotNull Miner miner) {
        double densitySum = 0.0;
        Deque<TunnelUnit> units = miner.getTunnelQueue();
        if (units.size() <= 30) return ScoreErrorCodes.NOT_ENOUGH_DATA.getCode();

        long totalBlocks = units.stream().filter(unit -> !unit.isExposed()).count();

        if (totalBlocks == 0) return ScoreErrorCodes.NOT_ENOUGH_DATA.getCode();

        double oreCount = units.stream()
                .filter(unit -> !unit.isExposed())
                .filter(unit -> Utils.isPreciousOre(unit.getMaterial()))
                .count();

        double density = oreCount / totalBlocks;

        return Math.min(1.0, density / SUSPICIOUS_DENSITY_THRESHOLD);
    }
}
