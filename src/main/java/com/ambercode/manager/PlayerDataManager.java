package com.ambercode.manager;

import com.ambercode.XRayDetector;
import com.ambercode.data.Miner;
import com.ambercode.database.PluginDatabase;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class PlayerDataManager {

    private final XRayDetector plugin;
    private final Set<Miner> miners = new HashSet<>();

    public PlayerDataManager(@NotNull XRayDetector plugin) {
        this.plugin = plugin;
    }

    public void loadCacheFromDatabase() {
        PluginDatabase db = plugin.getPluginDatabase();
        List<Miner> miners = db.getAllData();
        plugin.getLogger().info(String.format("Loaded %d miners from the database.",  miners.size()));
        this.miners.addAll(miners);
    }

    public Set<Miner> getMiners() {
        return miners;
    }
}
