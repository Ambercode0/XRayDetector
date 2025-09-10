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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    @Nullable
    public TunnelUnit getContained(@NotNull TunnelUnit tunnelUnit) {
        return this.mainTunnelPath.getUnits().stream().filter(tunnelUnit::equals).findAny().get();
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