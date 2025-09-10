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

package com.ambercode.database;

import com.ambercode.XRayDetector;
import com.ambercode.data.Miner;
import com.ambercode.data.TunnelPath;
import com.ambercode.data.TunnelStructure;
import com.ambercode.data.TunnelUnit;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class MySQLDatabase extends HikariPluginDatabase {

    public MySQLDatabase(@NotNull XRayDetector plugin) {
        super(plugin, DatabaseType.MYSQL);
    }


    @Override
    public void createTables() {

    }

    @Override
    public @NotNull List<Miner> getAllData() {
        return List.of();
    }

    @Override
    public void insertTunnelUnit(@NotNull TunnelUnit tunnelUnit, @NotNull TunnelPath tunnelPath) {

    }

    @Override
    public void insertTunnelPath(@NotNull TunnelPath tunnelPath, @NotNull TunnelStructure tunnelStructure) {

    }

    @Override
    public void insertTunnelStructure(@NotNull TunnelStructure tunnelStructure, @NotNull Miner miner) {

    }

    @Override
    public void insertMiner(@NotNull Miner miner) {

    }

    @Override
    public void mergeStructures(UUID minerUuid, TunnelStructure newMergedStructure, TunnelUnit newTunnelUnit, List<TunnelStructure> oldStructures) {

    }

}
