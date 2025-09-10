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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Supported databases
 */
public enum DatabaseType {

    /**
     * Represents the SQLite database type.
     * This enum value defines the database name as "sqlite" and its corresponding JDBC driver class as "org.sqlite.JDBC".
     * Used to identify and interact with SQLite databases in the application.
     */
    SQLITE("sqlite", "org.sqlite.JDBC"),
    /**
     * Enum constant representing MySQL database type.
     * Provides the database name and the corresponding JDBC driver class name.
     */
    MYSQL("mysql", "com.mysql.jdbc.Driver");

    /**
     * The name of the database type, used to uniquely identify the database.
     * It specifies the short identifier or the common name of the database for internal usage.
     */
    private final String name;
    /**
     * Represents the fully qualified name of the Java class used as the JDBC driver
     * for the database type. This is used to load the specific database driver
     * dynamically at runtime when establishing a connection.
     * <p>
     * This variable is immutable and its value is assigned during the
     * initialization of a DatabaseType instance.
     */
    private final String className;

    /**
     * Constructs a DatabaseType with the specified database name and driver class name.
     *
     * @param name      the name of the database type
     * @param className the fully qualified name of the driver class associated with the database
     */
    DatabaseType(@NotNull String name, @NotNull String className) {
        this.name = name;
        this.className = className;
    }

    /**
     * Retrieves the name of the database type.
     *
     * @return the database type name as a string.
     */
    @NotNull
    public String getName() {
        return name;
    }

    /**
     * Retrieves the name of the database driver's class associated with this database type.
     *
     * @return the class name of the database driver as a string.
     */
    @NotNull
    public String getClassName() {
        return className;
    }

    /**
     * Retrieves a DatabaseType enum constant matching the specified name, ignoring case considerations.
     *
     * @param name the name of the database type to match, must not be null
     * @return the matching DatabaseType if found, otherwise null
     */
    @Nullable
    public static DatabaseType fromName(@NotNull String name) {
        for (DatabaseType type : values()) {
            if (type.name.equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }
}