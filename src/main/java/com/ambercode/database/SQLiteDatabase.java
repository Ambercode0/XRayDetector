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
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.*;

public class SQLiteDatabase extends CredentialPluginDatabase {

        /**
         * SQL statement for creating the "miners" table in the database.
         * <p>
         * The "miners" table stores information related to individual miners, including
         * - A unique UUID for identifying each miner.
         * - A suspicion score representing a numerical assessment of the miner's activities.
         * - Timestamps for record creation and last update.
         * <p>
         * Table schema:
         * - `uuid` (TEXT): Primary key and unique identifier for each miner.
         * - `suspicion_score` (REAL): Score indicating the miner's level of suspicion, cannot be null and defaults to 0.0.
         * - `created_at` (TIMESTAMP): Automatically set to the current timestamp upon record creation.
         * - `updated_at` (TIMESTAMP): Automatically set to the current timestamp upon record update.
         * <p>
         * Implements `CREATE TABLE IF NOT EXISTS` syntax to ensure the table is created only if it does not already exist.
         */
        // Table creation SQL statements
        private static final String CREATE_TABLE_MINERS = """
            CREATE TABLE IF NOT EXISTS miners (
                uuid TEXT PRIMARY KEY UNIQUE,
                suspicion_score REAL NOT NULL DEFAULT 0.0,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            );
            """;

        /**
         * SQL statement for creating the "tunnel_paths" table in the SQLite database.
         * <p>
         * This table stores information about tunnel paths, uniquely identified by a UUID.
         * Each record in the table is associated with a specific tunnel structure, as defined
         * by the "structure_uuid" foreign key, which references the "uuid" column in the
         * "tunnel_structures" table. The foreign key constraint enforces a cascading delete,
         * ensuring that tunnel paths are automatically removed when their associated tunnel
         * structure is deleted.
         * <p>
         * Table schema:
         * - `uuid`: A unique identifier for the tunnel path (TEXT, PRIMARY KEY, UNIQUE).
         * - `structure_uuid`: The UUID of the associated tunnel structure (TEXT, NOT NULL).
         * - `created_at`: A timestamp indicating when the record was created. Defaults to
         *   the current timestamp at the time of insertion.
         */
        private static final String CREATE_TABLE_PATHS = """
            CREATE TABLE IF NOT EXISTS tunnel_paths (
                uuid TEXT PRIMARY KEY UNIQUE,
                structure_uuid TEXT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (structure_uuid) REFERENCES tunnel_structures(uuid) ON DELETE CASCADE
            );
            """;

        /**
         * SQL statement to create the "tunnel_structures" table within the SQLite database.
         * This table stores information about tunnel structures associated with miners.
         * <p>
         * Schema:
         * - `uuid` (TEXT): Primary key and unique identifier for the tunnel structure.
         * - `miner_uuid` (TEXT): Foreign key referencing the UUID of the associated miner in the "miners" table.
         * - `created_at` (TIMESTAMP): Timestamp indicating when the tunnel structure was created, defaults to the current time.
         * <p>
         * Constraints:
         * - Foreign key constraint ensures that a valid miner exists for every tunnel structure,
         *   and cascades deletion of the structure when the associated miner is deleted.
         * <p>
         * The table is created only if it does not already exist.
         */
        private static final String CREATE_TABLE_STRUCTURES = """
            CREATE TABLE IF NOT EXISTS tunnel_structures (
                uuid TEXT PRIMARY KEY UNIQUE,
                miner_uuid TEXT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (miner_uuid) REFERENCES miners(uuid) ON DELETE CASCADE
            );
            """;

