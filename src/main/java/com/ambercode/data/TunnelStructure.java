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

import com.ambercode.utils.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a tunnel structure which contains a main tunnel path and is uniquely identified
 * by a UUID. The main tunnel path is composed of a sequence of TunnelUnit objects, representing
 * discrete components of the tunnel. The class provides functionality to interact with and
 * manipulate the main tunnel path.
 */
public class TunnelStructure {

    private TunnelPath mainTunnelPath;
    private final UUID uuid;

    /**
     * Constructs a new TunnelStructure instance with a single TunnelUnit as the origin of the main tunnel path.
     * This constructor initializes a main tunnel path containing the provided origin TunnelUnit and assigns
     * a unique identifier (UUID) to this TunnelStructure instance.
     *
     * @param origin the origin TunnelUnit used to initialize the main tunnel path; must not be null
     */
    public TunnelStructure(@NotNull TunnelUnit origin) {
        this.mainTunnelPath = new TunnelPath(origin);
        this.uuid = UUID.randomUUID();
    }

    /**
     * Constructs a new instance of the TunnelStructure class using a list of TunnelUnit objects.
     * Each TunnelUnit represents a component of the tunnel's main path.
     * This constructor creates a main tunnel path and populates it with the given units.
     *
     * @param units the list of TunnelUnit objects to be added to the main tunnel path; must not be null.
     */
    public TunnelStructure(@NotNull List<TunnelUnit> units) {
        this.mainTunnelPath = new TunnelPath();
        mainTunnelPath.getUnits().addAll(units);
        this.uuid = UUID.randomUUID();
    }

    /**
     * Constructs a new TunnelStructure with the specified list of TunnelUnits and unique identifier (UUID).
     * The provided TunnelUnits are added to the main tunnel path of this structure.
     *
     * @param units a non-null list of TunnelUnits to initialize the main tunnel path.
     * @param uuid a non-null unique identifier (UUID) for this TunnelStructure.
     */
    public TunnelStructure(@NotNull List<TunnelUnit> units, @NotNull UUID uuid) {
        this.mainTunnelPath = new TunnelPath();
        mainTunnelPath.getUnits().addAll(units);
        this.uuid = uuid;
    }

    /**
     * Checks if the specified TunnelUnit is contained within the main tunnel path
     * of this TunnelStructure. The containment is determined by verifying if there
     * exists a TunnelUnit in the main tunnel path that is equal to the given TunnelUnit.
     *
     * @param tunnelUnit the TunnelUnit to check for containment within the main tunnel path; must not be null
     * @return true if the specified TunnelUnit is contained in the main tunnel path, false otherwise
     */
    public boolean isContained(@NotNull TunnelUnit tunnelUnit) {
        return this.mainTunnelPath.getUnits().stream().anyMatch(tunnelUnit::equals);
    }

    /**
     * Retrieves a TunnelUnit from the main tunnel path that is equal to the provided TunnelUnit.
     * Equality is determined using the {@link TunnelUnit#equals(Object)} method, which compares
     * the X and Z coordinates of the TunnelUnit.
     *
     * @param tunnelUnit the TunnelUnit to search for within the main tunnel path; must not be null.
     * @return the TunnelUnit from the main tunnel path that matches the provided TunnelUnit, or null if no match is found.
     */
    @Nullable
    public TunnelUnit getContained(@NotNull TunnelUnit tunnelUnit) {
        return this.mainTunnelPath.getUnits().stream().filter(tunnelUnit::equals).findAny().get();
    }

    /**
     * Determines if the specified TunnelUnit is adjacent to any of the TunnelUnits
     * within the main tunnel path. Adjacency is determined using the Manhattan distance
     * metric in a 2D plane, where the distance is 1.
     *
     * @param tunnelUnit the TunnelUnit to check for adjacency; must not be null.
     * @return true if the specified TunnelUnit is adjacent to any of the TunnelUnits
     *         in the main tunnel path, false otherwise.
     */
    public boolean isAdjacent(@NotNull TunnelUnit tunnelUnit) {
        return this.mainTunnelPath.getUnits().stream().anyMatch(v -> Utils.manhattanDistance2D(v,tunnelUnit) == 1);
    }

    /**
     * Retrieves the main tunnel path associated with this structure.
     *
     * @return the main {@link TunnelPath} instance representing the primary path of this tunnel structure
     */
    @NotNull
    public TunnelPath getMainTunnelPath() {
        return mainTunnelPath;
    }

    /**
     * Sets the main tunnel path for the tunnel structure.
     *
     * @param mainTunnelPath the new main tunnel path to assign, must not be null
     */
    public void setMainTunnelPath(@NotNull TunnelPath mainTunnelPath) {
        this.mainTunnelPath = mainTunnelPath;
    }

    /**
     * Retrieves the unique identifier (UUID) associated with this instance.
     *
     * @return a non-null UUID representing the unique identifier of this instance
     */
    @NotNull
    public UUID getUuid() {
        return uuid;
    }

    /**
     * Compares the provided object with this {@code TunnelStructure} instance for equality.
     * The comparison is based on the unique {@code UUID} of the {@code TunnelStructure}.
     *
     * @param o the object to compare with this instance
     * @return {@code true} if the specified object is a {@code TunnelStructure} and has the same {@code UUID},
     *         {@code false} otherwise
     */
    @Override
    public boolean equals(@NotNull Object o) {
        if (!(o instanceof TunnelStructure that)) return false;
        return Objects.equals(uuid, that.uuid);
    }

    /**
     * Computes the hash code for this object based on its unique identifier (UUID).
     *
     * @return the hash code value for this object, computed using the UUID.
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(uuid);
    }
}