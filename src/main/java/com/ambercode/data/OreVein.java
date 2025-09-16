package com.ambercode.data;

import org.jetbrains.annotations.NotNull;

import java.util.*;

public class OreVein {
    private final UUID id;
    private final List<TunnelUnit> units = new ArrayList<>();

    public OreVein() {
        this(UUID.randomUUID());
    }
    public OreVein(@NotNull UUID uuid) {
        this.id = uuid;
    }
    public OreVein(@NotNull Collection<TunnelUnit> units) {
        this();
        this.units.addAll(units);
    }

    @NotNull public UUID getUuid() { return id; }
    @NotNull public List<TunnelUnit> units() { return Collections.unmodifiableList(units); }

    protected boolean contains(@NotNull TunnelUnit u) { return units.contains(u); }
    protected void addUnitInternal(@NotNull TunnelUnit u) { units.add(u); }       // package-private
    protected void removeUnitInternal(@NotNull TunnelUnit u) { units.remove(u); } // package-private

    public int size() { return units.size(); }
    public boolean isEmpty() { return units.isEmpty(); }
    public boolean containsAll(@NotNull Collection<TunnelUnit> units) { return this.units.containsAll(units); }
    public boolean containsAny(@NotNull Collection<TunnelUnit> units) { return units.stream().anyMatch(this::contains); }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof OreVein oreVein)) return false;
        return Objects.equals(id, oreVein.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
