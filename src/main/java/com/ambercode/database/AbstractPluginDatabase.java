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
