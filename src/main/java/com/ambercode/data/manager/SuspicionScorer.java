package com.ambercode.data.manager;

import com.ambercode.data.Miner;
import com.ambercode.data.OreVein;
import com.ambercode.data.ScoreErrorCodes;
import com.ambercode.data.TunnelUnit;
import org.jetbrains.annotations.NotNull;

import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public final class SuspicionScorer {


    private static final double SUSPICIOUS_TIME_INTERVAL_THRESHOLD = 30_000;

    public static double computeTimeIntervalScore(@NotNull Miner miner) {
        long timeDiffSum = 0L;
        Deque<OreVein> veins = miner.getVeinDeque();

        if (veins.size() <= 10) return ScoreErrorCodes.NOT_ENOUGH_DATA.getCode();

        List<OreVein> sortedVeins = veins.stream()
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
}
