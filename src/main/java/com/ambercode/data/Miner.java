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

package com.ambercode.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The Miner class represents a mining entity that creates and tracks tunnel structures.
 * Each miner is associated with a unique identifier (UUID) and maintains a record of created
 * tunnels, a suspicion score, and metrics related to mining activity.
 */
public class Miner {

    private final UUID uuid;
    private final List<TunnelStructure> createdTunnels = new ArrayList<>();
    private double suspicionScore = 0.00;

    public Miner(@NotNull UUID uuid) {
        this.uuid = uuid;
    }

    /**
     * Retrieves a {@link TunnelStructure} from the list of created tunnels that matches the specified UUID.
     *
     * @param uuid the unique identifier of the tunnel structure to be retrieved; must not be null.
     * @return the {@link TunnelStructure} with the specified UUID if found, or {@code null} if no matching structure exists.
     */
    @Nullable
    public TunnelStructure getTunnelStructure(@NotNull UUID uuid) {
      // return createdTunnels.stream().filter(t -> t.getUuid().equals(uuid)).findAny().orElse(null);
        for (final TunnelStructure structure : createdTunnels)
            if (structure.getUuid().equals(uuid))
                return structure;
        return null;
    }

    /**
     * Retrieves the universally unique identifier (UUID) associated with this instance.
     *
     * @return a non-null UUID representing the unique identity of this instance.
     */
    @NotNull
    public UUID getUuid() {
        return uuid;
    }

    /**
     * Retrieves the list of created tunnel structures associated with this instance.
     *
     * @return a non-null list of {@code TunnelStructure} objects representing the tunnels
     *         created and tracked by this instance.
     */
    @NotNull
    public List<TunnelStructure> getCreatedTunnels() {
        return createdTunnels;
    }

    /**
     * Retrieves the suspicion score associated with this entity.
     * The suspicion score is an indicator that quantifies the potential for suspicious activity.
     *
     * @return the suspicion score as a double value.
     */
    public double getSuspicionScore() {
        return suspicionScore;
    }

    /**
     * Returns the number of ore veins discovered by the miner.
     *
     * @return the number of ore veins discovered as an integer.
     */
    public int getDiscoveredOreVeins() {
        int counter = 0;
        for (final TunnelStructure structure : createdTunnels)
            counter += structure.getMainTunnelPath().veinsSize();
        return counter;
    }

    /**
     * Retrieves the total number of blocks mined by the miner.
     *
     * @return the number of blocks mined as an integer.
     */
    public int getMinedBlocks() {
        int counter = 0;
        for (final TunnelStructure createdTunnel : getCreatedTunnels())
            counter += createdTunnel.getMainTunnelPath().unitsSize();
        return counter;
    }


    /**
     * Sets the suspicion score for this miner. The suspicion score is a measure
     * of potentially suspicious activity associated with the miner.
     *
     * @param suspicionScore the new suspicion score, represented as a double value
     */
    public void setSuspicionScore(double suspicionScore) {
        this.suspicionScore = suspicionScore;
    }
}