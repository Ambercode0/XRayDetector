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