        /**
         * SQL statement for creating the 'tunnel_units' table in the SQLite database.
         * This table stores information about individual units in a tunnel, such as their
         * location, material, and metadata. The statement ensures the table is created
         * only if it does not already exist.
         * <p>
         * Table schema:
         * - id: The unique identifier for each tunnel unit (primary key, autoincremented).
         * - path_uuid: The UUID of the tunnel path this unit is associated with (foreign key).
         * - x: The x-coordinate of the unit within the tunnel path.
         * - z: The z-coordinate of the unit within the tunnel path.
         * - material: The material type of the unit.
         * - exposed: A boolean indicating if the unit is exposed.
         * - world_name: The name of the world the unit is located in.
         * - mined_at: A timestamp (epoch milliseconds) representing when the unit was mined.
         * - created_at: The date and time when the unit entry was created (timestamp, defaults to the current time).
         * <p>
         * Constraints:
         * - A foreign key constraint on 'path_uuid' linking to the 'uuid' field in the 'tunnel_paths' table. The
         *   referenced entry is deleted if the associated tunnel path is removed (`ON DELETE CASCADE`).
         * - A unique constraint on the combination of 'path_uuid', 'x', and 'z' to prevent duplicate units
         *   within the same tunnel path at the same coordinates.
         */
        private static final String CREATE_TABLE_UNITS = """
            CREATE TABLE IF NOT EXISTS tunnel_units (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                path_uuid TEXT NOT NULL,
                x INTEGER NOT NULL,
                z INTEGER NOT NULL,
                material TEXT NOT NULL,
                exposed BOOLEAN NOT NULL,
                world_name TEXT NOT NULL,
                mined_at BIGINT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (path_uuid) REFERENCES tunnel_paths(uuid) ON DELETE CASCADE,
                UNIQUE(path_uuid, x, z)
            );
            """;

        /**
         * SQL script containing the creation statements for multiple database indexes used to
         * optimize query performance in the application's SQLite database.
         * These indexes correspond to specific fields across several database tables and
         * are created if they do not already exist.
         * <p>
         * Index definitions included:
         * - idx_miners_suspicion: Optimizes queries on the "suspicion_score" column in the "miners" table.
         * - idx_tunnel_paths_miner: Optimizes queries on the "miner_uuid" column in the "tunnel_paths" table.
         * - idx_tunnel_units_path: Optimizes queries on the "path_uuid" column in the "tunnel_units" table.
         * - idx_tunnel_units_position: Optimizes queries involving the "x" and "z" columns in the "tunnel_units" table.
         * - idx_tunnel_units_material: Optimizes queries on the "material" column in the "tunnel_units" table.
         * - idx_tunnel_units_exposed: Optimizes queries on the "exposed" column in the "tunnel_units" table.
         * - idx_tunnel_units_mined_at: Optimizes queries on the "mined_at" column in the "tunnel_units" table.
         */
        private static final String INDEXES = """
            CREATE INDEX IF NOT EXISTS idx_miners_suspicion ON miners(suspicion_score);
            CREATE INDEX IF NOT EXISTS idx_tunnel_paths_miner ON tunnel_paths(miner_uuid);
            CREATE INDEX IF NOT EXISTS idx_tunnel_units_path ON tunnel_units(path_uuid);
            CREATE INDEX IF NOT EXISTS idx_tunnel_units_position ON tunnel_units(x, z);
            CREATE INDEX IF NOT EXISTS idx_tunnel_units_material ON tunnel_units(material);
            CREATE INDEX IF NOT EXISTS idx_tunnel_units_exposed ON tunnel_units(exposed);
            CREATE INDEX IF NOT EXISTS idx_tunnel_units_world_name ON tunnel_units(world_name);
            CREATE INDEX IF NOT EXISTS idx_tunnel_units_mined_at ON tunnel_units(mined_at);
            """;

