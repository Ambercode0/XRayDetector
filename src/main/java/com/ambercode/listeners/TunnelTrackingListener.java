package com.ambercode.listeners;

import com.ambercode.XRayDetector;
import com.ambercode.data.*;
import com.ambercode.database.PluginDatabase;
import com.ambercode.logging.FileLogger;
import com.ambercode.manager.PlayerDataManager;

import com.ambercode.utils.Utils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public record TunnelTrackingListener(@NotNull FileLogger fileLogger, @NotNull PlayerDataManager playerDataManager,
                                     @NotNull XRayDetector xRayDetector) implements Listener {

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        PluginDatabase db = xRayDetector().getPluginDatabase();
        Block block = event.getBlock();
        Player player = event.getPlayer();
        String playerName = player.getName();
        UUID uuid = player.getUniqueId();
        Location blockLocation = block.getLocation();
        Material blockMaterial = block.getType();

        Miner miner = null;
        Optional<Miner> optionalMiner = playerDataManager.getMiners().stream().filter(m -> m.getUuid().equals(uuid)).findAny();
        if (optionalMiner.isPresent()) {
            miner = optionalMiner.get();
        } else {
            playerDataManager.getMiners().add(miner = new Miner(uuid));
            db.insertMiner(miner);
        }

        if (isValidDiamondLocation(blockLocation) || isValidNetheriteLocation(blockLocation)) // ignore impossible to find ores in this range
            return;

        List<TunnelStructure> minerTunnelStructures = miner.getCreatedTunnels();

        TunnelUnit tunnelUnit = new TunnelUnit(blockLocation.getBlockX(), blockLocation.getBlockZ(), blockMaterial, System.currentTimeMillis());

        if (minerTunnelStructures.isEmpty()) { // logic: no structures present, creating FIRST new one, setting unit as path origin and main path
            TunnelStructure firstTunnel = new TunnelStructure(tunnelUnit);
            boolean exposedToAir = Utils.isExposedToAir(tunnelUnit, null, block);
            tunnelUnit.setExposedToAir(exposedToAir);
            minerTunnelStructures.add(firstTunnel);
            fileLogger.addLogMessage(String.format("player %s created new first structure (%s) at [%d, %d] exposed=%b", playerName, firstTunnel.getUuid(), blockLocation.getBlockX(), blockLocation.getBlockZ(), exposedToAir));

            db.insertTunnelStructure(firstTunnel, miner);
            db.insertTunnelPath(firstTunnel.getMainTunnelPath(), firstTunnel);
            db.insertTunnelUnit(tunnelUnit, firstTunnel.getMainTunnelPath());

            return;
        }

        // checking if it is already contained in the structure
        for (TunnelStructure tunnelStructure : minerTunnelStructures) {
            // found a structure with the unit in its path; ignore.
            if (tunnelStructure.isContained(tunnelUnit)) {
                fileLogger.addLogMessage(String.format("player %s mined block in pre-existing unit [%d, %d] of structure (%s)", playerName, blockLocation.getBlockX(), blockLocation.getBlockZ(), tunnelStructure.getUuid()));
                return;
            }
        }

        // checking if we can find any structure for which this unit is adjacent to
        // trying to implement tunnels merging (if adjacent to at least 2 block of different structures)
        int adjacentToUniqueStructuresCount = 0;
        final TunnelStructure[] adjacentStructures = new TunnelStructure[0x04];
        for (TunnelStructure tunnelStructure : minerTunnelStructures) {
            if (tunnelStructure.isAdjacent(tunnelUnit)) {
                adjacentStructures[adjacentToUniqueStructuresCount++] = tunnelStructure;
            }
        }

        if (adjacentToUniqueStructuresCount == 0) { // is not adjacent to anything; is new structure!
            TunnelStructure newStructure = new TunnelStructure(tunnelUnit);
            minerTunnelStructures.add(newStructure);
            boolean exposedToAir = Utils.isExposedToAir(tunnelUnit, null, block);
            tunnelUnit.setExposedToAir(exposedToAir);
            fileLogger.addLogMessage(String.format("player %s created new structure (%s) at [%d, %d] exposed=%b", playerName, newStructure.getUuid(), blockLocation.getBlockX(), blockLocation.getBlockZ(), exposedToAir));

            db.insertTunnelStructure(newStructure, miner);
            db.insertTunnelPath(newStructure.getMainTunnelPath(), newStructure);
            db.insertTunnelUnit(tunnelUnit, newStructure.getMainTunnelPath());

            return;
        }

        if (adjacentToUniqueStructuresCount == 1) { // is adjacent to only ONE structure, we just extend existing.
            adjacentStructures[0].getMainTunnelPath().getUnits().add(tunnelUnit);
            boolean exposedToAir = Utils.isExposedToAir(tunnelUnit, adjacentStructures[0], block);
            tunnelUnit.setExposedToAir(exposedToAir);
            fileLogger.addLogMessage(String.format("player %s extended existing structure (%s) at [%d, %d] exposed=%b", playerName, adjacentStructures[0].getUuid(), blockLocation.getBlockX(), blockLocation.getBlockZ(), exposedToAir));

            db.insertTunnelUnit(tunnelUnit, adjacentStructures[0].getMainTunnelPath()); // TODO: fix FOREIGNKEY crash

            return;
        }

        // here it must be by exclusion that there are two to four total adjacent structures, we must merge all to one.
        List<TunnelUnit> mergedTunnelUnits = new ArrayList<>();
        StringBuilder uuidsList = new StringBuilder();
        for (int i = 0; i < adjacentToUniqueStructuresCount; i++) {
            TunnelStructure tunnelStructure = adjacentStructures[i];
            uuidsList.append(tunnelStructure.getUuid()).append(',');
            minerTunnelStructures.remove(tunnelStructure);
            mergedTunnelUnits.addAll(tunnelStructure.getMainTunnelPath().getUnits());
        }

        TunnelStructure mergedStructure = new TunnelStructure(mergedTunnelUnits);
        mergedStructure.getMainTunnelPath().getUnits().addLast(tunnelUnit);
        minerTunnelStructures.add(mergedStructure);
        boolean exposedToAir = Utils.isExposedToAir(tunnelUnit, mergedStructure, block);
        tunnelUnit.setExposedToAir(exposedToAir);

        List<TunnelStructure> nonNullStructures = new ArrayList<>(Arrays.asList(adjacentStructures).subList(0, adjacentToUniqueStructuresCount));
        db.mergeStructures(miner.getUuid(), mergedStructure, tunnelUnit, nonNullStructures);

        fileLogger.addLogMessage(String.format("player %s merged %d structures (%s) at [%d, %d] into new structure %s exposed=%b", playerName,adjacentToUniqueStructuresCount , uuidsList, blockLocation.getBlockX(), blockLocation.getBlockZ(), mergedStructure.getUuid(), exposedToAir));
    }

    private boolean isValidDiamondLocation(@NotNull Location blockLocation) {
        return Objects.requireNonNull(blockLocation.getWorld()).getEnvironment() == World.Environment.NORMAL && (blockLocation.getBlockY() < -64 || blockLocation.getBlockY() > 16);
    }

    private boolean isValidNetheriteLocation(@NotNull Location blockLocation) {
        return Objects.requireNonNull(blockLocation.getWorld()).getEnvironment() == World.Environment.NETHER && (blockLocation.getBlockY() < 13 || blockLocation.getBlockY() > 119);
    }
}