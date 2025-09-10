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