        /**
         * SQL query string used to retrieve and cache all hierarchical data related to miners,
         * including associated tunnel structures, paths, and units, from the database.
         * <p>
         * This query is designed to perform multiple left joins across the `miners`, `tunnel_structures`,
         * `tunnel_paths`, and `tunnel_units` tables to gather all relevant data in a single operation.
         * The resulting dataset includes details such as UUIDs, creation and update timestamps,
         * suspicion scores, material types, and mining-related information, organized and ordered
         * hierarchically by miner, structure, path, and mining timestamp.
         * <p>
         * Key parts of the query:
         * - The main table is `miners`, representing the primary data source.
         * - `LEFT JOIN` operations are used to incorporate related data from `tunnel_structures`,
         *   `tunnel_paths`, and `tunnel_units` tables, ensuring all miners are represented even
         *   if they lack associated structures, paths, or units.
         * - Data ordering is performed by miner UUIDs, structure UUIDs, path UUIDs, and unit mining timestamps
         *   to maintain hierarchical and chronological consistency.
         * <p>
         * Purpose:
         * - Primarily used to fetch all relevant miner-related data for application processing or caching,
         *   enabling rapid and efficient access to complex hierarchical relationships.
         * <p>
         * Limitations:
         * - The query assumes that UUIDs and timestamps are the primary identifiers and sorting criteria.
         * - It may return a large dataset depending on the size and relationships within the database,
         *   which could impact performance for particularly large or complex miner datasets.
         */
        private static final String CACHE_ALL_DATA_QUERY = """
            SELECT
                m.uuid as miner_uuid,
                m.suspicion_score,
                m.created_at as miner_created_at,
                m.updated_at as miner_updated_at,
              \s
                ts.uuid as structure_uuid,
                ts.created_at as structure_created_at,
              \s
                tp.uuid as path_uuid,
                tp.created_at as path_created_at,
              \s
                tu.id as unit_id,
                tu.x as unit_x,
                tu.z as unit_z,
                tu.material as unit_material,
                tu.exposed as unit_exposed,
                tu.world_name as unit_world_name,
                tu.mined_at as unit_mined_at,
                tu.created_at as unit_created_at
              \s
            FROM miners m
            LEFT JOIN tunnel_structures ts ON m.uuid = ts.miner_uuid
            LEFT JOIN tunnel_paths tp ON ts.uuid = tp.structure_uuid
            LEFT JOIN tunnel_units tu ON tp.uuid = tu.path_uuid
            ORDER BY m.uuid, ts.uuid, tp.uuid, tu.mined_at
           \s""";

        /**
         * The SQL query that inserts a new tunnel unit into the database.
         * This query creates a record in the `tunnel_units` table, populating fields such as:
         * - `path_uuid`: The UUID of the associated tunnel path.
         * - `x`: The x-coordinate of the tunnel unit.
         * - `z`: The z-coordinate of the tunnel unit.
         * - `material`: The material type of the tunnel unit.
         * - `exposed`: Whether the unit is exposed or not.
         * - `world_name`: The name of the world the unit is located in.
         * - `mined_at`: The timestamp when the unit was mined.
         * - `created_at`: The timestamp when the record was created (set to the current timestamp).
         * <p>
         * The query utilizes prepared statement placeholders (`?`) for parameterized values to
         * prevent SQL injection and ensure efficient database interaction.
         */
        private static final String INSERT_UNIT = """
            INSERT INTO tunnel_units (path_uuid, x, z, material, exposed, world_name ,mined_at, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP);
            """;

        /**
         * SQL statement used to insert a new tunnel path into the database. This statement adds
         * a record to the `tunnel_paths` table with the specified unique identifiers for the
         * tunnel path and its associated structure. The creation timestamp is automatically
         * set to the current database time.
         *
         * Table: tunnel_paths
         * Columns:
         * - `uuid`: Unique identifier for the tunnel path.
         * - `structure_uuid`: Unique identifier of the associated structure.
         * - `created_at`: Timestamp of when the record is created.
         *
         * Usage Context:
         * This constant is used within the database layer to execute insertion
         * operations for associating a new tunnel path with a structure.
         */
        private static final String INSERT_PATH = """
            INSERT INTO tunnel_paths (uuid, structure_uuid, created_at)
            VALUES (?, ?, CURRENT_TIMESTAMP);
            """;

        /**
         * SQL query template for inserting a new record into the `tunnel_structures` table.
         * This query is used to store a tunnel structure's unique identifier, the associated miner's identifier,
         * and a timestamp indicating when the record was created.
         *
         * Fields being inserted:
         * - `uuid`: The unique identifier of the tunnel structure.
         * - `miner_uuid`: The unique identifier of the miner associated with this structure.
         * - `created_at`: Automatically set to the current timestamp.
         *
         * The placeholders (`?`) are parameterized to allow the insertion of dynamic values at runtime.
         */
        private static final String INSERT_STRUCTURE = """
            INSERT INTO tunnel_structures (uuid, miner_uuid, created_at)
            VALUES (?, ?, CURRENT_TIMESTAMP);
            """;

        /**
         * SQL statement for inserting a new miner into the "miners" table of the database.
         * This statement adds a miner's unique identifier (UUID), suspicion score, and timestamps
         * for creation and last update. The timestamps are automatically set to the current time
         * at the moment of insertion.
         *
         * The placeholders (?) in the query are filled with the values provided during the execution
         * of the prepared statement.
         *
         * Structure of the insertion:
         * - 'uuid': Represents the unique identifier of the miner.
         * - 'suspicion_score': Represents the suspicion score related to the miner.
         * - 'created_at': Timestamp of when the miner record is created (set automatically).
         * - 'updated_at': Timestamp of when the miner record is last updated (set automatically).
         */
        private static final String INSERT_MINER = """
            INSERT INTO miners (uuid, suspicion_score, created_at, updated_at)
            VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
            """;

