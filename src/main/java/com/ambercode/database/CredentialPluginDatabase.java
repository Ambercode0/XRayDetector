package com.ambercode.database;

import com.ambercode.XRayDetector;
import com.ambercode.config.StandardConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

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

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getAddress() {
        return address;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public int getPort() {
        return port;
    }
}
