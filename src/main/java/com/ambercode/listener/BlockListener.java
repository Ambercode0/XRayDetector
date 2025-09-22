package com.ambercode.listener;

import com.ambercode.XRayDetector;
import com.ambercode.data.*;
import com.ambercode.data.manager.GlobalManager;
import com.ambercode.data.manager.SuspicionScorer;
import com.ambercode.data.manager.VeinManager;
import com.ambercode.database.PluginDatabase;
import com.ambercode.database.SQLiteDatabase;
import com.ambercode.listener.events.PlayerFlaggedEvent;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Deque;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.ambercode.data.IdGenerator.UNIT_ID_GEN;

public final class BlockListener implements Listener {

    private final XRayDetector xRayDetector;
    private final SuspicionScorer suspicionScorer;
    private final VeinManager veinManager;
    private final PluginDatabase database;
    private final GlobalManager gm;

    public BlockListener(@NotNull XRayDetector xRayDetector) {
        this.xRayDetector = xRayDetector;
        this.database = new SQLiteDatabase(xRayDetector);
        this.suspicionScorer = new SuspicionScorer();
        this.veinManager = new VeinManager();
        this.gm = new GlobalManager();
        // debug();
    }
    @EventHandler
    public void onBlockBreak(@NotNull BlockBreakEvent e) {
        if (e.isCancelled()) return;
        UUID playerUuid = e.getPlayer().getUniqueId();
        Block b = e.getBlock();
        int x = b.getX();
        int y = b.getY();
        int z = b.getZ();

        TunnelUnit unit = new TunnelUnit(x, y, z, b.getType(), b.isBlockPowered(), System.currentTimeMillis());
        Miner miner = gm.getOrInsertMiner(playerUuid);
        miner.addUnit(unit);

        if (Utils.isPreciousOre(unit.getMaterial())) {
            handlePreciousOre(miner, b, unit);
        }
    }

    private void handlePreciousOre(@NotNull Miner miner, @NotNull Block block, @NotNull TunnelUnit unit) {
        Deque<OreVein> minerVeins = miner.getVeinQueue();

        if (minerVeins.isEmpty()) {
            OreVein vein = new OreVein(unit.getMaterial());
            Utils.getAllOreVeinUnits(block, unit.isExposed()).forEach(vein::addTunnelUnit);
            minerVeins.addLast(vein);
        } else {

        }
    }

}
