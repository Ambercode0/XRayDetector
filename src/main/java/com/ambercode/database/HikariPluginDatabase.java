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
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

public abstract class HikariPluginDatabase extends CredentialPluginDatabase {

    protected final HikariConfig hikariConfig;
    protected final HikariDataSource hikariDataSource;

    /**
     * Constructs a new instance of HikariPluginDatabase using the specified plugin and database type.
     * Initializes the HikariCP connection pool configuration and establishes default properties
     * for optimized database connection handling.
     *
     * @param plugin the instance of {@link XRayDetector} that provides plugin integration and functionality. Must not be null.
     * @param databaseType the type of database to connect to, represented by {@link DatabaseType}. Must not be null.
     */
    protected HikariPluginDatabase(@NotNull XRayDetector plugin, @NotNull DatabaseType databaseType) {
        super(plugin, databaseType);
        this.hikariConfig = new HikariConfig();
        this.hikariConfig.setJdbcUrl(super.createConnectionUrl());
        this.hikariConfig.setUsername(super.username);
        this.hikariConfig.setPassword(super.password); // password is safely stored in config.yml
        this.hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        this.hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        this.hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        this.hikariDataSource = new HikariDataSource(this.hikariConfig);
    }

    /**
     * Establishes a connection to the database using the HikariCP connection pool.
     * If a connection is already active, the method exits without performing any action.
     * <p>
     * In case of failure to acquire a connection, a warning is logged,
     * and the server is forcefully shut down to prevent further operation without a valid connection.
     * <p>
     * This method relies on the `hikariDataSource` to provide a connection
     * and handles exceptions by logging errors and invoking server shutdown.
     */
    @Override
    public void connect() {

        if (this.connection != null) {
            return;
        }

        try {
            this.connection = this.hikariDataSource.getConnection();
        } catch (SQLException exception) {
            super.plugin.getLogger().warning("WARNING! Could not connect to the database using HikariCP.");
            super.plugin.getLogger().warning("Shutting the server down!");
            super.plugin.getServer().shutdown();

        }
    }
}