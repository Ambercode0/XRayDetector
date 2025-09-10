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
import com.ambercode.config.StandardConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Represents an abstract base class for database integrations that require user credentials.
 * This class provides a foundation for managing database connections, including storing
 * credentials, generating connection URLs, and handling connection lifecycles.
 * Subclasses are expected to implement specific database behavior (e.g., SQLite, MySQL).
 */
public abstract class CredentialPluginDatabase extends AbstractPluginDatabase {

    /* ------------------------------ */

    protected final String username;
    protected final String password;
    protected final String address;
    protected final String databaseName;
    protected final int port;

    /* ------------------------------ */

    protected Connection connection = null;

    protected CredentialPluginDatabase(final @NotNull XRayDetector plugin,
                                       final @NotNull DatabaseType type) {
        super(plugin, type);
        StandardConfig conf = plugin.getStandardConfig();
        this.username = conf.getUsername();
        this.password = conf.getPassword();
        this.address = conf.getAddress();
        this.databaseName = conf.getDatabaseName();
        this.port = conf.getPort();
    }


    /**
     * Creates a connection URL for the database based on the configured properties.
     * The URL includes the database type, address, port, and database name required for connection.
     *
     * @return the formatted connection URL as a non-null string
     */
    @NotNull
    protected String createConnectionUrl() {
        return String.format("jdbc:%s://%s:%d/%s",
                this.databaseType.getName(),
                this.address,
                this.port,
                this.databaseName);
    }

    @Override
    public final @Nullable Connection getConnection() {
        return this.connection;
    }

    @Override
    public void connect() {
        if (this.connection != null) {
            return;
        }

        try {
            this.connection = DriverManager.getConnection(this.createConnectionUrl());
        } catch (SQLException exception) {
            super.plugin.getLogger().warning("WARNING! Could not connect to the database.");
            super.plugin.getLogger().warning("Shutting the server down!");
            super.plugin.getServer().shutdown();
        }
    }

    /**
     * Retrieves the configured username for the database connection.
     *
     * @return the database username as a non-null string.
     */
    @NotNull
    public String getUsername() {
        return username;
    }

    /**
     * Retrieves the password used for the database connection.
     *
     * @return the database password as a non-null string
     */
    @NotNull
    public String getPassword() {
        return password;
    }

    /**
     * Retrieves the address associated with the database connection.
     *
     * @return the database address as a non-null string.
     */
    @NotNull
    public String getAddress() {
        return address;
    }

    /**
     *
     */
    @NotNull
    public String getDatabaseName() {
        return databaseName;
    }

    /**
     * Retrieves the port number used for database connections.
     *
     * @return the port number as an integer.
     */
    public int getPort() {
        return port;
    }
}
