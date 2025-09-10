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

package com.ambercode.manager;

import com.ambercode.XRayDetector;
import com.ambercode.data.Miner;
import com.ambercode.data.TunnelStructure;
import com.ambercode.database.PluginDatabase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * The PlayerDataManager class is responsible for managing player data, specifically
 * `Miner` objects, for the XRayDetector plugin. It provides methods to load player
 * data from the database, retrieve specific miners by their UUID, and access tunnel
 * structures associated with miners.
 */
public class PlayerDataManager {

    private final XRayDetector plugin;
    private final Set<Miner> miners = new HashSet<>();

    /**
     * Constructs a new PlayerDataManager instance.
     *
     * @param plugin the plugin instance of XRayDetector must not be null
     */
    public PlayerDataManager(@NotNull XRayDetector plugin) {
        this.plugin = plugin;
    }

    /**
     * Loads miners data from the database into the in-memory cache of the PlayerDataManager instance.
     * <p>
     * This method fetches all miner records from the database using the {@link PluginDatabase#getAllData()}
     * method and adds them to the internal cache managed by the PlayerDataManager. After loading the data,
     * it logs the total number of loaded miners into the plugin's logger.
     * <p>
     * Responsibilities:
     * - Retrieves miner data from the database via {@link PluginDatabase}.
     * - Updates the internal cache of miners in the PlayerDataManager.
     * - Logs the number of miners successfully loaded.
     * <p>
     * Dependencies:
     * - Uses the {@link PluginDatabase} provided by the XRayDetector plugin for data retrieval.
     * - Relies on the plugin's logging system to provide feedback.
     * <p>
     * Pre-conditions:
     * - The database connection must be properly established before this method is called.
     * <p>
     * Side Effects:
     * - Mutates the internal cache of {@code PlayerDataManager} by adding all retrieved miners.
     * - Generates a log entry containing the number of miners loaded.
     * <p>
     * Thread Safety:
     * - This method is not thread-safe. Concurrent modifications to the internal cache or simultaneous database
     *   queries could lead to inconsistent states.
     */
    public void loadCacheFromDatabase() {
        PluginDatabase db = plugin.getPluginDatabase();
        List<Miner> miners = db.getAllData();
        plugin.getLogger().info(String.format("Loaded %d miners from the database.",  miners.size()));
        this.miners.addAll(miners);
    }

    /**
     * Retrieves a Miner object by its unique identifier (UUID) from a collection of miners.
     *
     * @param uuid the UUID of the miner to retrieve
     * @return the Miner object associated with the given UUID, or null if no such miner exists
     */
    @Nullable
    public Miner getMiner(@NotNull UUID uuid) {
        return miners.stream().filter(miner -> miner.getUuid().equals(uuid)).findFirst().orElse(null);
    }

    /**
     * Retrieves a {@link TunnelStructure} associated with the given UUID. The method searches through all miners
     * and their respective tunnel structures to find a match for the specified UUID.
     *
     * @param uuid the unique identifier of the tunnel structure to retrieve
     * @return the {@link TunnelStructure} corresponding to the specified UUID if found, otherwise {@code null}
     */
    @Nullable
    public TunnelStructure getTunnelStructure(@NotNull UUID uuid) {
        for (Miner miner : miners) {
            TunnelStructure tunnelStructure = miner.getTunnelStructure(uuid);
            if (tunnelStructure != null) {
                return tunnelStructure;
            }
        }
        return null;
    }

    /**
     * Retrieves an unmodifiable set of all miners currently managed by this instance.
     * The returned set represents the internal cache of miners, providing access to
     * their associated information and operations.
     *
     * @return a set containing all {@link Miner} instances being managed,
     *         or an empty set if no miners are present
     */
    @NotNull
    public Set<Miner> getMiners() {
        return miners;
    }
}
