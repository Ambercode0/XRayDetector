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
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Represents a single unit within a tunnel system, characterized by its coordinates
 * in the XZ plane, the material it consists of, whether it is exposed to air,
 * and the time at which it was mined.
 * <p>
 * This class provides methods to retrieve and modify the material of the unit,
 * check for adjacency with other tunnel units, and determine if the material is an ore.
 */
public class TunnelUnit {

    private final int x,z;
    private Material material;
    private final long minedAt;
    private boolean exposedToAir;

    public TunnelUnit(int x, int z, @NotNull Material material, long minedAt) {
        this.x = x;
        this.z = z;
        this.material = material;
        this.minedAt = minedAt;
    }

    /**
     * Retrieves the material associated with this tunnel unit.
     *
     * @return the material of the tunnel unit, guaranteed to be non-null.
     */
    @NotNull
    public Material getMaterial() {
        return material;
    }

    /**
     * Updates the material associated with this TunnelUnit.
     *
     * @param material the new material to assign to this TunnelUnit; must not be null
     */
    public void setMaterial(@NotNull Material material) {
        this.material = material;
    }

    /**
     * Retrieves the X-coordinate of this TunnelUnit in the tunnel system's XZ plane.
     *
     * @return the X-coordinate of the TunnelUnit.
     */
    public int getX() {
        return x;
    }

    /**
     * Retrieves the Z-coordinate of this TunnelUnit in the tunnel system's XZ plane.
     *
     * @return the Z-coordinate of the TunnelUnit.
     */
    public int getZ() {
        return z;
    }

    /**
     * Retrieves the timestamp representing when this TunnelUnit was mined.
     *
     * @return the timestamp in milliseconds at which this TunnelUnit was mined.
     */
    public long getMinedAt() {
        return minedAt;
    }

    /**
     * Checks whether this TunnelUnit is exposed to air.
     *
     * @return true if this TunnelUnit is exposed to air, false otherwise.
     */
    public boolean isExposedToAir() {
        return exposedToAir;
    }

    /**
     * Sets whether this TunnelUnit is exposed to air. This can be used to mark blocks
     * that are directly accessible from air or other open spaces.
     *
     * @param exposedToAir true if the TunnelUnit is exposed to air, false otherwise.
     */
    public void setExposedToAir(boolean exposedToAir) {
        this.exposedToAir = exposedToAir;
    }

    /**
     * Determines whether the material of this TunnelUnit is classified as an ore.
     *
     * @return true if the material of this TunnelUnit is classified as an ore; false otherwise.
     */
    public boolean isOre() {
        return Utils.isOre(material);
    }

    /**
     * Compares this TunnelUnit instance to another object to determine equality.
     * Two TunnelUnit instances are considered equal if their x and z coordinates are identical.
     *
     * @param o the object to be compared for equality with this TunnelUnit, must not be null.
     * @return true if the specified object is equal to this TunnelUnit; false otherwise.
     */
    @Override
    public boolean equals(@NotNull Object o) {
        if (!(o instanceof final TunnelUnit that)) return false;
        return x == that.x && z == that.z;
    }

    /**
     * Computes the hash code for this TunnelUnit instance based on its x and z coordinates.
     *
     * @return an integer representing the hash code derived from the x and z coordinates.
     */
    @Override
    public int hashCode() {
        return Objects.hash(x, z);
    }

    /**
     * check if another unit is adjacent to this one.
     * NOTE: this does not check whether it's already contained in a path.
     *
     * @param unit the external unit.
     * @return true if manhattan distance is 1, false otherwise.
     */
    public boolean checkAdjacent(@NotNull TunnelUnit unit) {
        return Utils.manhattanDistance2D(this, unit) == 1;
    }

}