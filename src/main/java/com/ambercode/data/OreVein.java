package com.ambercode.data;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

public final class OreVein {

    List<TunnelUnit> tunnelUnits = new ArrayList<>();
    private final Material material;

    public OreVein(Material material) {
        this.material = material;
    }

    public void addTunnelUnit(TunnelUnit tunnel) {
        this.tunnelUnits.add(tunnel);
    }

    public Material getMaterial() {
        return material;
    }

    public List<TunnelUnit> getTunnelUnits() {
        return tunnelUnits;
    }
}