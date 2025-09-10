/*
 *     XRayDetector - An advanced automatic detector to prevent X-Ray in your server
 *     Copyright (C) 2025 'AmberCode'
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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

    /**
     * Handles the BlockBreakEvent triggered when a player breaks a block.
     * This method tracks and manages tunnel structures created by players breaking blocks.
     * It updates the player's mining activity, checks for existing tunnel structures,
     * creates new structures if necessary, merges tunnel structures if applicable,
     * and records relevant data into the database and logs.
     *
     * @param event the BlockBreakEvent instance, which holds details about the block
     *              being broken, the player involved, and the event location. Must not be null.
     */
    @EventHandler
    public void onBlockBreak(@NotNull BlockBreakEvent event) {
        PluginDatabase db = xRayDetector().getPluginDatabase();
        Block block = event.getBlock();
        Player player = event.getPlayer();
        String playerName = player.getName();
        UUID uuid = player.getUniqueId();
        Location blockLocation = block.getLocation();
        Material blockMaterial = block.getType();

        World.Environment worldEnvironment = blockLocation.getWorld().getEnvironment();
        if (worldEnvironment != World.Environment.NORMAL && worldEnvironment != World.Environment.NETHER)
            return; // ignore non-normal worlds and end dimensions.

        if (isInvalidDiamondLocation(blockLocation) || isInvalidNetheriteLocation(blockLocation))
            return; // ignore impossible to find ores in this range.

        Miner miner = null;
        Optional<Miner> optionalMiner = playerDataManager.getMiners().stream().filter(m -> m.getUuid().equals(uuid)).findAny();
        if (optionalMiner.isPresent()) {
            miner = optionalMiner.get();
        } else {
            playerDataManager.getMiners().add(miner = new Miner(uuid));
            db.insertMiner(miner);
        }

        List<TunnelStructure> minerTunnelStructures = miner.getCreatedTunnels();

        TunnelUnit tunnelUnit = new TunnelUnit(blockLocation.getBlockX(), blockLocation.getBlockZ(), blockMaterial,
                System.currentTimeMillis(), blockLocation.getWorld().getName());

        if (minerTunnelStructures.isEmpty()) { // logic: no structures present, creating FIRST new one, setting unit as path origin and main path
            TunnelStructure firstTunnel = new TunnelStructure(tunnelUnit);
            boolean exposedToAir = Utils.isExposedToAir(tunnelUnit, null, block);
            tunnelUnit.setExposedToAir(exposedToAir);
            minerTunnelStructures.add(firstTunnel);
            fileLogger.addLogMessage(String.format("player %s created new first structure (%s) at [%d, %d] exposed=%b",
                    playerName, firstTunnel.getUuid(), blockLocation.getBlockX(), blockLocation.getBlockZ(), exposedToAir));

            db.insertTunnelStructure(firstTunnel, miner);
            db.insertTunnelPath(firstTunnel.getMainTunnelPath(), firstTunnel);
            db.insertTunnelUnit(tunnelUnit, firstTunnel.getMainTunnelPath());

            return;
        }

        // checking if it is already contained in the structure
        for (TunnelStructure tunnelStructure : minerTunnelStructures) {
            // found a structure with the unit in its path; ignore.
            if (tunnelStructure.isContained(tunnelUnit)) {
                fileLogger.addLogMessage(String.format("player %s mined block in pre-existing unit [%d, %d] of structure (%s)",
                        playerName, blockLocation.getBlockX(), blockLocation.getBlockZ(), tunnelStructure.getUuid()));
                // checking if there's ore above/below, if positive updating tunnelUnit material to that ore.
                // this avoids bypassing diamond detection by first mining a block above/below to it.

                if (Utils.isOre(blockMaterial)) {
                    TunnelUnit toAlter = tunnelStructure.getContained(tunnelUnit);
                    assert toAlter != null;
                    toAlter.setMaterial(blockMaterial); // updating the unit to new ore material. This won't be changed again.
                                                        // meaning once a unit is ore, it cannot change to non-ore.
                }

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

            db.insertTunnelUnit(tunnelUnit, adjacentStructures[0].getMainTunnelPath());
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

        fileLogger.addLogMessage(String.format("player %s merged %d structures (%s) at [%d, %d] into new structure %s exposed=%b",
                playerName,adjacentToUniqueStructuresCount , uuidsList, blockLocation.getBlockX(),
                blockLocation.getBlockZ(), mergedStructure.getUuid(), exposedToAir));
    }

    /**
     * Determines whether the specified block location is an invalid diamond location.
     * A valid diamond location is within the NORMAL world environment and exists
     * either below Y-level -64 or above Y-level 16.
     *
     * @param blockLocation the location of the block to be validated, must not be null
     * @return true if the block location is valid for diamond spawning, false otherwise
     */
    private boolean isInvalidDiamondLocation(@NotNull Location blockLocation) {
        return Objects.requireNonNull(blockLocation.getWorld()).getEnvironment() == World.Environment.NORMAL && (blockLocation.getBlockY() < -64 || blockLocation.getBlockY() > 16);
    }

    /**
     * Checks if the given location is an invalid location for finding netherite in the Nether dimension.
     *
     * @param blockLocation the location to evaluate; must not be null.
     * @return true if the location is in the Nether environment and the Y-coordinate
     *         is less than 13 or greater than 119; false otherwise.
     */
    private boolean isInvalidNetheriteLocation(@NotNull Location blockLocation) {
        return Objects.requireNonNull(blockLocation.getWorld()).getEnvironment() == World.Environment.NETHER && (blockLocation.getBlockY() < 13 || blockLocation.getBlockY() > 119);
    }
}