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

import com.ambercode.data.Miner;
import com.ambercode.data.TunnelPath;
import com.ambercode.data.TunnelStructure;
import com.ambercode.data.TunnelUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public interface PluginDatabase {

    /**
     * Load the necessary driver classes used by the database.
     * When not necessary, leave implementation empty.
     */
    default void loadDriverClass() {
    }

    /**
     * Perform a connection to the database server.
     */
    void connect();

    /**
     * Creates the tables necessarily used by this software.
     * Do not rewrite the table if already exists.
     */
    void createTables();

    /**
     * Get the connection to the database.
     *
     * @return Connection to the database, null if you could not achieve connection.
     */
    @Nullable
    Connection getConnection();

    default void close() {
        try {
            Connection connection = getConnection();
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    @NotNull
    List<Miner> getAllData();

    void insertTunnelUnit(@NotNull TunnelUnit tunnelUnit, @NotNull TunnelPath tunnelPath);

    void insertTunnelPath(@NotNull TunnelPath tunnelPath, @NotNull TunnelStructure tunnelStructure);

    void insertTunnelStructure(@NotNull TunnelStructure tunnelStructure, @NotNull Miner miner);

    void insertMiner(@NotNull Miner miner);

    void mergeStructures(@NotNull UUID minerUuid,
                         @NotNull TunnelStructure newMergedStructure,
                         @NotNull TunnelUnit newTunnelUnit,
                         @NotNull List<TunnelStructure> oldStructures);

    boolean deleteMinerData(@NotNull UUID minerUuid);

    boolean deleteTunnelStructureData(@NotNull UUID stuctureUuid);

}