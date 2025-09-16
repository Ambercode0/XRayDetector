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
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public record TunnelTrackingListener(@NotNull FileLogger fileLogger, @NotNull PlayerDataManager playerDataManager,
                                     @NotNull XRayDetector xRayDetector) implements Listener {

    /**
     * Handles the BlockBreakEvent when a player breaks a block. This method performs
     * operations related to mining, such as verifying the mining location, creating
     * or retrieving a miner instance, and managing tunnel structures tied to the
     * block break.
     *
     * @param event the BlockBreakEvent instance triggered when a block is broken
     */
    @EventHandler
    public void onBlockBreak(@NotNull BlockBreakEvent event) {
        final Block block = event.getBlock();
        final Player player = event.getPlayer();
        final Location blockLocation = block.getLocation();

        if (!isValidMiningLocation(blockLocation)) return;

        final Miner miner = getOrCreateMiner(player);
        final TunnelUnit tunnelUnit = createTunnelUnit(block);

        handleTunnelStructures(player, miner, tunnelUnit, block);
    }

    /**
     * Checks if the given location is a valid mining location based on specific criteria.
     *
     * @param location the location to be evaluated must not be null
     * @return {@code true} if the location is valid for mining, {@code false} otherwise
     */
    private boolean isValidMiningLocation(@NotNull Location location) {
        World.Environment worldEnv = location.getWorld().getEnvironment();
        if (worldEnv != World.Environment.NORMAL && worldEnv != World.Environment.NETHER) {
            return false;
        }
        return !isInvalidDiamondLocation(location) && !isInvalidNetheriteLocation(location);
    }

    /**
     * Retrieves an existing Miner associated with the given player or creates a new one if none exists.
     *
     * @param player the player whose associated Miner is to be retrieved or created
     * @return the existing or newly created Miner associated with the given player
     */
    @NotNull
    private Miner getOrCreateMiner(@NotNull Player player) {
        UUID uuid = player.getUniqueId();
        Optional<Miner> optionalMiner = playerDataManager.getMiners().stream()
                .filter(m -> m.getUuid().equals(uuid))
                .findAny();

        if (optionalMiner.isPresent()) {
            return optionalMiner.get();
        }

        Miner newMiner = new Miner(uuid);
        playerDataManager.getMiners().add(newMiner);
        xRayDetector().getPluginDatabase().insertMiner(newMiner);
        return newMiner;
    }

    /**
     * Creates a new TunnelUnit instance based on the provided block information.
     *
     * @param block the block from which the tunnel unit is created; must not be null
     * @return a TunnelUnit object containing details derived from the block
     */
    @NotNull
    private TunnelUnit createTunnelUnit(@NotNull Block block) {
        Location location = block.getLocation();
        return new TunnelUnit(
                location.getBlockX(),
                location.getBlockZ(),
                block.getType(),
                System.currentTimeMillis(),
                location.getWorld().getName()
        );
    }

    /**
     * Handles the creation and management of tunnel structures based on the miner's actions and the game context.
     *
     * @param player the player interacting with the miner and tunnel unit
     * @param miner the miner associated with the tunnel operation
     * @param tunnelUnit the tunnel unit being processed
     * @param block the block involved in the tunnel operation
     */
    private void handleTunnelStructures(@NotNull Player player, @NotNull Miner miner, @NotNull TunnelUnit tunnelUnit, @NotNull Block block) {
        List<TunnelStructure> minerTunnels = miner.getCreatedTunnels();

        if (minerTunnels.isEmpty()) {
            handleFirstTunnel(player, miner, tunnelUnit, block);
            return;
        }

        if (handleExistingUnit(player, tunnelUnit, minerTunnels, block)) {
            return;
        }

        final TunnelStructure[] adjacentStructures = findAdjacentStructures(minerTunnels, tunnelUnit);
        int adjacentCount = countNonNullStructures(adjacentStructures);

        if (adjacentCount == 0) {
            handleNewStructure(player, miner, tunnelUnit, block);
        } else if (adjacentCount == 1) {
            handleSingleAdjacent(player, tunnelUnit, block, adjacentStructures[0]);
        } else {
            handleMultipleAdjacent(player, miner, tunnelUnit, block, adjacentStructures, adjacentCount);
        }
    }

    /**
     * Handles the creation and processing of the first tunnel structure when a Miner starts digging.
     * This includes marking the associated TunnelUnit as exposed to air if applicable,
     * logging the event, adding the tunnel structure to the miner's list of created tunnels,
     * and saving the data to the database.
     *
     * @param player the Player who initiated the mining action; must not be null.
     * @param miner the Miner associated with the tunnel creation; must not be null.
     * @param tunnelUnit the TunnelUnit that represents the specific part of the tunnel being processed; must not be null.
     * @param block the Block at the location where the tunnel is being created; must not be null.
     */
    private void handleFirstTunnel(@NotNull Player player, @NotNull Miner miner, @NotNull TunnelUnit tunnelUnit, @NotNull Block block) {
        TunnelStructure firstTunnel = new TunnelStructure(tunnelUnit);
        boolean exposedToAir = Utils.isExposedToAir(tunnelUnit, null, block);
        tunnelUnit.setExposedToAir(exposedToAir);
        miner.getCreatedTunnels().add(firstTunnel);

        logFirstTunnel(player.getName(), firstTunnel, tunnelUnit, exposedToAir);
        saveTunnelToDatabase(firstTunnel, miner, tunnelUnit);

        if (tunnelUnit.isOre()) {
            TunnelPath path = firstTunnel.getMainTunnelPath();
            OreVein vein = path.getOrCreateVein(tunnelUnit, block);
            newTunnelStartsWithVein(vein, firstTunnel, tunnelUnit.isExposedToAir());
        } else {
            xRayDetector().getPluginDatabase().insertTunnelUnit(tunnelUnit, firstTunnel.getMainTunnelPath());
        }
    }

    /**
     * Initializes a new tunnel starting with a given vein, updates the tunnel structure,
     * and stores the relevant data in the plugin database.
     *
     * @param vein the ore vein associated with the new tunnel; must not be null
     * @param structure the tunnel structure used to manage the new tunnel path; must not be null
     */
    private void newTunnelStartsWithVein(@NotNull OreVein vein, @NotNull TunnelStructure structure, boolean setExposed) {
        PluginDatabase database = xRayDetector.getPluginDatabase();
        TunnelPath path = structure.getMainTunnelPath();
        database.insertOreVein(vein, path);
        for (TunnelUnit tunnelUnit : vein.units()) {
            tunnelUnit.setExposedToAir(true);
            path.add(tunnelUnit);
            database.insertTunnelUnit(tunnelUnit, path);
            database.updateTunnelUnitOreVein(tunnelUnit, vein);
        }
    }

    /**
     * Handles an existing tunnel unit by checking if it belongs to any of the provided tunnel structures.
     * If the unit is contained in a structure, it logs the relevant details and updates the unit within the structure.
     *
     * @param player the player associated with the operation; must not be null
     * @param tunnelUnit the tunnel unit to check against the structures; must not be null
     * @param minerTunnels a list of tunnel structures to evaluate; must not be null
     * @return true if the tunnel unit is found and handled within one of the structures, false otherwise
     */
    private boolean handleExistingUnit(@NotNull Player player, @NotNull TunnelUnit tunnelUnit, @NotNull List<TunnelStructure> minerTunnels, @NotNull Block block) {
        for (final TunnelStructure structure : minerTunnels) {
            if (structure.isContained(tunnelUnit)) {
                logExistingUnit(player.getName(), tunnelUnit, structure);
                updateExistingUnit(tunnelUnit, structure, block);
                return true;
            }
        }
        return false;
    }

    /**
     * Handles the creation of a new tunnel structure for a given player and miner,
     * associates it with the specified tunnel unit and block, and updates its state
     * based on environmental conditions.
     *
     * @param player the player initiating the creation of the new tunnel structure; must not be null
     * @param miner the miner associated with the tunnel creation activity; must not be null
     * @param tunnelUnit the tunnel unit representing the segment of the created tunnel; must not be null
     * @param block the block that forms the base reference for the tunnel unit creation; must not be null
     */
    private void handleNewStructure(@NotNull Player player, @NotNull Miner miner, @NotNull TunnelUnit tunnelUnit, @NotNull Block block) {
        TunnelStructure newStructure = new TunnelStructure(tunnelUnit);
        miner.getCreatedTunnels().add(newStructure);
        boolean exposedToAir = Utils.isExposedToAir(tunnelUnit, null, block);
        tunnelUnit.setExposedToAir(exposedToAir);

        logNewStructure(player.getName(), newStructure, tunnelUnit, exposedToAir);
        saveTunnelToDatabase(newStructure, miner, tunnelUnit);

        if (tunnelUnit.isOre()) {
            TunnelPath path = newStructure.getMainTunnelPath();
            OreVein vein = path.getOrCreateVein(tunnelUnit, block);
            newTunnelStartsWithVein(vein, newStructure, tunnelUnit.isExposedToAir());
        } else {
            xRayDetector().getPluginDatabase().insertTunnelUnit(tunnelUnit, newStructure.getMainTunnelPath());
        }
    }

    /**
     * Handles the addition of a single adjacent TunnelUnit to an existing TunnelStructure.
     * This method validates whether the newly added TunnelUnit is exposed to air,
     * logs the operation, and updates the database with the new tunnel information.
     *
     * @param player the Player performing the action; must not be null
     * @param tunnelUnit the TunnelUnit being added to the TunnelStructure; must not be null
     * @param block the Block associated with the TunnelUnit; must not be null
     * @param structure the TunnelStructure being extended with the TunnelUnit; must not be null
     */
    private void handleSingleAdjacent(@NotNull Player player, @NotNull TunnelUnit tunnelUnit, @NotNull Block block, @NotNull TunnelStructure structure) {
        structure.getMainTunnelPath().add(tunnelUnit);
        boolean exposedToAir = Utils.isExposedToAir(tunnelUnit, structure, block);
        tunnelUnit.setExposedToAir(exposedToAir);
        PluginDatabase db = xRayDetector().getPluginDatabase();
        logExtendedStructure(player.getName(), structure, tunnelUnit, exposedToAir);

        if (!tunnelUnit.isOre()) {
            db.insertTunnelUnit(tunnelUnit, structure.getMainTunnelPath());
            return;
        }

        // unit adjacent and ore

        TunnelPath path = structure.getMainTunnelPath();
        // OreVein oreVein = path.getOrCreateVein(tunnelUnit, block);
        Optional<OreVein> oreVeinOpt = path.veinOf(tunnelUnit);
        OreVein oreVein;
        if (oreVeinOpt.isEmpty()) { // unit adjacent, ore, and new vein
            oreVein = path.getOrCreateVein(tunnelUnit, block); // creating 100%
            // we should add all blocks since this is a new vein;
            List<TunnelUnit> veinUnits = oreVein.units();
            db.insertOreVein(oreVein, path);
            veinUnits.forEach(vu -> {
                db.insertTunnelUnit(vu, structure.getMainTunnelPath());
                db.updateTunnelUnitOreVein(vu, oreVein);
            });
        } else { // unit adjacent, ore, and existing vein (ignore?)
            oreVein = oreVeinOpt.get();
            assert oreVein.units().contains(tunnelUnit);
        }
    }

    /**
     * Handles the merging of multiple adjacent tunnel structures into a single new TunnelStructure, updates
     * relevant associations, and logs the details of the operation. This method is used when multiple adjacent
     * tunnel structures need to be unified due to a player's mining activity.
     *
     * @param player the Player initiating the operation; must not be null
     * @param miner the Miner associated with the Player who owns the created or updated tunnel structures; must not be null
     * @param tunnelUnit the TunnelUnit representing the new or modified part of the tunnel; must not be null
     * @param block the Block associated with the mining operation; must not be null
     * @param adjacentStructures an array of TunnelStructure instances that are adjacent to the given TunnelUnit; must not be null
     * @param count the number of non-null TunnelStructure instances to process from the adjacentStructures array
     */
    private void handleMultipleAdjacent(@NotNull Player player, @NotNull Miner miner, @NotNull TunnelUnit tunnelUnit,
                                        @NotNull Block block, @NotNull TunnelStructure[] adjacentStructures, int count) {
        TunnelStructure[] structuresToMerge = new TunnelStructure[count];
        StringBuilder uuidsList = new StringBuilder();

        for (int i = 0; i < count; i++) {
            TunnelStructure structure = adjacentStructures[i];
            uuidsList.append(structure.getUuid()).append(',');
            miner.getCreatedTunnels().remove(structure);
            structuresToMerge[i] = structure;
        }

        TunnelStructure mergedStructure = new TunnelStructure(structuresToMerge);
        mergedStructure.getMainTunnelPath().addLast(tunnelUnit);
        miner.getCreatedTunnels().add(mergedStructure);

        boolean exposedToAir = Utils.isExposedToAir(tunnelUnit, mergedStructure, block);
        tunnelUnit.setExposedToAir(exposedToAir);

        List<TunnelStructure> nonNullStructures = new ArrayList<>(Arrays.asList(adjacentStructures).subList(0, count));
        xRayDetector().getPluginDatabase().mergeStructures(miner.getUuid(), mergedStructure, tunnelUnit, nonNullStructures);

        logMergedStructures(player.getName(), count, uuidsList.toString(), tunnelUnit, mergedStructure, exposedToAir);
    }

    /**
     * Saves information about a tunnel structure, its associated miner, and a tunnel unit to the database.
     *
     * @param structure the TunnelStructure instance representing the overall structure of the tunnel; must not be null.
     * @param miner the Miner instance associated with the tunnel activity; must not be null.
     * @param unit the TunnelUnit instance representing a segment or part of the tunnel; must not be null.
     */
    private void saveTunnelToDatabase(@NotNull TunnelStructure structure, @NotNull Miner miner, @NotNull TunnelUnit unit) {
        PluginDatabase db = xRayDetector().getPluginDatabase();
        db.insertTunnelStructure(structure, miner);
        db.insertTunnelPath(structure.getMainTunnelPath(), structure);
        // db.insertTunnelUnit(unit, structure.getMainTunnelPath());
    }

    /**
     * Logs the creation of the first tunnel structure by a player, including its
     * associated properties and whether it is marked as exposed.
     *
     * @param player the name or identifier of the player who created the first
     *               tunnel structure; must not be null
     * @param tunnel the TunnelStructure object representing the created tunnel
     *               structure; must not be null
     * @param unit   the TunnelUnit object representing the location of the created
     *               tunnel; must not be null
     * @param exposed a boolean indicating whether the created tunnel is marked as
     *                exposed
     */
    private void logFirstTunnel(@NotNull String player, @NotNull TunnelStructure tunnel, @NotNull TunnelUnit unit, boolean exposed) {
        fileLogger.addLogMessage(String.format("player %s created new first structure (%s) at [%d, %d] exposed=%b",
                player, tunnel.getUuid(), unit.getX(), unit.getZ(), exposed));
    }

    /**
     * Logs an action where a player mined a block within a pre-existing tunnel unit of a tunnel structure.
     *
     * @param player   the name or identifier of the player performing the action; must not be null
     * @param unit     the TunnelUnit where the action occurred; must not be null
     * @param structure the TunnelStructure containing the affected TunnelUnit; must not be null
     */
    private void logExistingUnit(@NotNull String player, @NotNull TunnelUnit unit, @NotNull TunnelStructure structure) {
        fileLogger.addLogMessage(String.format("player %s mined block in pre-existing unit [%d, %d] of structure (%s)",
                player, unit.getX(), unit.getZ(), structure.getUuid()));
    }

    /**
     * Logs the creation of a new tunnel structure by a player along with its properties.
     *
     * @param player   the name or identifier of the player who created the structure; must not be null
     * @param structure the new TunnelStructure that was created; must not be null
     * @param unit      the TunnelUnit representing the location where the structure was created; must not be null
     * @param exposed   whether the new structure is marked as exposed or not
     */
    private void logNewStructure(@NotNull String player, @NotNull TunnelStructure structure, @NotNull TunnelUnit unit, boolean exposed) {
        fileLogger.addLogMessage(String.format("player %s created new structure (%s) at [%d, %d] exposed=%b",
                player, structure.getUuid(), unit.getX(), unit.getZ(), exposed));
    }

    /**
     * Logs when a player extends an existing tunnel structure with a new tunnel unit.
     *
     * @param player the name of the player performing the action; must not be null.
     * @param structure the TunnelStructure being extended; must not be null.
     * @param unit the TunnelUnit being added to the structure; must not be null.
     * @param exposed a boolean indicating whether the extension is exposed to the surface.
     */
    private void logExtendedStructure(@NotNull String player, @NotNull TunnelStructure structure, @NotNull TunnelUnit unit, boolean exposed) {
        fileLogger.addLogMessage(String.format("player %s extended existing structure (%s) at [%d, %d] exposed=%b",
                player, structure.getUuid(), unit.getX(), unit.getZ(), exposed));
    }

    /**
     * Logs details about merged tunnel structures, including the player involved,
     * the number of structures merged, their UUIDs, and the new merged structure.
     *
     * @param player the name of the player who performed the merge; must not be null
     * @param count the number of structures merged
     * @param uuids a string representation of the UUIDs of the merged structures; must not be null
     * @param unit the tunnel unit where the merge occurred; must not be null
     * @param merged the resulting merged tunnel structure; must not be null
     * @param exposed whether the merged structure is exposed
     */
    private void logMergedStructures(@NotNull String player, int count, @NotNull String uuids, @NotNull TunnelUnit unit,
                                     @NotNull TunnelStructure merged, boolean exposed) {
        fileLogger.addLogMessage(String.format("player %s merged %d structures (%s) at [%d, %d] into new structure %s exposed=%b",
                player, count, uuids, unit.getX(), unit.getZ(), merged.getUuid(), exposed));
    }

    /**
     * Finds and returns all TunnelStructures from the provided list that are adjacent to the given TunnelUnit.
     *
     * @param structures a list of TunnelStructure instances to search through; must not be null.
     * @param unit the TunnelUnit to check adjacency against; must not be null.
     * @return an array of TunnelStructure instances that are adjacent to the specified TunnelUnit; never null.
     */
    @NotNull
    private TunnelStructure[] findAdjacentStructures(@NotNull List<TunnelStructure> structures, @NotNull TunnelUnit unit) {
        final TunnelStructure[] adjacent = new TunnelStructure[0x04];
        int count = 0;
        for (TunnelStructure structure : structures) {
            if (structure.isAdjacent(unit)) {
                adjacent[count++] = structure;
            }
        }
        return adjacent;
    }
    
    /**
     * Counts the number of non-null TunnelStructure objects in the given array.
     *
     * @param structures an array of TunnelStructure objects, which can be null or contain null elements
     * @return the count of non-null TunnelStructure objects in the array
     */
    private int countNonNullStructures(@Nullable TunnelStructure[] structures) {
        int count = 0;
        for (TunnelStructure structure : structures) {
            if (structure != null) count++;
        }
        return count;
    }
    
    /**
     * Updates an existing TunnelUnit's material within the specified TunnelStructure if the material
     * of the provided TunnelUnit is classified as an ore. The update is only performed if the
     * specified TunnelStructure contains the provided TunnelUnit.
     *
     * @param tunnelUnit the TunnelUnit containing the material to update; must not be null.
     * @param structure the TunnelStructure that may contain the TunnelUnit to be updated; can be null.
     */
    private void updateExistingUnit(@NotNull TunnelUnit tunnelUnit, @Nullable TunnelStructure structure, @NotNull Block block) {
        if (Utils.isOre(tunnelUnit.getMaterial())) {
            TunnelUnit existingUnit = structure.getContained(tunnelUnit);
            if (existingUnit != null && !existingUnit.isOre()) {
                existingUnit.setMaterial(tunnelUnit.getMaterial());
                xRayDetector.getPluginDatabase().updateTunnelUnitMaterial(existingUnit, tunnelUnit.getMaterial().name());
            }

            TunnelPath path = structure.getMainTunnelPath();
            List<OreVein> oreVeins = path.getOreVeins();

            OreVein found = null;
            outLoop:
            for (OreVein oreVein : oreVeins) {              // checking if any ore veins in this path contains
                for (TunnelUnit unit : oreVein.units()) {   // the unit that we are trying to update
                    if (unit.equals(tunnelUnit)) {
                        found = oreVein;
                        break outLoop;
                    }
                }
            }

            if (found == null) {    // no vein was found, it means we must create a new vein and add all blocks.
                found = path.getOrCreateVein(tunnelUnit, block);
                xRayDetector.getPluginDatabase().insertOreVein(found, path);
                xRayDetector.getPluginDatabase().updateTunnelUnitOreVein(tunnelUnit, found);
                for (final TunnelUnit unit : found.units()) {
                    if (unit.equals(tunnelUnit)) continue;
                    xRayDetector.getPluginDatabase().insertTunnelUnit(unit, path);
                    xRayDetector.getPluginDatabase().updateTunnelUnitOreVein(unit, found);
                }
            } else {
                // it's already there?
            }

        }
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