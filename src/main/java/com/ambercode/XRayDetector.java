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
        setupCommands();
        setupListeners();
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
        XRayDetectorCommand xRayDetectorCommand = new XRayDetectorCommand(playerDataManager);
        Objects.requireNonNull(getCommand("xraydetector")).setExecutor(xRayDetectorCommand);
        Objects.requireNonNull(getCommand("xraydetector")).setTabCompleter(xRayDetectorCommand);
    }

    private void setupListeners() {
        getServer().getPluginManager().registerEvents(new TunnelTrackingListener(fileLogger, playerDataManager, this), this);
        getServer().getPluginManager().registerEvents(new GUIEventListener(new SuspicionGUI(playerDataManager)), this);
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
}
