package com.ambercode.data;

import org.jetbrains.annotations.NotNull;

import java.util.*;

// Represents a continuous path of mined blocks
public class TunnelPath {

    private final UUID uuid;
    private final List<TunnelUnit> units = new ArrayList<>();

    public TunnelPath(@NotNull TunnelUnit origin) {
        units.addFirst(origin);
        uuid = UUID.randomUUID();
    }

    public TunnelPath(@NotNull UUID uuid) {
        this.uuid = uuid;
    }

    public TunnelPath() {
        this.uuid = UUID.randomUUID();
    }

    @NotNull
    public List<TunnelUnit> getUnits() {
        return units;
    }

    @NotNull
    public UUID getUuid() {
        return uuid;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TunnelPath that)) return false;
        return Objects.equals(uuid, that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(uuid);
    }
}