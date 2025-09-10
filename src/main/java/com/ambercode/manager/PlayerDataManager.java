package com.ambercode.manager;

import com.ambercode.XRayDetector;
import com.ambercode.data.Miner;
import com.ambercode.data.TunnelStructure;
import com.ambercode.database.PluginDatabase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    @Nullable
    public Miner getMiner(UUID uuid) {
        return miners.stream().filter(miner -> miner.getUuid().equals(uuid)).findFirst().orElse(null);
    }

    @Nullable
    public TunnelStructure getTunnelStructure(UUID uuid) {
        for (Miner miner : miners) {
            TunnelStructure tunnelStructure = miner.getTunnelStructure(uuid);
            if (tunnelStructure != null) {
                return tunnelStructure;
            }
        }
        return null;
    }

    public Set<Miner> getMiners() {
        return miners;
    }
}
