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
import org.jetbrains.annotations.NotNull;

/**
 * Abstract base class for database implementations in the plugin. This class provides
 * foundational functionality and structure for managing database connections, types,
 * and related operations.
 * <p>
 * Subclasses are expected to provide concrete implementations for certain database
 * operations while utilizing the shared functionality in this class. The class ensures
 * that database-specific configurations like driver class loading are handled and logged
 * appropriately.
 */
public abstract class AbstractPluginDatabase implements PluginDatabase {

    protected final XRayDetector plugin;
    protected final DatabaseType databaseType;

    protected AbstractPluginDatabase(final @NotNull XRayDetector plugin,
                                     final @NotNull DatabaseType databaseType) {
        this.plugin = plugin;
        this.databaseType = databaseType;
    }

    /**
     * Loads the JDBC driver class corresponding to the database type associated with this instance.
     * <p>
     * This method attempts to load the driver class using the class name retrieved from the
     * {@code databaseType} object. If the driver class cannot be found, an appropriate log
     * warning is written, and the server is gracefully shut down to prevent further operations.
     * <p>
     * The method relies on the `Class.forName` mechanism to dynamically load the driver class.
     * A failure to locate the class results in a {@link ClassNotFoundException}.
     * <p>
     * Logging and server management are handled using the {@code plugin} instance, ensuring
     * that all warnings and shutdown actions are properly recorded and executed.
     *
     * @throws RuntimeException if the driver class could not be loaded or the server fails to shut down
     */
    @Override
    public void loadDriverClass() {
        try {
            final Class<?> driverClass = Class.forName(this.databaseType.getClassName());
        } catch (ClassNotFoundException exception) {
            this.plugin.getLogger().warning("WARNING! Could not load database driver class \"" + this.databaseType.getClassName() + "\"");
            this.plugin.getLogger().warning("Shutting down the server. . .");
            this.plugin.getServer().shutdown();
        }
    }

    /**
     * Retrieves the database type associated with this instance.
     *
     * @return the database type, represented as an instance of {@link DatabaseType}.
     */
    public DatabaseType getDatabaseType() {
        return databaseType;
    }
}
