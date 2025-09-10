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

package com.ambercode;

import com.ambercode.command.XRayDetectorCommand;
import com.ambercode.config.StandardConfig;
import com.ambercode.database.DatabaseType;
import com.ambercode.database.MySQLDatabase;
import com.ambercode.database.PluginDatabase;
import com.ambercode.database.SQLiteDatabase;
import com.ambercode.gui.SuspicionGUI;
import com.ambercode.listeners.GUIEventListener;
import com.ambercode.listeners.TunnelTrackingListener;
import com.ambercode.logging.FileLogger;
import com.ambercode.manager.PlayerDataManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class XRayDetector extends JavaPlugin {

    private PlayerDataManager playerDataManager = null;
    private FileLogger fileLogger = null;
    private StandardConfig standardConfig = null;
    private PluginDatabase pluginDatabase = null;
    private SuspicionGUI suspicionGUI = null;


    @Override
    public void onLoad() {
        // TODO: setup databases later
    }

    @Override
    public void onEnable() {
        setupConfig();
        setupFileLogger();
        setupDatabase();
        setupPlayerDataManager();
        setupListeners();
        setupCommands();
    }

    private void setupDatabase() {
        DatabaseType databaseType = DatabaseType.fromName(Objects.requireNonNull(getConfig().getString("database.type")));
        if (databaseType == null) {
            getLogger().warning("Database type not found!");
            getServer().shutdown();
            return;
        }

        switch (databaseType) {
            case SQLITE: {pluginDatabase = new SQLiteDatabase(this); break;}
            case MYSQL: {pluginDatabase = new MySQLDatabase(this); break;}
            default: {pluginDatabase = null; break;}
        }

        assert pluginDatabase != null;
        pluginDatabase.loadDriverClass();
        pluginDatabase.connect();
        pluginDatabase.createTables();

        getLogger().info("Database loaded successfully, type: " + databaseType);
    }

    private void setupPlayerDataManager() {
        playerDataManager = new PlayerDataManager(this);
        playerDataManager.loadCacheFromDatabase();
    }

    private void setupConfig() {
        this.standardConfig = new StandardConfig(this);
    }

    private void setupFileLogger() {
        this.fileLogger = new FileLogger(this);
    }

    private void setupCommands() {
        XRayDetectorCommand xRayDetectorCommand = new XRayDetectorCommand(playerDataManager, suspicionGUI);
        Objects.requireNonNull(getCommand("xraydetector")).setExecutor(xRayDetectorCommand);
        Objects.requireNonNull(getCommand("xraydetector")).setTabCompleter(xRayDetectorCommand);
    }

    private void setupListeners() {
        getServer().getPluginManager().registerEvents(new TunnelTrackingListener(fileLogger, playerDataManager, this), this);
        suspicionGUI = new SuspicionGUI(playerDataManager, this);
        getServer().getPluginManager().registerEvents(new GUIEventListener(suspicionGUI, fileLogger), this);
    }

    @Override
    public void onDisable() {
        String path = fileLogger.writeLogsAndGetPath();
        getLogger().info("Logs successfully written to: " + path);

        if (pluginDatabase != null) {
            pluginDatabase.close();
        }

    }

    public StandardConfig getStandardConfig() {
        return standardConfig;
    }

    public PluginDatabase getPluginDatabase() {
        return pluginDatabase;
    }

    public FileLogger getFileLogger() {
        return fileLogger;
    }

    public SuspicionGUI getSuspicionGUI() {
        return suspicionGUI;
    }
}
