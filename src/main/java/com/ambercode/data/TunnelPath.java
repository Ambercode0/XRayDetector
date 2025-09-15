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

import java.util.*;

public final class TunnelPath {
    private final UUID id;
    private final List<TunnelUnit> units = new ArrayList<>();
    private final List<OreVein> veins = new ArrayList<>();
    private final Map<TunnelUnit, OreVein> unitToVein = new HashMap<>();

    public TunnelPath() {
        this(UUID.randomUUID());
    }

    public TunnelPath(@NotNull TunnelUnit unit) {
        this();
        add(unit);
    }

    public TunnelPath(@NotNull UUID uuid) {
        this.id = uuid;
    }

    private List<TunnelUnit> getUnits() { return this.units; }
    public List<OreVein> getOreVeins() { return Collections.unmodifiableList(veins); }

    public int veinsSize() {
        return veins.size();
    }

    public int unitsSize() {
        return units.size();
    }

    public boolean add(@NotNull TunnelUnit unit) {
        return this.units.add(unit);
    }

    public void addLast(@NotNull TunnelUnit unit) {
        this.units.addLast(unit);
    }

    public boolean addAll(@NotNull List<TunnelUnit> units) {
        return this.units.addAll(units);
    }

    public boolean addAll(@NotNull TunnelStructure structure) {
        return addAll(structure.getMainTunnelPath().units);
    }

    public boolean contains(@NotNull TunnelUnit unit) {
        return this.units.contains(unit);
    }

    @Nullable
    public TunnelUnit getContained(@NotNull TunnelUnit unit) {
        for (final TunnelUnit u : units)
            if (u.equals(unit))
                return u;
        return null;
    }

    public boolean isAdjacent(@NotNull TunnelUnit unit) {
        for (final TunnelUnit u : units)
            if (Utils.manhattanDistance2D(u, unit) == 1)
                return true;
        return false;
    }

    @NotNull
    public Optional<OreVein> veinOf(@NotNull TunnelUnit u) {
        return Optional.ofNullable(unitToVein.get(u));
    }

    public void addVein(@NotNull OreVein vein) {
        // Optionally validate all units of the vein are in this path and not already assigned
        veins.add(vein);
        for (TunnelUnit u : vein.units()) {
            assignUnitToVein(u, vein);
        }
    }

    public void assignUnitToVein(@NotNull TunnelUnit u, @NotNull OreVein v) {
        if (!units.contains(u)) throw new IllegalArgumentException("Unit not in path");
        OreVein existing = unitToVein.putIfAbsent(u, v);
        if (existing != null && existing != v) {
            throw new IllegalStateException("Unit already assigned to another vein");
        }
        if (!v.contains(u)) v.addUnitInternal(u);
    }

    public void removeUnitFromVein(@NotNull TunnelUnit u) {
        OreVein v = unitToVein.remove(u);
        if (v != null) v.removeUnitInternal(u);
    }

    @NotNull
    public UUID getUuid() {
        return id;
    }
}