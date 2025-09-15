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
import org.bukkit.block.Block;
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

    /**
     * Adds a specified OreVein to the TunnelPath, integrating all its associated TunnelUnits
     * into the path and mapping them to the provided OreVein. Any TunnelUnit in the OreVein
     * that is not already part of the TunnelPath will be validated and assigned appropriately.
     *
     * @param vein the OreVein to be added, containing TunnelUnits to be associated with the path; must not be null.
     */
    public void addVein(@NotNull OreVein vein) {
        // Optionally validate all units of the vein are in this path and not already assigned
        veins.add(vein);
        for (TunnelUnit u : vein.units()) {
            assignUnitToVein(u, vein);
        }
    }

    /**
     * Retrieves the OreVein associated with the given TunnelUnit. If no existing OreVein is found,
     * a new OreVein is created based on the ores associated with the provided Block,
     * and the TunnelUnit is assigned to this new OreVein.
     *
     * @param unit the TunnelUnit for which the OreVein is to be retrieved or created; must not be null.
     * @param block the Block used to generate a new OreVein if an existing one is not found; must not be null.
     * @return the existing or newly created OreVein associated with the provided TunnelUnit; never null.
     */
    @NotNull
    public OreVein getOrCreateVein(@NotNull TunnelUnit unit, @NotNull Block block) {
        return veinOf(unit).orElseGet(() -> {
            List<TunnelUnit> list = Utils.getOreVein(block);
            OreVein newVein = new OreVein(list);
            assignUnitToVein(unit, newVein);
            veins.add(newVein);
            return newVein;
        });
    }

    /**
     * Assigns a given {@link TunnelUnit} to a specified {@link OreVein}.
     * If the {@link TunnelUnit} is not part of the current path, an exception is thrown.
     * If the {@link TunnelUnit} is already assigned to a different {@link OreVein}, an exception is thrown.
     * If the {@link TunnelUnit} is not already in the specified {@link OreVein}, it will be added to it.
     *
     * @param u the {@link TunnelUnit} to be assigned; must not be null and must be part of the current path
     * @param v the {@link OreVein} to which the {@link TunnelUnit} is being assigned; must not be null
     * @throws IllegalArgumentException if the {@link TunnelUnit} is not in the current path
     * @throws IllegalStateException if the {@link TunnelUnit} is already assigned to another {@link OreVein}
     */
    public void assignUnitToVein(@NotNull TunnelUnit u, @NotNull OreVein v) {
        if (!units.contains(u)) throw new IllegalArgumentException("Unit not in path");
        OreVein existing = unitToVein.putIfAbsent(u, v);
        if (existing != null && existing != v) {
            throw new IllegalStateException("Unit already assigned to another vein");
        }
        if (!v.contains(u)) v.addUnitInternal(u);
    }

    /**
     * Removes a specified {@link TunnelUnit} from its associated {@link OreVein}.
     * If the unit is not linked to any vein, the method performs no action.
     *
     * @param u the {@link TunnelUnit} to be removed from its associated ore vein; must not be null
     */
    public void removeUnitFromVein(@NotNull TunnelUnit u) {
        OreVein v = unitToVein.remove(u);
        if (v != null) v.removeUnitInternal(u);
    }

    @NotNull
    public UUID getUuid() {
        return id;
    }
}