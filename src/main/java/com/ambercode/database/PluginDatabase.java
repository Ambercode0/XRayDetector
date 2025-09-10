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

    public void mergeStructures(UUID minerUuid,
                                TunnelStructure newMergedStructure,
                                TunnelUnit newTunnelUnit,
                                List<TunnelStructure> oldStructures);
}