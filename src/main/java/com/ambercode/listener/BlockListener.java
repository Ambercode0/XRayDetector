package com.ambercode.listener;

import com.ambercode.XRayDetector;
import com.ambercode.data.Miner;
import com.ambercode.data.PackedBlockPos;
import com.ambercode.data.TunnelUnit;
import com.ambercode.data.Registry;
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

import java.util.UUID;

import static com.ambercode.data.IdGenerator.UNIT_ID_GEN;

public final class BlockListener implements Listener {
    
    private final XRayDetector xRayDetector;
    private final Registry registry;
    private final SuspicionScorer suspicionScorer;
    private final VeinManager veinManager;
    private final PluginDatabase database;

    public BlockListener(@NotNull XRayDetector xRayDetector) {
        this.xRayDetector = xRayDetector;
        this.registry = new Registry();
        this.database = new SQLiteDatabase(xRayDetector);
        this.suspicionScorer = new SuspicionScorer(registry);
        this.veinManager = new VeinManager(registry);
    }

    @EventHandler
    public void onBlockBreak(@NotNull BlockBreakEvent e) {
        if (e.isCancelled()) return;
        UUID playerUuid = e.getPlayer().getUniqueId();
        Block b = e.getBlock();
        int matId = mapMaterial(b.getType());
        int x = b.getX();
        int y = b.getY();
        int z = b.getZ();
        PackedBlockPos pos = PackedBlockPos.of(x, y, z);
        boolean isExposed = quickExposureCheck(b);
        int uId = UNIT_ID_GEN.next();
        TunnelUnit tu = new TunnelUnit(uId, pos, matId, System.currentTimeMillis(), playerUuid, isExposed);
        registry.putUnit(uId, tu);
        Miner m = registry.getOrCreateMiner(playerUuid);
        m.recordUnit(uId, matId, isOreMaterial(matId), isExposed, tu.minedAt);

        if (isOreMaterial(matId)) {
            double suspicionScore = suspicionScorer.score(m);
            if (suspicionScore > xRayDetector.getStandardConfig().getAnalysisSuspectsGuiAddThreshold()) {
                xRayDetector.getServer().getPluginManager().callEvent(new PlayerFlaggedEvent(e.getPlayer(), suspicionScore));
            }
        }
    }

    private static int mapMaterial(@NotNull Material material) {
        return material.ordinal();
    }

    private static boolean isOreMaterial(int materialId) {
        Material material = Material.values()[materialId];
        return material.name().endsWith("DIAMOND_ORE");
    }

    private static boolean quickExposureCheck(@NotNull Block block) {
        final BlockFace[] faces = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN};
        for (BlockFace face : faces) {
            if (block.getRelative(face).getType() == Material.AIR) {
                return true;
            }
        }
        return false;
    }

}
