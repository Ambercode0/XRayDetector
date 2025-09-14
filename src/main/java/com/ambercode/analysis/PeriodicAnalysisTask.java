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

package com.ambercode.analysis;

import com.ambercode.XRayDetector;
import com.ambercode.config.StandardConfig;
import com.ambercode.data.Miner;
import com.ambercode.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.UUID;

final class PeriodicAnalysisTask implements Runnable {

    private final XRayDetector xRayDetector;
    private final StandardConfig conf;

    PeriodicAnalysisTask(@NotNull XRayDetector xRayDetector) {
        this.xRayDetector = xRayDetector;
        this.conf = xRayDetector.getStandardConfig();
    }

    @Override
    public void run() {
        // running every X minutes (see config.yml)
        for (final Miner miner : xRayDetector.getPlayerDataManager().getMiners()) {
            final double sus = Utils.calculateOverallMinerSuspicionScore(miner, xRayDetector);
            miner.setSuspicionScore(sus);
            if (conf.isAnalysisAutoPunishEnabled() && conf.getAnalysisAutoPunishThreshold() <= sus)
                applyPunishment(miner);
            if (conf.getAnalysisSuspectsGuiChatWarnAdministrators() && conf.getAnalysisSuspectsGuiAddThreshold() <= sus)
                warnAdminsOfSuspect(miner);
        }
    }

    private void warnAdminsOfSuspect(@NotNull Miner miner) {
        Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.hasPermission("xraydetector.notifications"))
                .forEach(p -> {

                    UUID uuid = miner.getUuid();
                    OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);

                    if (!op.hasPlayedBefore()) {
                        xRayDetector.getLogger().warning("ERROR: Player " + op.getName() + "(" + uuid + ") has not played before, skipping warning.");
                        return;
                    }

                    String username = op.getName();
                    assert username != null;

                    p.sendMessage("§8§l[§c§lX-RAY DETECTOR§8§l]");
                    p.sendMessage("§7Suspect: §c" + username);
                    p.sendMessage("§7Suspicion Score: §c" + String.format("%.2f", miner.getSuspicionScore()) + "§8/§c1.00");
                    p.sendMessage("§7Mined Blocks: §f" + miner.getMinedBlocks());
                    p.sendMessage("§7Ore Veins Found: §f" + miner.getDiscoveredOreVeins());
                    p.playSound(p.getLocation(), Sound.BLOCK_BELL_RESONATE, 1.0f, .85f);
                });
    }

    /**
     * Applies a predefined punishment to a miner based on their suspicion score and configuration settings.
     * This method handles determining punishment eligibility and dispatches the appropriate punishment command.
     * If no punishment can be applied due to missing configurations or conditions, the method logs warnings.
     *
     * @param miner the {@link Miner} instance representing the player to whom the punishment may be applied;
     *              must not be null.
     */
    private void applyPunishment(@NotNull Miner miner) {
        UUID uuid = miner.getUuid();
        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);

        if (!op.hasPlayedBefore()) {
            xRayDetector.getLogger().warning("ERROR: Player " + op.getName() + "(" + uuid + ") has not played before, skipping punishment.");
            return;
        }

        String username = op.getName();
        assert username != null;

        String reason = conf.getAnalysisAutoPunishReason();
        if (reason == null) {
            xRayDetector.getLogger().warning("WARNING: No reason configured for automatic punishment, skipping punishment.");
            return;
        }

        String logMsg = "Applying automatic punishment to " + username + " (" + uuid + ") for suspicion score " + miner.getSuspicionScore() + " (" + reason + ")";
        xRayDetector.getLogger().info(logMsg);
        xRayDetector.getFileLogger().addLogMessage(logMsg);

        String command = conf.getAnalysisAutoPunishCommand();
        if (command == null) {
            xRayDetector.getLogger().warning("WARNING: No command configured for automatic punishment, skipping punishment.");
            return;
        }

        xRayDetector.getServer().dispatchCommand(xRayDetector.getServer().getConsoleSender(), conf.getAnalysisAutoPunishCommand()
                .replace("%player%", username)
                .replace("%reason%", reason));

        xRayDetector.getPlayerDataManager().removeMiner(uuid);
    }
}
