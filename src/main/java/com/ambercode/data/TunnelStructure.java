package com.ambercode.data;

import com.ambercode.utils.Utils;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

// Represents the complete tunnel structure with main path and branches
public class TunnelStructure {

    private TunnelPath mainTunnelPath;
    private final UUID uuid;

    public TunnelStructure(@NotNull TunnelUnit origin) {
        this.mainTunnelPath = new TunnelPath(origin);
        this.uuid = UUID.randomUUID();
    }

    public TunnelStructure(@NotNull List<TunnelUnit> units) {
        this.mainTunnelPath = new TunnelPath();
        mainTunnelPath.getUnits().addAll(units);
        this.uuid = UUID.randomUUID();
    }

    public TunnelStructure(@NotNull List<TunnelUnit> units, @NotNull UUID uuid) {
        this.mainTunnelPath = new TunnelPath();
        mainTunnelPath.getUnits().addAll(units);
        this.uuid = uuid;
    }

    public boolean isContained(@NotNull TunnelUnit tunnelUnit) {
        return this.mainTunnelPath.getUnits().stream().anyMatch(tunnelUnit::equals);
    }

    public boolean isAdjacent(@NotNull TunnelUnit tunnelUnit) {
        return this.mainTunnelPath.getUnits().stream().anyMatch(v -> Utils.manhattanDistance2D(v,tunnelUnit) == 1);
    }

    @NotNull
    public TunnelPath getMainTunnelPath() {
        return mainTunnelPath;
    }

    public void setMainTunnelPath(@NotNull TunnelPath mainTunnelPath) {
        this.mainTunnelPath = mainTunnelPath;
    }

    @NotNull
    public UUID getUuid() {
        return uuid;
    }

    @Override
    public boolean equals(@NotNull Object o) {
        if (!(o instanceof TunnelStructure that)) return false;
        return Objects.equals(uuid, that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(uuid);
    }
}