        public SQLiteDatabase(@NotNull XRayDetector plugin) {
            super(plugin, DatabaseType.SQLITE);
        }

        /**
         * Generates the SQLite connection URL for the application's database. If the database file
         * does not exist, it attempts to create a new file in the plugin's data folder. In case of
         * an error during file creation, the exception is silently handled without interrupting the flow.
         *
         * @return A non-null connection URL string in the format "jdbc:sqlite:<path_to_database_file>".
         */
        @Override
        @NotNull
        protected String createConnectionUrl() {
            File pluginDataFolder = super.plugin.getDataFolder();
            File dbFile = new File(pluginDataFolder, "xraydetector.sqlite");

            if (!dbFile.exists()) {
                try {
                    boolean ignored = dbFile.createNewFile();
                } catch (IOException exception) {
                    // logError("createConnectionUrl", "Error during SQLite file creation", exception);
                }
            }

            return "jdbc:sqlite:" + dbFile;
        }

        /**
         * Creates the necessary database tables and indexes for the plugin. This method ensures
         * that foreign key constraints are enforced and sets up the required schema for proper
         * functioning of the application.
         * <p>
         * Database tables and indexes created:
         * - Miners table
         * - Structures table
         * - Paths table
         * - Units table
         * - Relevant indexes
         * <p>
         * In case of a failure during table creation, the error is logged and, if configured to do so,
         * the server will shut down to prevent further operation on an uninitialized database.
         * This behavior is controlled by the {@code database.crash-shutdown} configuration setting.
         * <p>
         * Implementation Notes:
         * - `PRAGMA foreign_keys = ON;` ensures foreign key support for SQLite.
         * - SQL exceptions are logged for debugging, including detailed error information.
         */
        @Override
        public void createTables() {
            final String methodName = "createTables";

            try (final Statement statement = super.connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON;");
                statement.execute(CREATE_TABLE_MINERS);
                statement.execute(CREATE_TABLE_STRUCTURES);
                statement.execute(CREATE_TABLE_PATHS);
                statement.execute(CREATE_TABLE_UNITS);
                statement.execute(INDEXES);

                super.plugin.getLogger().info("Database tables created successfully");
            } catch (SQLException exception) {
                logError(methodName, "Error during table creation", exception);

                if (super.plugin.getStandardConfig().isCrashShutdown()) {
                    super.plugin.getLogger().severe("Shutting down server due to database initialization failure...");
                    super.plugin.getServer().shutdown();
                }
            }
        }

