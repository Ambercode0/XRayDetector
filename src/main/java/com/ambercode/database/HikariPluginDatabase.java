package com.ambercode.database;

import com.ambercode.XRayDetector;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

public abstract class HikariPluginDatabase extends CredentialPluginDatabase {

    protected final HikariConfig hikariConfig;
    protected final HikariDataSource hikariDataSource;

    protected HikariPluginDatabase(@NotNull XRayDetector plugin, @NotNull DatabaseType databaseType) {
        super(plugin, databaseType);
        this.hikariConfig = new HikariConfig();
        this.hikariConfig.setJdbcUrl(super.createConnectionUrl());
        this.hikariConfig.setUsername(super.username);
        this.hikariConfig.setPassword(super.password);
        this.hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        this.hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        this.hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        this.hikariDataSource = new HikariDataSource(this.hikariConfig);
    }

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