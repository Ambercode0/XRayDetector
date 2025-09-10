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

        // Table creation SQL statements
        private static final String CREATE_TABLE_MINERS = """
            CREATE TABLE IF NOT EXISTS miners (
                uuid TEXT PRIMARY KEY UNIQUE,
                suspicion_score REAL NOT NULL DEFAULT 0.0,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            );
            """;

        private static final String CREATE_TABLE_PATHS = """
            CREATE TABLE IF NOT EXISTS tunnel_paths (
                uuid TEXT PRIMARY KEY UNIQUE,
                structure_uuid TEXT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (structure_uuid) REFERENCES tunnel_structures(uuid) ON DELETE CASCADE
            );
            """;

        private static final String CREATE_TABLE_STRUCTURES = """
            CREATE TABLE IF NOT EXISTS tunnel_structures (
                uuid TEXT PRIMARY KEY UNIQUE,
                miner_uuid TEXT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (miner_uuid) REFERENCES miners(uuid) ON DELETE CASCADE
            );
            """;

        private static final String CREATE_TABLE_UNITS = """
            CREATE TABLE IF NOT EXISTS tunnel_units (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                path_uuid TEXT NOT NULL,
                x INTEGER NOT NULL,
                z INTEGER NOT NULL,
                material TEXT NOT NULL,
                exposed BOOLEAN NOT NULL,
                mined_at BIGINT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (path_uuid) REFERENCES tunnel_paths(uuid) ON DELETE CASCADE,
                UNIQUE(path_uuid, x, z)
            );
            """;

        private static final String INDEXES = """
            CREATE INDEX IF NOT EXISTS idx_miners_suspicion ON miners(suspicion_score);
            CREATE INDEX IF NOT EXISTS idx_tunnel_paths_miner ON tunnel_paths(miner_uuid);
            CREATE INDEX IF NOT EXISTS idx_tunnel_units_path ON tunnel_units(path_uuid);
            CREATE INDEX IF NOT EXISTS idx_tunnel_units_position ON tunnel_units(x, z);
            CREATE INDEX IF NOT EXISTS idx_tunnel_units_material ON tunnel_units(material);
            CREATE INDEX IF NOT EXISTS idx_tunnel_units_exposed ON tunnel_units(exposed);
            CREATE INDEX IF NOT EXISTS idx_tunnel_units_mined_at ON tunnel_units(mined_at);
            """;

        // Query constants
        private static final String CACHE_ALL_DATA_QUERY = """
            SELECT
                m.uuid as miner_uuid,
                m.suspicion_score,
                m.created_at as miner_created_at,
                m.updated_at as miner_updated_at,
               
                ts.uuid as structure_uuid,
                ts.created_at as structure_created_at,
               
                tp.uuid as path_uuid,
                tp.created_at as path_created_at,
               
                tu.id as unit_id,
                tu.x as unit_x,
                tu.z as unit_z,
                tu.material as unit_material,
                tu.exposed as unit_exposed,
                tu.mined_at as unit_mined_at,
                tu.created_at as unit_created_at
               
            FROM miners m
            LEFT JOIN tunnel_structures ts ON m.uuid = ts.miner_uuid
            LEFT JOIN tunnel_paths tp ON ts.uuid = tp.structure_uuid
            LEFT JOIN tunnel_units tu ON tp.uuid = tu.path_uuid
            ORDER BY m.uuid, ts.uuid, tp.uuid, tu.mined_at
            """;

        private static final String INSERT_UNIT = """
            INSERT INTO tunnel_units (path_uuid, x, z, material, exposed, mined_at, created_at)
            VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP);
            """;

        private static final String INSERT_PATH = """
            INSERT INTO tunnel_paths (uuid, structure_uuid, created_at)
            VALUES (?, ?, CURRENT_TIMESTAMP);
            """;

        private static final String INSERT_STRUCTURE = """
            INSERT INTO tunnel_structures (uuid, miner_uuid, created_at)
            VALUES (?, ?, CURRENT_TIMESTAMP);
            """;

        private static final String INSERT_MINER = """
            INSERT INTO miners (uuid, suspicion_score, created_at, updated_at)
            VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
            """;

        public SQLiteDatabase(@NotNull XRayDetector plugin) {
            super(plugin, DatabaseType.SQLITE);
        }

        @Override
        protected @NotNull String createConnectionUrl() {
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
                                long minedAt = rs.getLong("unit_mined_at");

                                Material material = Material.getMaterial(materialStr);
                                if (material != null) {
                                    TunnelUnit unit = new TunnelUnit(x, z, material, minedAt);
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

        @Override
        public void insertTunnelUnit(@NotNull TunnelUnit tunnelUnit, @NotNull TunnelPath tunnelPath) {
            final String methodName = "insertTunnelUnit";

            try (PreparedStatement ps = connection.prepareStatement(INSERT_UNIT)) {
                ps.setString(1, tunnelPath.getUuid().toString());
                ps.setInt(2, tunnelUnit.getX());
                ps.setInt(3, tunnelUnit.getZ());
                ps.setString(4, tunnelUnit.getMaterial().toString());
                ps.setBoolean(5, tunnelUnit.isExposedToAir());
                ps.setLong(6, tunnelUnit.getMinedAt());
                ps.executeUpdate();
            } catch (SQLException exception) {
                logError(methodName, "Error inserting tunnel unit", exception);
            }
        }

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

        @Override
        public void mergeStructures(UUID minerUuid,
                                    TunnelStructure newMergedStructure,
                                    TunnelUnit newTunnelUnit,
                                    List<TunnelStructure> oldStructures) {
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

            String insertNewUnitSql = "INSERT INTO tunnel_units (path_uuid, x, z, material, exposed, mined_at, created_at) VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
            String deletePathsSql = String.format("DELETE FROM tunnel_paths WHERE structure_uuid IN (%s)", inClausePlaceholders);
            String deleteStructuresSql = String.format("DELETE FROM tunnel_structures WHERE uuid IN (%s)", inClausePlaceholders);

            try {
                connection.setAutoCommit(false);

                // Step 1: Create new structure
                try (PreparedStatement stmt = connection.prepareStatement(createStructureSql)) {
                    stmt.setString(1, newMergedStructure.getUuid().toString());
                    stmt.setString(2, minerUuid.toString());
                    stmt.executeUpdate();
                }

                // Step 2: Create new path
                try (PreparedStatement stmt = connection.prepareStatement(createPathSql)) {
                    stmt.setString(1, newMergedStructure.getMainTunnelPath().getUuid().toString());
                    stmt.setString(2, newMergedStructure.getUuid().toString());
                    stmt.executeUpdate();
                }

                // Step 3: Update all tunnel units to point to new path
                try (PreparedStatement stmt = connection.prepareStatement(updateUnitsSql)) {
                    stmt.setString(1, newMergedStructure.getMainTunnelPath().getUuid().toString());

                    // Set old structure UUIDs for the IN clause
                    for (int i = 0; i < oldStructures.size(); i++) {
                        stmt.setString(i + 2, oldStructures.get(i).getUuid().toString());
                    }

                    stmt.executeUpdate();
                }

                // Step 4: Insert the new tunnel unit
                try (PreparedStatement stmt = connection.prepareStatement(insertNewUnitSql)) {
                    stmt.setString(1, newMergedStructure.getMainTunnelPath().getUuid().toString());
                    stmt.setInt(2, newTunnelUnit.getX());
                    stmt.setInt(3, newTunnelUnit.getZ());
                    stmt.setString(4, newTunnelUnit.getMaterial().toString());
                    stmt.setBoolean(5, newTunnelUnit.isExposedToAir());
                    stmt.setLong(6, newTunnelUnit.getMinedAt());
                    stmt.executeUpdate();
                }

                // Step 5: Delete old paths
                try (PreparedStatement stmt = connection.prepareStatement(deletePathsSql)) {
                    for (int i = 0; i < oldStructures.size(); i++) {
                        stmt.setString(i + 1, oldStructures.get(i).getUuid().toString());
                    }
                    stmt.executeUpdate();
                }

                // Step 6: Delete old structures
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