        /**
         * Retrieves all available data from the database, constructing and returning a list of Miner objects.
         * Each Miner may include associated TunnelStructures and TunnelPaths, with all relevant details populated.
         * If an error occurs during data retrieval, it is logged, and an empty list is returned.
         *
         * @return A non-null list of Miner objects, possibly with nested TunnelStructures and TunnelPaths,
         *         or an empty list if an exception occurs.
         */
        @Override
        public @NotNull List<Miner> getAllData() {
            final String methodName = "getAllData";
            Map<UUID, Miner> miners = new LinkedHashMap<>();
            Map<UUID, TunnelStructure> structures = new HashMap<>();
            Map<UUID, TunnelPath> paths = new HashMap<>();

            try (PreparedStatement statement = connection.prepareStatement(CACHE_ALL_DATA_QUERY);
                 ResultSet rs = statement.executeQuery()) {

                while (rs.next()) {
                    String minerUuidStr = rs.getString("miner_uuid");
                    UUID minerUuid = UUID.fromString(minerUuidStr);
                    Miner miner = miners.computeIfAbsent(minerUuid, Miner::new);

                    String structureUuidStr = rs.getString("structure_uuid");
                    if (structureUuidStr != null) {
                        UUID structureUuid = UUID.fromString(structureUuidStr);

                        TunnelStructure structure = structures.get(structureUuid);
                        if (structure == null) {
                            structure = new TunnelStructure(new ArrayList<>(), structureUuid);
                            structures.put(structureUuid, structure);
                            miner.getCreatedTunnels().add(structure);
                        }

                        String pathUuidStr = rs.getString("path_uuid");
                        if (pathUuidStr != null) {
                            UUID pathUuid = UUID.fromString(pathUuidStr);

                            TunnelPath path = paths.get(pathUuid);
                            if (path == null) {
                                path = new TunnelPath(pathUuid);
                                paths.put(pathUuid, path);
                                structure.setMainTunnelPath(path);
                            }

                            if (rs.getObject("unit_id") != null) {
                                int x = rs.getInt("unit_x");
                                int z = rs.getInt("unit_z");
                                String materialStr = rs.getString("unit_material");
                                boolean exposed = rs.getBoolean("unit_exposed");
                                String worldName = rs.getString("unit_world_name");
                                long minedAt = rs.getLong("unit_mined_at");

                                Material material = Material.getMaterial(materialStr);
                                if (material != null) {
                                    TunnelUnit unit = new TunnelUnit(x, z, material, minedAt, worldName);
                                    unit.setExposedToAir(exposed);

                                    if (!path.getUnits().contains(unit)) {
                                        path.getUnits().add(unit);
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (SQLException exception) {
                logError(methodName, "Error retrieving all data", exception);
            }

            return new ArrayList<>(miners.values());
        }

        /**
         * Inserts a tunnel unit into the database and associates it with the specified tunnel path.
         *
         * @param tunnelUnit The {@link TunnelUnit} instance representing the unit to be added. Must not be null.
         * @param tunnelPath The {@link TunnelPath} instance representing the path the unit belongs to. Must not be null.
         */
        @Override
        public void insertTunnelUnit(@NotNull TunnelUnit tunnelUnit, @NotNull TunnelPath tunnelPath) {
            final String methodName = "insertTunnelUnit";

            try (PreparedStatement ps = connection.prepareStatement(INSERT_UNIT)) {
                ps.setString(1, tunnelPath.getUuid().toString());
                ps.setInt(2, tunnelUnit.getX());
                ps.setInt(3, tunnelUnit.getZ());
                ps.setString(4, tunnelUnit.getMaterial().toString());
                ps.setBoolean(5, tunnelUnit.isExposedToAir());
                ps.setString(6, tunnelUnit.getWorldName());
                ps.setLong(7, tunnelUnit.getMinedAt());
                ps.executeUpdate();
            } catch (SQLException exception) {
                logError(methodName, "Error inserting tunnel unit", exception);
            }
        }

        /**
         * Inserts a tunnel path into the database and associates it with the given tunnel structure.
         *
         * @param tunnelPath The {@link TunnelPath} instance representing the path to be inserted. Must not be null.
         * @param tunnelStructure The {@link TunnelStructure} instance representing the structure to associate the path with. Must not be null.
         */
        @Override
        public void insertTunnelPath(@NotNull TunnelPath tunnelPath, @NotNull TunnelStructure tunnelStructure) {
            final String methodName = "insertTunnelPath";

            try (PreparedStatement ps = connection.prepareStatement(INSERT_PATH)) {
                ps.setString(1, tunnelPath.getUuid().toString());
                ps.setString(2, tunnelStructure.getUuid().toString());
                ps.executeUpdate();
            } catch (SQLException exception) {
                logError(methodName, "Error inserting tunnel path", exception);
            }
        }

        /**
         * Inserts a tunnel structure into the database, associating it with a specified miner.
         *
         * @param tunnelStructure the TunnelStructure object to be inserted. Must not be null.
         * @param miner the Miner object to associate with the tunnel structure. Must not be null.
         */
        @Override
        public void insertTunnelStructure(@NotNull TunnelStructure tunnelStructure, @NotNull Miner miner) {
            final String methodName = "insertTunnelStructure";

            try (PreparedStatement ps = connection.prepareStatement(INSERT_STRUCTURE)) {
                ps.setString(1, tunnelStructure.getUuid().toString());
                ps.setString(2, miner.getUuid().toString());
                ps.executeUpdate();
            } catch (SQLException exception) {
                logError(methodName, "Error inserting tunnel structure", exception);
            }
        }

        /**
         * Inserts a miner into the database.
         * This method attempts to store the miner's UUID and suspicion score
         * into the appropriate database table. If the insertion fails, an error
         * is logged for debugging and analysis.
         *
         * @param miner The miner to be inserted, containing information such as
         *              their unique identifier and suspicion score. Must not be null.
         */
        @Override
        public void insertMiner(@NotNull Miner miner) {
            final String methodName = "insertMiner";

            try (PreparedStatement ps = connection.prepareStatement(INSERT_MINER)) {
                ps.setString(1, miner.getUuid().toString());
                ps.setDouble(2, miner.getSuspicionScore());
                ps.executeUpdate();
            } catch (SQLException exception) {
                logError(methodName, "Error inserting miner", exception);
            }
        }

        /**
         * Merges multiple tunnel structures into one unified structure.
         * This operation updates database records, ensuring that the old structures,
         * along with their associated paths and units, are migrated to the new merged structure.
         * After migration, the old structures and related data are safely removed.
         *
         * @param minerUuid The UUID of the miner associated with the new merged structure.
         * @param newMergedStructure The TunnelStructure instance representing the newly created merged structure.
         * @param newTunnelUnit The TunnelUnit to be added to the new merged structure.
         * @param oldStructures The list of old TunnelStructures to be merged into the new structure. Must not be empty.
         */
        @Override
        public void mergeStructures(@NotNull UUID minerUuid,
                                    @NotNull TunnelStructure newMergedStructure,
                                    @NotNull TunnelUnit newTunnelUnit,
                                    @NotNull List<TunnelStructure> oldStructures) {
            final String methodName = "mergeStructures";

            if (oldStructures.isEmpty()) {
                super.plugin.getLogger().warning("mergeStructures called with empty oldStructures list");
                return;
            }

            // Prepare SQL statements with dynamic IN clause
            String inClausePlaceholders = String.join(",", Collections.nCopies(oldStructures.size(), "?"));

            String createStructureSql = "INSERT INTO tunnel_structures (uuid, miner_uuid, created_at) VALUES (?, ?, CURRENT_TIMESTAMP)";
            String createPathSql = "INSERT INTO tunnel_paths (uuid, structure_uuid, created_at) VALUES (?, ?, CURRENT_TIMESTAMP)";

            String updateUnitsSql = String.format("""
            UPDATE tunnel_units
            SET path_uuid = ?
            WHERE path_uuid IN (
                SELECT tp.uuid
                FROM tunnel_paths tp
                JOIN tunnel_structures ts ON tp.structure_uuid = ts.uuid
                WHERE ts.uuid IN (%s)
            )
            """, inClausePlaceholders);

            String insertNewUnitSql = "INSERT INTO tunnel_units (path_uuid, x, z, material, exposed, world_name, mined_at, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
            String deletePathsSql = String.format("DELETE FROM tunnel_paths WHERE structure_uuid IN (%s)", inClausePlaceholders);
            String deleteStructuresSql = String.format("DELETE FROM tunnel_structures WHERE uuid IN (%s)", inClausePlaceholders);

            try {
                connection.setAutoCommit(false);

                // Create a new structure
                try (PreparedStatement stmt = connection.prepareStatement(createStructureSql)) {
                    stmt.setString(1, newMergedStructure.getUuid().toString());
                    stmt.setString(2, minerUuid.toString());
                    stmt.executeUpdate();
                }

                // Create a new path
                try (PreparedStatement stmt = connection.prepareStatement(createPathSql)) {
                    stmt.setString(1, newMergedStructure.getMainTunnelPath().getUuid().toString());
                    stmt.setString(2, newMergedStructure.getUuid().toString());
                    stmt.executeUpdate();
                }

                // Update all tunnel units to point to a new path
                try (PreparedStatement stmt = connection.prepareStatement(updateUnitsSql)) {
                    stmt.setString(1, newMergedStructure.getMainTunnelPath().getUuid().toString());

                    // Set old structure UUIDs for the IN clause
                    for (int i = 0; i < oldStructures.size(); i++) {
                        stmt.setString(i + 2, oldStructures.get(i).getUuid().toString());
                    }

                    stmt.executeUpdate();
                }

                // Insert the new tunnel unit
                try (PreparedStatement stmt = connection.prepareStatement(insertNewUnitSql)) {
                    stmt.setString(1, newMergedStructure.getMainTunnelPath().getUuid().toString());
                    stmt.setInt(2, newTunnelUnit.getX());
                    stmt.setInt(3, newTunnelUnit.getZ());
                    stmt.setString(4, newTunnelUnit.getMaterial().toString());
                    stmt.setBoolean(5, newTunnelUnit.isExposedToAir());
                    stmt.setString(6, newTunnelUnit.getWorldName());
                    stmt.setLong(7, newTunnelUnit.getMinedAt());
                    stmt.executeUpdate();
                }

                // Delete old paths
                try (PreparedStatement stmt = connection.prepareStatement(deletePathsSql)) {
                    for (int i = 0; i < oldStructures.size(); i++) {
                        stmt.setString(i + 1, oldStructures.get(i).getUuid().toString());
                    }
                    stmt.executeUpdate();
                }

                // Delete old structures
                try (PreparedStatement stmt = connection.prepareStatement(deleteStructuresSql)) {
                    for (int i = 0; i < oldStructures.size(); i++) {
                        stmt.setString(i + 1, oldStructures.get(i).getUuid().toString());
                    }
                    stmt.executeUpdate();
                }

                connection.commit();
                super.plugin.getLogger().info("Successfully merged " + oldStructures.size() + " structures");

            } catch (SQLException exception) {
                logError(methodName, "Error during structure merge transaction", exception);

                try {
                    connection.rollback();
                    super.plugin.getLogger().warning("Rolled back mergeStructures transaction due to error");
                } catch (SQLException rollbackException) {
                    logError(methodName, "Error rolling back transaction", rollbackException);
                }
            } finally {
                try {
                    connection.setAutoCommit(true);
                } catch (SQLException exception) {
                    logError(methodName, "Could not re-enable autocommit", exception);
                }
            }
        }

    /**
     * Deletes miner data from the database based on the provided unique identifier.
     * This method attempts to remove the miner entry corresponding to the specified UUID.
     * If the deletion is successful, a positive result is returned.
     * If an error occurs during the operation, it is logged and the method returns false.
     *
     * @param minerUuid The unique identifier of the miner whose data is to be deleted. Must not be null.
     * @return True if the miner data was successfully deleted, false otherwise.
     */
    @Override
    public boolean deleteMinerData(@NotNull UUID minerUuid) {
        final String methodName = "deleteMinerData";

        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM miners WHERE uuid = ?")) {
            ps.setString(1, minerUuid.toString());
            return ps.executeUpdate() > 0;
        } catch (SQLException exception) {
            logError(methodName, "Error deleting miner", exception);
            return false;
        }
    }

    /**
     * Deletes a tunnel structure from the database using the specified UUID.
     * Returns {@code true} if the deletion was successful, {@code false} otherwise.
     *
     * @param stuctureUuid The UUID of the tunnel structure to delete. Must not be null.
     * @return {@code true} if the tunnel structure was successfully deleted, {@code false} if an error occurred or no record was deleted.
     */
    @Override
    public boolean deleteTunnelStructureData(@NotNull UUID stuctureUuid) {
        final String methodName = "deleteTunnelStructureData";

        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM tunnel_structures WHERE uuid = ?")) {
            ps.setString(1, stuctureUuid.toString());
            return ps.executeUpdate() > 0;
        } catch (SQLException exception) {
            logError(methodName, "Error deleting tunnel structure", exception);
            return false;
        }
    }
    
    /**
         * Enhanced error logging method that includes method name and detailed SQL error information
         *
         * @param methodName The name of the method where the error occurred
         * @param errorMessage Descriptive error message
         * @param exception The SQLException that was thrown
         */
        private void logError(String methodName, String errorMessage, SQLException exception) {
            String logMessage = String.format(
                    "DATABASE ERROR in %s.%s: %s%n" +
                            "SQL State: %s%n" +
                            "Error Code: %d%n" +
                            "Message: %s",
                    this.getClass().getSimpleName(),
                    methodName,
                    errorMessage,
                    exception.getSQLState(),
                    exception.getErrorCode(),
                    exception.getMessage()
            );

            super.plugin.getLogger().severe(logMessage);

            // Log additional details for foreign key constraint violations
            if ("23503".equals(exception.getSQLState())) { // Foreign key violation
                super.plugin.getLogger().severe("FOREIGN KEY CONSTRAINT VIOLATION DETECTED");
                super.plugin.getLogger().severe("This typically indicates a missing reference in a parent table");
            }

            // Log the stack trace for deeper debugging if needed
            if (super.plugin.getStandardConfig().isDebugEnabled()) {
                exception.printStackTrace();
            }
        }

}

