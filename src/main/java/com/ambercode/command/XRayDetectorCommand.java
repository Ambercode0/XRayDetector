package com.ambercode.command;

import com.ambercode.data.Miner;
import com.ambercode.data.TunnelStructure;
import com.ambercode.data.TunnelUnit;
import com.ambercode.gui.SuspicionGUI;
import com.ambercode.manager.PlayerDataManager;
import com.ambercode.utils.TunnelPathFinder;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class XRayDetectorCommand implements CommandExecutor, TabCompleter {

    private final PlayerDataManager playerDataManager;
    private final SuspicionGUI suspicionGUI;

    public XRayDetectorCommand(PlayerDataManager playerDataManager, SuspicionGUI suspicionGUI) {
        this.playerDataManager = playerDataManager;
        this.suspicionGUI = suspicionGUI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("xraydetector.gui")) {
            player.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            int page = 0;
            String sortType = "suspicion";
            boolean showOnlySuspicious = false;

            // Parse additional arguments
            if (args.length >= 2) {
                try {
                    page = Math.max(0, Integer.parseInt(args[1]) - 1); // Convert to 0-based index
                } catch (NumberFormatException e) {
                    player.sendMessage("§cInvalid page number: " + args[1]);
                    return true;
                }
            }

            if (args.length >= 3) {
                if (args[2].equalsIgnoreCase("name") || args[2].equalsIgnoreCase("suspicion")) {
                    sortType = args[2].toLowerCase();
                } else {
                    player.sendMessage("§cInvalid sort type. Use 'name' or 'suspicion'.");
                    return true;
                }
            }

            if (args.length >= 4) {
                showOnlySuspicious = args[3].equalsIgnoreCase("suspicious");
            }

            suspicionGUI.openGUI(player, page, sortType, showOnlySuspicious);
            return true;
        } else if (args[0].equalsIgnoreCase("view")) {
            // highlightOptimisedPath(player);
            return true;
        }

        sendUsage(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("list");
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("list")) {
            return Arrays.asList("1", "2", "3", "4", "5");
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("list")) {
            return Arrays.asList("suspicion", "name");
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("list")) {
            return Arrays.asList("all", "suspicious");
        }

        return new ArrayList<>();
    }

    private void sendUsage(Player player) {
        player.sendMessage("§7§m─────────────────────────────────────");
        player.sendMessage("§6§lX-Ray Detection System");
        player.sendMessage("§7Usage:");
        player.sendMessage("§e/xraydetector list §7[page] [sort] [filter]");
        player.sendMessage("§7  page: §fPage number (default: 1)");
        player.sendMessage("§7  sort: §fsuspicion §7or §fname §7(default: suspicion)");
        player.sendMessage("§7  filter: §fall §7or §fsuspicious §7(default: all)");
        player.sendMessage("§7§m─────────────────────────────────────");
    }

    private void highlightOptimisedPath(Player player) {
        Miner miner = playerDataManager.getMiners().stream().filter(m -> m.getUuid().equals(player.getUniqueId())).findFirst().orElse(null);

        if (miner == null) {
            player.sendMessage("You have not Mined anything!");
            return;
        }

        for (TunnelStructure createdTunnel : miner.getCreatedTunnels()) {
            List<TunnelUnit> units = TunnelPathFinder.findLongestPath(createdTunnel.getMainTunnelPath().getUnits());
            for (TunnelUnit unit : units) {
                player.spawnParticle(Particle.END_ROD, new Location(player.getWorld(), unit.getX()+.5f, player.getY()+.25f, unit.getZ()+.5f),0);
            }
        }
    }
}
