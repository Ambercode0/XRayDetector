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

// Represents a single mined block in the tunnel system using XZ plane simplified model
public class TunnelUnit {

    private final int x,z;
    private Material material;
    private final long minedAt;
    private boolean exposedToAir;

    public TunnelUnit(int x, int z, Material material, long minedAt) {
        this.x = x;
        this.z = z;
        this.material = material;
        this.minedAt = minedAt;
    }

    @NotNull
    public Material getMaterial() {
        return material;
    }

    public void setMaterial(@NotNull Material material) {
        this.material = material;
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    public long getMinedAt() {
        return minedAt;
    }

    public boolean isExposedToAir() {
        return exposedToAir;
    }

    public void setExposedToAir(boolean exposedToAir) {
        this.exposedToAir = exposedToAir;
    }

    public boolean isOre() {
        return Utils.isOre(material);
    }

    @Override
    public boolean equals(@NotNull Object o) {
        if (!(o instanceof final TunnelUnit that)) return false;
        return x == that.x && z == that.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, z);
    }

    /**
     * check if another unit is adjacent to this one.
     * NOTE: this does not check whether it's already contained in path.
     *
     * @param unit the external unit.
     * @return true if manhattan distance is 1, false otherwise.
     */
    public boolean checkAdjacent(@NotNull TunnelUnit unit) {
        return Utils.manhattanDistance2D(this, unit) == 1;
    }

}