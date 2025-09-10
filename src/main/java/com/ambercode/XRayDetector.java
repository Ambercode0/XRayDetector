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

    /**
     * Manages player data and interactions with the {@link PlayerDataManager} instance.
     * <p>
     * This field is responsible for handling and coordinating actions related to
     * players, including managing cached player data, fetching specific player-related
     * data entities (e.g., miners and their associated tunnel structures), and
     * interfacing with the database via the {@link PluginDatabase}.
     * <p>
     * The instance of {@link PlayerDataManager} is initialized and configured
     * during the plugin's setup phase. It uses the {@link XRayDetector} plugin
     * instance to access plugin-level configurations, logging, and database operations.
     * <p>
     * The {@link PlayerDataManager} provides mechanisms to:
     * - Load player data from the database into an in-memory cache.
     * - Retrieve specific information like miners or structures using unique player identifiers (UUIDs).
     * - Access the entire set of managed miners.
     */
    private PlayerDataManager playerDataManager = null;
    /**
     * Represents an instance of {@link FileLogger} associated with the plugin.
     * The fileLogger is utilized for handling logging operations such as buffering log messages,
     * writing logs to files, and managing log-related configurations.
     * <p>
     * This field is initialized during the plugin's setup phase and configured based on the
     * plugin's {@link StandardConfig} settings. It is designed to ensure thread-safe operations
     * for logging purposes.
     */
    private FileLogger fileLogger = null;
    /**
     *
     */
    private StandardConfig standardConfig = null;
    /**
     * Represents the database interface used by the plugin for handling data storage
     * and persistence operations. This variable is primarily responsible for managing
     * database connections, executing queries, creating tables, and storing plugin-related
     * data. It implements methods for interacting with entities such as miners, tunnels,
     * and structures in the context of the plugin.
     * <p>
     * The {@code pluginDatabase} field is initialized during the plugin's startup phase
     * (typically in the {@code setupDatabase()} method) and remains accessible throughout
     * the plugin's lifecycle. It acts as the primary mechanism for communicating with
     * the database, managing data transactions, and ensuring persistent storage of
     * information critical to the plugin's functionality.
     * <p>
     * This field is nullable and initially set to {@code null}. Before invoking any
     * database operations, it is necessary to ensure that the database has been properly
     * initialized and connected to avoid runtime exceptions.
     */
    private PluginDatabase pluginDatabase = null;
    /**
     *
     */
    private SuspicionGUI suspicionGUI = null;


    /**
     * Called when the plugin is loaded into the server.
     * This method is invoked by the Bukkit framework before the plugin is fully enabled.
     * <p>
     * The typical use case for this method includes initializing resources or
     * performing non-server-interactive preloading tasks. Currently, it is
     * intended for setting up databases in future implementation.
     * <p>
     * Note: This method is executed once during the plugin loading phase and
     * does not interact with the server's runtime environment.
     */
    @Override
    public void onLoad() {
        // TODO: setup databases later
    }

    /**
     *
     */
    @Override
    public void onEnable() {
        setupConfig();
        setupFileLogger();
        setupDatabase();
        setupPlayerDataManager();
        setupListeners();
        setupCommands();
    }

    /**
     * Initializes and configures the database for the plugin based on the specified type in the configuration.
     * This method determines the database type, ensures the required driver is loaded,
     * establishes the connection, and creates the necessary tables for the plugin's functionality.
     * <p>
     * If the database type is not specified or recognized, the server shuts down with a warning.
     * <p>
     * Functionality includes:
     * - Retrieving the database type from the plugin's configuration.
     * - Initializing the appropriate database implementation (SQLite or MySQL).
     * - Loading the required database driver class.
     * - Establishing a database connection.
     * - Creating the necessary tables in the database.
     * - Logging the success or failure of database initialization.
     */
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

    /**
     * Initializes the PlayerDataManager instance and loads cached player data from the database.
     * <p>
     * This method creates a new instance of {@link PlayerDataManager} using the
     * current plugin instance and then invokes {@code loadCacheFromDatabase()}
     * to retrieve and cache player data from the database.
     * <p>
     * The {@code PlayerDataManager} is responsible for managing player-related
     * data, including loading and accessing miner information cached from the database.
     * <p>
     * Related actions:
     * - Creates a {@link PlayerDataManager} instance.
     * - Retrieves all miner data from the database to populate the cache.
     * <p>
     * Dependencies:
     * - Requires {@link PluginDatabase} to fetch miner data from the database.
     * - Utilizes the plugin's logging functionality for feedback and status updates.
     */
    private void setupPlayerDataManager() {
        playerDataManager = new PlayerDataManager(this);
        playerDataManager.loadCacheFromDatabase();
    }

    /**
     * Initializes the standard configuration for the plugin.
     * <p>
     * This method sets up the {@link StandardConfig} instance using the current plugin context.
     * The configuration includes various settings such as database details, logger preferences, and crash handling.
     * The {@code StandardConfig} is responsible for managing access to these settings throughout the plugin's lifecycle.
     * <p>
     * Functionality:
     * - Creates an instance of {@link StandardConfig} and assigns it to the {@code standardConfig} field.
     * - Ensures that the default configuration is saved and loaded for further operations.
     * <p>
     * Dependencies:
     * - Requires the plugin to provide access to its configuration management methods such as {@code saveDefaultConfig()} and {@code getConfig()}.
     */
    private void setupConfig() {
        this.standardConfig = new StandardConfig(this);
    }

    /**
     * Sets up the file logger for the plugin.
     * <p>
     * This method initializes the {@link FileLogger} instance and assigns it
     * to the class's {@code fileLogger} field. The {@code FileLogger} is
     * responsible for handling logging operations, such as writing log
     * entries to a file for persistent record-keeping.
     * <p>
     * Dependencies:
     * - Utilizes the current plugin instance to create and configure the
     *   {@code FileLogger}.
     * <p>
     * Side effects:
     * - The {@code fileLogger} field of the class is initialized with a new
     *   {@code FileLogger} instance.
     */
    private void setupFileLogger() {
        this.fileLogger = new FileLogger(this);
    }

    /**
     * Sets up and configures the commands used by the plugin.
     * <p>
     * This method initializes the necessary command executor and tab completer for the "xraydetector" command.
     * The "xraydetector" command is used to interact with the plugin's functionality
     * related to detecting and managing X-ray activity.
     * <p>
     * Key actions performed:
     * - Instantiates an {@link XRayDetectorCommand} object by passing the {@code playerDataManager}
     *   and {@code suspicionGUI} fields to its constructor.
     * - Links the "xraydetector" command to the created {@link XRayDetectorCommand} instance
     *   as its command executor.
     * - Assigns the same {@link XRayDetectorCommand} instance as the tab completer for the command.
     * <p>
     * Dependencies:
     * - Requires the {@code playerDataManager} and {@code suspicionGUI} fields to initialize
     *   the {@link XRayDetectorCommand}.
     * - Relies on the Bukkit framework's {@code getCommand(String)} method to retrieve
     *   the "xraydetector" command configuration.
     * <p>
     * Side effects:
     * - Registers the command executor and tab completer for the "xraydetector" command,
     *   enabling the functionality to be interactively accessed via command input.
     * <p>
     * This method should be invoked during the plugin's setup phase, typically within the
     * {@code onEnable} method, to ensure the command is operational when the plugin is enabled.
     */
    private void setupCommands() {
        XRayDetectorCommand xRayDetectorCommand = new XRayDetectorCommand(playerDataManager, suspicionGUI);
        Objects.requireNonNull(getCommand("xraydetector")).setExecutor(xRayDetectorCommand);
        Objects.requireNonNull(getCommand("xraydetector")).setTabCompleter(xRayDetectorCommand);
    }

    /**
     * Sets up the event listeners and graphical user interface components required by the plugin.
     * <p>
     * This method initializes and registers the necessary event listeners for plugin functionality.
     * It also creates an instance of the {@code SuspicionGUI} class, which is used for managing
     * and displaying a GUI related to managing player suspicions, and links it to an event listener.
     * <p>
     * The following key actions are performed:
     * - Registers a {@link TunnelTrackingListener} to monitor and log tunneling activity, utilizing
     *   the {@code fileLogger} for logging and the {@code playerDataManager} for player data handling.
     * - Instantiates a {@link SuspicionGUI} for managing suspicion-related interactions and assigns
     *   it to the class's {@code suspicionGUI} field.
     * - Registers a {@link GUIEventListener} to handle events related to the {@code SuspicionGUI},
     *   linking it to the {@code fileLogger} for logging where needed.
     * <p>
     * Dependencies:
     * - Requires {@code fileLogger} for managing log operations.
     * - Requires {@code playerDataManager} for interacting with cached player data.
     * - Relies on the plugin instance to register event listeners with the server.
     * <p>
     * Side effects:
     * - The {@code suspicionGUI} field of the class is initialized with a new {@link SuspicionGUI} instance.
     * - Event listeners are registered with the server, enabling plugin functionality.
     * <p>
     * This method is critical for initializing the event-driven aspects of the plugin and
     * should be called during the plugin's setup phase.
     */
    private void setupListeners() {
        getServer().getPluginManager().registerEvents(new TunnelTrackingListener(fileLogger, playerDataManager, this), this);
        suspicionGUI = new SuspicionGUI(playerDataManager, this);
        getServer().getPluginManager().registerEvents(new GUIEventListener(suspicionGUI, fileLogger), this);
    }

    /**
     * Handles the cleanup process when the plugin is disabled. This method is
     * called automatically when the plugin is stopped or reloaded.
     * <p>
     * Key functionalities:
     * - Finalizes the logging process by writing any buffered log messages to a file,
     *   and logs the location of the generated log file if successful.
     * - Closes the database connection safely if it has been initialized, to avoid
     *   potential resource leaks.
     * <p>
     * It ensures that critical resources like file logs and database connections
     * are properly closed when the plugin is stopped, maintaining stability and
     * preventing unnecessary issues.
     */
    @Override
    public void onDisable() {
        String path = fileLogger.writeLogsAndGetPath();
        getLogger().info("Logs successfully written to: " + path);

        if (pluginDatabase != null) {
            pluginDatabase.close();
        }

    }

    /**
     * Retrieves the instance of the standard configuration used by the plugin.
     *
     * @return the instance of {@link StandardConfig} associated with the plugin,
     *         providing access to configuration settings.
     */
    public StandardConfig getStandardConfig() {
        return standardConfig;
    }

    /**
     * Retrieves the instance of the plugin's database used for handling data storage and persistence.
     *
     * @return the {@link PluginDatabase} instance associated with the plugin,
     *         providing methods for database operations such as connection management,
     *         data insertion, and table creation.
     */
    public PluginDatabase getPluginDatabase() {
        return pluginDatabase;
    }

    /**
     * Retrieves the instance of {@link FileLogger} used by the plugin for logging purposes.
     *
     * @return the {@link FileLogger} instance, providing methods for managing log files and messages.
     */
    public FileLogger getFileLogger() {
        return fileLogger;
    }

    /**
     * Retrieves the instance of {@link SuspicionGUI} associated with the plugin.
     *
     * @return the {@link SuspicionGUI} instance, providing methods and interface for managing and displaying suspicion-related GUI components.
     */
    public SuspicionGUI getSuspicionGUI() {
        return suspicionGUI;
    }
}
