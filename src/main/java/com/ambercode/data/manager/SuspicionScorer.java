package com.ambercode.data.manager;

import com.ambercode.data.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public final class SuspicionScorer {

    private static final double SUSPICIOUS_TIME_INTERVAL_THRESHOLD = 30_000;
    private static final double SUSPICIOUS_TIME_INTERVAL_WEIGHT = 0.25;

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

    private static final double SUSPICIOUS_DENSITY_WEIGHT = 0.25;
    private static final double SUSPICIOUS_DENSITY_THRESHOLD = 0.02; // 2% is suspicious
    private static final double NORMAL_DENSITY = 0.005; // 0.5% is typical for legitimate mining
    private static final double MAX_DENSITY = 0.05; // 5% would be extremely suspicious

    public static double computeDensityScore(@NotNull Miner miner) {
        Deque<TunnelUnit> units = miner.getTunnelQueue();
        if (units.size() <= 100) return ScoreErrorCodes.NOT_ENOUGH_DATA.getCode();

        long totalBlocks = units.stream().filter(unit -> !unit.isExposed()).count();

        if (totalBlocks == 0) return ScoreErrorCodes.NOT_ENOUGH_DATA.getCode();

        double oreCount = units.stream()
                .filter(unit -> !unit.isExposed())
                .filter(unit -> Utils.isPreciousOre(unit.getMaterial()))
                .count();

        double density = oreCount / totalBlocks;

        System.out.println("Ore Count: " + oreCount + " Total Blocks: " + totalBlocks + " Density: " + density + ".");

        // The score is 0 below normal density, scales linearly above a threshold
        if (density <= NORMAL_DENSITY) {
            return 0.0;
        } else if (density >= SUSPICIOUS_DENSITY_THRESHOLD) {
            // Start applying weight only above a suspicious threshold
            double suspiciousRange = MAX_DENSITY - SUSPICIOUS_DENSITY_THRESHOLD;
            return Math.min(1.0, (density - SUSPICIOUS_DENSITY_THRESHOLD) / suspiciousRange);
        } else {
            // Between normal and suspicious - minor penalty
            return 0.3 * (density - NORMAL_DENSITY) / (SUSPICIOUS_DENSITY_THRESHOLD - NORMAL_DENSITY);
        }
    }

    private static final double SUSPICIOUS_DISTANCE_WEIGHT = 0.50;
    private static final double SUSPICIOUS_DISTANCE_THRESHOLD = 13;

    public static double computeDistanceScore(@NotNull Miner miner) {
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
        Deque<Integer> distances = new ArrayDeque<>(sortedVeins.size() - 1);

        OreVein prevVein = it.next();
        while (it.hasNext()) {
            OreVein currVein = it.next();
            int distance = Utils.manhattanDistance(prevVein.getTunnelUnits().getFirst(), currVein.getTunnelUnits().getFirst());
            distances.add(distance);
            prevVein = currVein;
        }

        double distAvg = distances.stream().mapToInt(Integer::intValue).average().orElse(ScoreErrorCodes.NOT_ENOUGH_DATA.getCode());
        System.out.println("Average Distance: " + distAvg);

        return Math.min(1.0 , 1.0 - (distAvg / SUSPICIOUS_DISTANCE_THRESHOLD));
    }

    // --------------------------

    // Weights (should sum to 1.0)
    private static final double TIME_INTERVAL_WEIGHT = 0.25;
    private static final double DENSITY_WEIGHT = 0.25;
    private static final double DISTANCE_WEIGHT = 0.50;

    // Threshold constants
    private static final double CHEATER_THRESHOLD = 0.85; // >85% probability of cheating
    private static final double SUSPICIOUS_THRESHOLD = 0.65; // >65% suspicious

    /**
     * Combines all detection methods into a unified cheating probability score [0,1]
     */
    public static double computeCombinedCheatingProbability(@NotNull Miner miner) {
        List<Double> scores = new ArrayList<>();
        List<Double> weights = new ArrayList<>();

        // Time interval score
        double timeScore = computeTimeIntervalScore(miner);
        if (timeScore >= 0) {
            scores.add(timeScore);
            weights.add(TIME_INTERVAL_WEIGHT);
        }

        // Density score
        double densityScore = computeDensityScore(miner);
        if (densityScore >= 0) {
            scores.add(densityScore);
            weights.add(DENSITY_WEIGHT);
        }

        // Distance score
        double distanceScore = computeDistanceScore(miner);
        if (distanceScore >= 0) {
            scores.add(distanceScore);
            weights.add(DISTANCE_WEIGHT);
        }

        if (scores.isEmpty()) {
            return ScoreErrorCodes.NOT_ENOUGH_DATA.getCode();
        }

        // Normalize weights in case some scores were unavailable
        double totalWeight = weights.stream().mapToDouble(Double::doubleValue).sum();
        if (totalWeight == 0) return 0.0;

        weights.replaceAll(aDouble -> aDouble / totalWeight);

        return weightedCombination(scores, weights);
    }

    /**
     * Mathematical combination using weighted power mean for better statistical properties
     */
    private static double weightedCombination(List<Double> scores, List<Double> weights) {
        // Method 1: Weighted quadratic mean (emphasizes high scores)
        double quadraticSum = 0.0;
        for (int i = 0; i < scores.size(); i++) {
            quadraticSum += weights.get(i) * Math.pow(scores.get(i), 2);
        }
        double quadraticMean = Math.sqrt(quadraticSum);

        // Method 2: Weighted arithmetic mean (balanced)
        double arithmeticMean = 0.0;
        for (int i = 0; i < scores.size(); i++) {
            arithmeticMean += weights.get(i) * scores.get(i);
        }

        // Method 3: Maximum score (most sensitive)
        double maxScore = scores.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);

        // Combine methods: Use quadratic mean but bias toward arithmetic when scores are moderate
        if (maxScore > 0.8) {
            // High suspicion: trust the quadratic mean more
            return 0.7 * quadraticMean + 0.3 * arithmeticMean;
        } else {
            // Moderate scores: balanced approach
            return 0.4 * quadraticMean + 0.6 * arithmeticMean;
        }
    }

    /**
     * Enhanced statistical method using Bayesian probability
     */
    public static double computeBayesianProbability(@NotNull Miner miner) {
        double timeScore = Math.max(0, computeTimeIntervalScore(miner));
        double densityScore = Math.max(0, computeDensityScore(miner));
        double distanceScore = Math.max(0, computeDistanceScore(miner));

        // Prior probability of cheating (base rate - adjust based on your server)
        double prior = 0.01; // 1% base cheating rate assumption

        // Likelihood ratios for each detector (calibrate these based on testing)
        double timeLikelihood = scoreToLikelihoodRatio(timeScore, 3.0);
        double densityLikelihood = scoreToLikelihoodRatio(densityScore, 4.0);
        double distanceLikelihood = scoreToLikelihoodRatio(distanceScore, 5.0);

        // Bayesian combination
        double combinedOdds = prior / (1 - prior);
        combinedOdds *= timeLikelihood * densityLikelihood * distanceLikelihood;

        return combinedOdds / (1 + combinedOdds);
    }

    /**
     * Convert detector score to likelihood ratio
     * @param score detector score [0,1]
     * @param maxRatio maximum likelihood ratio for strong evidence
     */
    private static double scoreToLikelihoodRatio(double score, double maxRatio) {
        if (score <= 0.3) return 0.5; // Evidence against cheating
        if (score <= 0.6) return 1.0; // Neutral evidence
        return 1.0 + (maxRatio - 1.0) * ((score - 0.6) / 0.4); // Scale up to maxRatio
    }

    /**
     * Determines if a score indicates >99% probability of cheating
     */
    public static boolean isAlmostCertainlyCheating(double probabilityScore) {
        // For >99% certainty, we need extremely strong evidence
        return probabilityScore >= 0.95; // Calibrate this based on your false positive tolerance
    }

    /**
     * Get confidence level description
     */
    public static String getConfidenceLevel(double probabilityScore) {
        if (probabilityScore < 0.3) return "LIKELY_LEGITIMATE";
        if (probabilityScore < 0.6) return "UNCERTAIN";
        if (probabilityScore < 0.8) return "SUSPICIOUS";
        if (probabilityScore < 0.95) return "LIKELY_CHEATING";
        return "ALMOST_CERTAINLY_CHEATING";
    }
}
