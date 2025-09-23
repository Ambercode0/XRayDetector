package com.ambercode.data;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class OreVein {

    List<TunnelUnit> tunnelUnits = new ArrayList<>();
    private final Material material;

    public OreVein(@NotNull Material material) {
        this.material = material;

        System.out.println("Created OreVein with material:" + material);
    }

    public void addTunnelUnit(TunnelUnit tunnel) {
        this.tunnelUnits.add(tunnel);
        System.out.println("Added TunnelUnit to OreVein coordinates: " + tunnel.toString());
    }

    public Material getMaterial() {
        return material;
    }

    public List<TunnelUnit> getTunnelUnits() {
        return tunnelUnits;
    }
}