package com.ambercode.data;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class TunnelUnit {

    private final int x,y,z;
    private final Material material;
    private final boolean isExposed;
    private final long minedAt;

    public TunnelUnit(int x, int y, int z, @NotNull Material material, boolean isExposed, long minedAt) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.material = material;
        this.isExposed = isExposed;
        this.minedAt = minedAt;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    @NotNull
    public Material getMaterial() {
        return material;
    }

    public boolean isExposed() {
        return isExposed;
    }

    public long getMinedAt() {
        return minedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TunnelUnit that)) return false;
        return x == that.x && y == that.y && z == that.z && material == that.material;
    }

    public boolean isSame(@NotNull Block block) {
        return block.getX() == x && block.getY() == y && block.getZ() == z && block.getType() == material;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z, material, isExposed, minedAt);
    }

    @Override
    public String toString() {
        return "TunnelUnit{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", material=" + material +
                ", isExposed=" + isExposed +
                ", minedAt=" + minedAt +
                '}';
    }
}
