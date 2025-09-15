package com.ambercode.data;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class OreVein {
    private final UUID id = UUID.randomUUID();
    private final List<TunnelUnit> units = new ArrayList<>();

    @NotNull public UUID id() { return id; }
    @NotNull public List<TunnelUnit> units() { return Collections.unmodifiableList(units); }

    protected boolean contains(@NotNull TunnelUnit u) { return units.contains(u); }
    protected void addUnitInternal(@NotNull TunnelUnit u) { units.add(u); }       // package-private
    protected void removeUnitInternal(@NotNull TunnelUnit u) { units.remove(u); } // package-private
}
