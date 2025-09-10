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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Miner {

    private final UUID uuid;
    private final List<TunnelStructure> createdTunnels = new ArrayList<>();
    private final double suspicionScore = 0.00;

    public Miner(@NotNull UUID uuid) {
        this.uuid = uuid;
    }

    @Nullable
    public TunnelStructure getTunnelStructure(@NotNull UUID uuid) {
        return  createdTunnels.stream().filter(t -> t.getUuid().equals(uuid)).findAny().orElse(null);
    }

    @NotNull
    public UUID getUuid() {
        return uuid;
    }

    @NotNull
    public List<TunnelStructure> getCreatedTunnels() {
        return createdTunnels;
    }

    public double getSuspicionScore() {
        return suspicionScore;
    }

    public int getDiscoveredOreVeins() {
        return +0;
    }

    public int getMinedBlocks() {
        return +0;
    }
}