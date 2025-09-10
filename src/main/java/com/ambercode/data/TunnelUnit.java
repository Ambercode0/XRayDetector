package com.ambercode.data;

import com.ambercode.utils.Utils;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.*;

// Represents a single mined block in the tunnel system using XZ plane simplified model
public class TunnelUnit {

    private final int x,z;
    private final Material material;
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