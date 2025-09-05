package com.ambercode.listeners;

import com.ambercode.data.*;
import com.ambercode.manager.PlayerDataManager;
import com.ambercode.utils.Utils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BlockBreakEventListener implements Listener {

    private final PlayerDataManager playerDataManager;

    public BlockBreakEventListener(PlayerDataManager playerDataManager) {
        this.playerDataManager = playerDataManager;
    }

    @EventHandler()
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Location blockLocation = block.getLocation();
        Player player = event.getPlayer();

        if (blockLocation.getBlockY() > 16 && blockLocation.getWorld().getEnvironment() == World.Environment.NORMAL) { // there's no diamond naturally spawning above 16
            return;
        }

        if ((blockLocation.getBlockY() > 119 || blockLocation.getBlockY() < 6) && blockLocation.getWorld().getEnvironment() == World.Environment.NETHER) {
            return;
        }

        if (blockLocation.getWorld().getEnvironment() == World.Environment.THE_END) {
            return;
        }

        UUID userUUID = player.getUniqueId();
        Miner miner = playerDataManager.getMinerMap().computeIfAbsent(userUUID, Miner::new);
        List<Tunnel> minerTunnels = playerDataManager.getMinerTunnelMap().computeIfAbsent(miner, v -> new ArrayList<>());

        Material blockMaterial = block.getType();

        if (blockMaterial != Material.STONE &&
                blockMaterial != Material.DEEPSLATE &&
                blockMaterial != Material.NETHERRACK &&
                blockMaterial != Material.GRAVEL &&
                !Utils.isOre(block)) {
            return;
        }

        // Add block to miner's history
        miner.getMinedBlocks().add(block);

        // Check if this is an ore block
        if (Utils.isOre(block)) {
            handleOreDiscovery(miner, block, blockLocation);
        }

        if (minerTunnels.isEmpty()) {
            minerTunnels.add(new Tunnel(new Structure(new Path(block)), miner));
            return;
        }

        boolean addedToExistingTunnel = false;

        for (Tunnel tunnel : minerTunnels) {
            for (Unit unit : tunnel.getStructure().getPath().getUnits()) {
                Location unitLocation = unit.getLocation();

                if (unitLocation.getBlockX() == blockLocation.getBlockX() &&
                        unitLocation.getBlockZ() == blockLocation.getBlockZ()) {
                    // Same X-Z coordinates, likely finishing a 2x1 hole
                    addedToExistingTunnel = true;
                    break;
                } else if (Utils.manhattanDistance2D(unitLocation, blockLocation) == 1) {
                    // Adjacent block - extend the tunnel
                    BlockFace direction = getDirectionBetween(unitLocation, blockLocation);
                    Unit newUnit = new Unit(direction, blockLocation, Utils.isOre(block), System.currentTimeMillis());
                    tunnel.getStructure().getPath().getUnits().add(newUnit);
                    addedToExistingTunnel = true;
                    break;
                }
            }
            if (addedToExistingTunnel) break;
        }

        if (!addedToExistingTunnel) {
            // Start a new tunnel
            minerTunnels.add(new Tunnel(new Structure(new Path(block)), miner));
        }
    }

    private void handleOreDiscovery(Miner miner, Block oreBlock, Location oreLocation) {
        List<OreVein> discoveredVeins = miner.getDiscoveredOreVeins();

        if (discoveredVeins.isEmpty()) {
            // First ore discovery
            OreVein firstVein = new OreVein(oreLocation, oreBlock.getType(), System.currentTimeMillis());
            discoveredVeins.add(firstVein);
            return;
        }

        // Find if this ore belongs to an existing vein (within 3 blocks)
        OreVein currentVein = null;
        for (OreVein vein : discoveredVeins) {
            if (Utils.manhattanDistance2D(vein.getCenterLocation(), oreLocation) <= 3) {
                currentVein = vein;
                break;
            }
        }

        if (currentVein == null) {
            // New vein discovered
            currentVein = new OreVein(oreLocation, oreBlock.getType(), System.currentTimeMillis());
            discoveredVeins.add(currentVein);

            // Analyze path efficiency if this is not the first vein
            if (discoveredVeins.size() > 1) {
                analyzePathEfficiency(miner, currentVein);
            }
        }

        currentVein.addOreBlock(oreLocation);
    }

    private void analyzePathEfficiency(Miner miner, OreVein newVein) {
        List<OreVein> veins = miner.getDiscoveredOreVeins();
        if (veins.size() < 2) return;

        // Get the previous vein
        OreVein previousVein = veins.get(veins.size() - 2);

        // Calculate Manhattan distance between vein centers
        double optimalDistance = Utils.manhattanDistance2D(
                previousVein.getCenterLocation(),
                newVein.getCenterLocation()
        );

        // Calculate actual path length taken by player
        double actualPathLength = calculateActualPathLength(miner, previousVein, newVein);

        // Calculate efficiency ratio
        double efficiencyRatio = optimalDistance / actualPathLength;

        // Update suspicion score based on efficiency
        updateSuspicionScore(miner, efficiencyRatio, optimalDistance, actualPathLength);

        // Log analysis for debugging
        //System.out.println("Player " + miner.getUuid() +
        //        " - Optimal: " + optimalDistance +
        //        ", Actual: " + actualPathLength +
        //        ", Efficiency: " + String.format("%.2f", efficiencyRatio) +
        //        ", Suspicion: " + miner.getSuspicionScore());
    }

    private double calculateActualPathLength(Miner miner, OreVein fromVein, OreVein toVein) {
        List<Block> minedBlocks = miner.getMinedBlocks();

        // Find the indices of blocks near the vein locations
        int startIndex = findBlockIndexNearLocation(minedBlocks, fromVein.getCenterLocation());
        int endIndex = findBlockIndexNearLocation(minedBlocks, toVein.getCenterLocation());

        if (startIndex == -1 || endIndex == -1 || startIndex >= endIndex) {
            return Double.MAX_VALUE; // Invalid path
        }

        // Calculate path length by summing movements between consecutive blocks
        double pathLength = 0.0;
        Location previousLocation = minedBlocks.get(startIndex).getLocation();

        for (int i = startIndex + 1; i <= endIndex; i++) {
            Location currentLocation = minedBlocks.get(i).getLocation();
            pathLength += Utils.manhattanDistance2D(previousLocation, currentLocation);
            previousLocation = currentLocation;
        }

        return pathLength;
    }

    private int findBlockIndexNearLocation(List<Block> blocks, Location targetLocation) {
        for (int i = 0; i < blocks.size(); i++) {
            Location blockLoc = blocks.get(i).getLocation();
            if (Utils.manhattanDistance2D(blockLoc, targetLocation) <= 2) {
                return i;
            }
        }
        return -1;
    }

    private void updateSuspicionScore(Miner miner, double efficiencyRatio, double optimalDistance, double actualPathLength) {
        double currentScore = miner.getSuspicionScore();
        double scoreIncrease = 0.0;

        // High efficiency (close to optimal path) increases suspicion
        if (efficiencyRatio > 0.85) {
            scoreIncrease = 15.0 * efficiencyRatio; // Up to 15 points for perfect efficiency
        } else if (efficiencyRatio > 0.70) {
            scoreIncrease = 8.0 * efficiencyRatio; // Moderate suspicion
        } else if (efficiencyRatio > 0.50) {
            scoreIncrease = 3.0 * efficiencyRatio; // Low suspicion
        }

        // Bonus for longer optimal distances (harder to achieve by chance)
        if (optimalDistance > 10 && optimalDistance < 20) {
            scoreIncrease *= 1.3;
        } else if (optimalDistance >= 20) {
            scoreIncrease *= 1.5;
        }

        // Additional penalty for extremely direct paths
        if (actualPathLength - optimalDistance < 2.0 && optimalDistance > 5) {
            scoreIncrease += 10.0; // Very suspicious
        }

        miner.setSuspicionScore(Math.min(100.0, currentScore + scoreIncrease));
    }

    private BlockFace getDirectionBetween(Location from, Location to) {
        int dx = to.getBlockX() - from.getBlockX();
        int dz = to.getBlockZ() - from.getBlockZ();

        if (dx == 1) return BlockFace.EAST;
        if (dx == -1) return BlockFace.WEST;
        if (dz == 1) return BlockFace.SOUTH;
        if (dz == -1) return BlockFace.NORTH;

        return BlockFace.SELF;
    }
}
