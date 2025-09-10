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

import java.util.*;

/**
 * Represents a path within a tunnel system, consisting of sequential TunnelUnit objects
 * that describe the structure of the tunnel. Each TunnelPath has a unique identifier (UUID)
 * for distinguishing it from other paths.
 * <p>
 * A TunnelPath may be initialized with one or more TunnelUnit objects or constructed
 * with an empty initial state. This class provides functionality to retrieve the list of
 * TunnelUnits and the unique identifier associated with the path. Equality between
 * TunnelPath instances is determined by their UUIDs.
 */
public class TunnelPath {

    private final UUID uuid;
    private final List<TunnelUnit> units = new ArrayList<>();

    public TunnelPath(@NotNull TunnelUnit origin) {
        units.addFirst(origin);
        uuid = UUID.randomUUID();
    }

    /**
     * Constructs a new TunnelPath instance using a specified unique identifier.
     *
     * @param uuid the unique identifier for the tunnel path; must not be null
     */
    public TunnelPath(@NotNull UUID uuid) {
        this.uuid = uuid;
    }

    /**
     * Constructs a new instance of TunnelPath with a randomly generated unique identifier (UUID).
     * <p>
     * This constructor initializes a TunnelPath object without any TunnelUnits
     * and assigns it a unique UUID. It can be used to represent an empty tunnel path
     * that can later have TunnelUnits added to it.
     */
    public TunnelPath() {
        this.uuid = UUID.randomUUID();
    }

    /**
     * Retrieves the list of TunnelUnit objects associated with this TunnelPath.
     * The list represents the sequence of tunnel units making up the path.
     *
     * @return a non-null list of TunnelUnit objects representing the tunnel path.
     */
    @NotNull
    public List<TunnelUnit> getUnits() {
        return units;
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
     * Compares this TunnelPath instance to another object to determine equality.
     * Two TunnelPath instances are considered equal if their UUIDs are identical.
     *
     * @param o the object to be compared for equality with this TunnelPath.
     * @return true if the specified object is equal to this TunnelPath; false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TunnelPath that)) return false;
        return Objects.equals(uuid, that.uuid);
    }

    /**
     * Computes the hash code for this TunnelPath instance based on its UUID.
     *
     * @return an integer representing the hash code derived from the UUID.
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(uuid);
    }
}