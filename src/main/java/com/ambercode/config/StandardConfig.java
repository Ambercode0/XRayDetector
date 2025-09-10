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

public class StandardConfig {

    private FileConfiguration config = null;

    public StandardConfig(@NotNull XRayDetector plugin) {
        plugin.reloadConfig();
        plugin.saveDefaultConfig();
        this.config = plugin.getConfig();
    }

    public boolean isLoggerEnabled() {
        return config.getBoolean("logger", true);
    }

    public boolean isDebugEnabled() {
        return config.getBoolean("debug");
    }

    public String getUsername() {
        return config.getString("database.username");
    }

    public String getPassword() {
        return config.getString("database.password");
    }

    public String getAddress() {
        return config.getString("database.address");
    }

    public String getDatabaseName() {
        return config.getString("database.db_name");
    }

    public int getPort() {
        return config.getInt("database.port");
    }

    public String getDatabaseType() {
        return config.getString("database.type","SQLITE");
    }

    public boolean isCrashShutdown() {
        return config.getBoolean("database.crash-shutdown");
    }
}
