package com.ambercode;

import com.ambercode.command.XRayDetectorCommand;
import com.ambercode.gui.SuspicionGUI;
import com.ambercode.listeners.BlockBreakEventListener;
import com.ambercode.listeners.GUIEventListener;
import com.ambercode.logging.FileLogger;
import com.ambercode.manager.PlayerDataManager;
import org.bukkit.plugin.java.JavaPlugin;

public class XRayDetector extends JavaPlugin {

    private final PlayerDataManager playerDataManager = new PlayerDataManager();
    private FileLogger fileLogger;

    @Override
    public void onLoad() {
        fileLogger = new FileLogger(this);
    }

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new BlockBreakEventListener(fileLogger, playerDataManager, this), this);
        getServer().getPluginManager().registerEvents(new GUIEventListener(new SuspicionGUI(playerDataManager)), this);
        XRayDetectorCommand xRayDetectorCommand = new XRayDetectorCommand(playerDataManager);
        getCommand("xraydetector").setExecutor(xRayDetectorCommand);
        getCommand("xraydetector").setTabCompleter(xRayDetectorCommand);
    }

    public FileLogger getFileLogger() {
        return fileLogger;
    }
}
