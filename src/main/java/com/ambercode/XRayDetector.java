package com.ambercode;

import com.ambercode.command.XRayDetectorCommand;
import com.ambercode.gui.SuspicionGUI;
import com.ambercode.listeners.BlockBreakEventListener;
import com.ambercode.listeners.GUIEventListener;
import com.ambercode.manager.PlayerDataManager;
import org.bukkit.plugin.java.JavaPlugin;

public class XRayDetector extends JavaPlugin {

    private final PlayerDataManager playerDataManager = new PlayerDataManager();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new BlockBreakEventListener(this.playerDataManager), this);
        getServer().getPluginManager().registerEvents(new GUIEventListener(new SuspicionGUI(this.playerDataManager)), this);
        XRayDetectorCommand xRayDetectorCommand = new XRayDetectorCommand(this.playerDataManager);
        getCommand("xraydetector").setExecutor(xRayDetectorCommand);
        getCommand("xraydetector").setTabCompleter(xRayDetectorCommand);
    }
}
