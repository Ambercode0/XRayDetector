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

package com.ambercode.config;

import com.ambercode.XRayDetector;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

/**
 * The StandardConfig class provides utility methods to access and manage the
 * configuration settings of the XRayDetector plugin. It interacts with the
 * plugin instance to reload, save, and retrieve configuration data, specifically
 * settings related to logging, debug mode, database connection, and server behavior.
 */
public class StandardConfig {

    private FileConfiguration config = null;

    /**
     * Initializes the StandardConfig instance by reloading and saving
     * the configuration of the provided XRayDetector plugin.
     *
     * @param plugin the XRayDetector plugin instance used to manage the configuration.
     *               This parameter must not be null.
     */
    public StandardConfig(@NotNull XRayDetector plugin) {
        plugin.reloadConfig();
        plugin.saveDefaultConfig();
        this.config = plugin.getConfig();
    }

    /**
     * Checks whether the logger functionality is enabled based on the configuration settings.
     * The method retrieves the value of the "logger" key from the configuration file.
     * If the key is not defined, it defaults to true.
     *
     * @return true if the logger is enabled, false otherwise
     */
    public boolean isLoggerEnabled() {
        return config.getBoolean("logger", true);
    }

    /**
     * Checks if debug logging is enabled in the configuration.
     *
     * @return true if debug mode is enabled, false otherwise.
     */
    public boolean isDebugEnabled() {
        return config.getBoolean("debug");
    }

    /**
     * Retrieves the username configured for the database connection.
     *
     * @return the database username as a string.
     */
    public String getUsername() {
        return config.getString("database.username");
    }

    /**
     * Retrieves the password used for database connection from the configuration.
     *
     * @return the database password as a string.
     */
    public String getPassword() {
        return config.getString("database.password");
    }

    /**
     * Retrieves the configured address for the database connection.
     *
     * @return the database address as a string.
     */
    public String getAddress() {
        return config.getString("database.address");
    }

    /**
     * Retrieves the name of the database from the configuration.
     *
     * @return the database name as a string, or null if not configured.
     */
    public String getDatabaseName() {
        return config.getString("database.db_name");
    }

    /**
     * Retrieves the port number for the database connection as specified in the configuration file.
     *
     * @return the port number as an integer.
     */
    public int getPort() {
        return config.getInt("database.port");
    }

    /**
     * Retrieves the type of database configured in the system.
     * The method returns the value of the "database.type" configuration,
     * defaulting to "SQLITE" if the configuration is not explicitly defined.
     *
     * @return the type of database as a String, or "SQLITE" if no type is configured
     */
    public String getDatabaseType() {
        return config.getString("database.type","SQLITE");
    }

    /**
     * Determines if the server should shut down in the event of a database crash or initialization failure.
     * This configuration value is controlled by the "database.crash-shutdown" flag in the configuration file.
     *
     * @return true if the server is configured to shut down upon a database crash, false otherwise.
     */
    public boolean isCrashShutdown() {
        return config.getBoolean("database.crash-shutdown");
    }
}
