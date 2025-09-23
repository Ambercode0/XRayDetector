package com.ambercode.listener;

import com.ambercode.XRayDetector;
import com.ambercode.data.*;
import com.ambercode.data.manager.GlobalManager;
import com.ambercode.data.manager.SuspicionScorer;
import com.ambercode.data.manager.VeinManager;
import com.ambercode.database.PluginDatabase;
import com.ambercode.database.SQLiteDatabase;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Deque;
import java.util.Set;
import java.util.UUID;

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
        Player p = e.getPlayer();
        Block b = e.getBlock();
        int x = b.getX();
        int y = b.getY();
        int z = b.getZ();

        Miner miner = gm.getOrInsertMiner(playerUuid);
        TunnelUnit unit = new TunnelUnit(x, y, z, b.getType(), Utils.isExposed(b,miner), System.currentTimeMillis());
        miner.addUnit(unit);

        if (Utils.isPreciousOre(unit.getMaterial())) {
            handlePreciousOre(miner, b, unit);
        }

        double score = SuspicionScorer.computeTimeIntervalScore(miner);
        p.sendActionBar(Component.text(String.format("Your Interval Score : %.3f", score)));
    }

    private static void handlePreciousOre(@NotNull Miner miner, @NotNull Block block, @NotNull TunnelUnit unit) {
        Deque<OreVein> minerVeins = miner.getVeinDeque();
        Set<TunnelUnit> foundAdjacent = Utils.getAllOreVeinUnits(block, miner);

        if (minerVeins.isEmpty()) { // no previous veins found
            OreVein vein = new OreVein(unit.getMaterial());
            foundAdjacent.forEach(vein::addTunnelUnit);
            minerVeins.addLast(vein);
        } else { // veins found
            boolean found = false;
            for (OreVein vein : minerVeins) { // checking if a vein contains unit, true ignore, false create vein
                if (vein.getTunnelUnits().contains(unit)) {
                    found = true;
                    break;
                }
            }

            if (!found) { // no vein contains unit, create new vein
                OreVein newVein = new OreVein(unit.getMaterial());
                foundAdjacent.forEach(newVein::addTunnelUnit);
                minerVeins.addLast(newVein);
            }
        }
    }

}
