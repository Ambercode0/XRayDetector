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

public abstract class AbstractPluginDatabase implements PluginDatabase {

    protected final XRayDetector plugin;
    protected final DatabaseType databaseType;

    protected AbstractPluginDatabase(final @NotNull XRayDetector plugin,
                                     final @NotNull DatabaseType databaseType) {
        this.plugin = plugin;
        this.databaseType = databaseType;
    }

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

    public DatabaseType getDatabaseType() {
        return databaseType;
    }
}
