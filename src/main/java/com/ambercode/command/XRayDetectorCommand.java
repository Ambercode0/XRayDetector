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

package com.ambercode.command;

import com.ambercode.data.Miner;
import com.ambercode.data.TunnelStructure;
import com.ambercode.data.TunnelUnit;
import com.ambercode.gui.FlaggedGUI;
import com.ambercode.gui.SuspicionGUI;
import com.ambercode.manager.PlayerDataManager;
import com.ambercode.utils.TunnelPathFinder;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The XRayDetectorCommand class serves as the main command handler for the
 * X-Ray Detection System plugin. It enables players with appropriate permissions
 * to view and manage X-ray detection data via subcommands. The class also provides
 * tab completion support for command arguments.
 * <p>
 * This command allows interactive functionality such as viewing a paginated list
 * of players and their suspicion levels, toggling filters, and customizing the
 * display based on sorting options. Additionally, it delivers user feedback for
 * invalid inputs while respecting the permission system.
 */
public class XRayDetectorCommand implements CommandExecutor, TabCompleter {

    private final PlayerDataManager playerDataManager;
    private final SuspicionGUI suspicionGUI;
    private final FlaggedGUI flaggedGUI;

    public XRayDetectorCommand(PlayerDataManager playerDataManager, SuspicionGUI suspicionGUI, FlaggedGUI flaggedGUI) {
        this.playerDataManager = playerDataManager;
        this.suspicionGUI = suspicionGUI;
        this.flaggedGUI = flaggedGUI;
    }

    /**
     * Handles the execution of the X-Ray Detection System command, allowing players to interact with
     * the system through various subcommands like listing or viewing data. This command supports permissions,
     * input validation, and displays usage feedback for invalid inputs.
     *
     * @param sender  the source of the command, which can be either a player or the console
     * @param command the command object associated with this execution
     * @param label   the alias or command name used by the sender
     * @param args    the arguments passed to the command by the sender
     * @return true to indicate that the command was successfully handled, regardless of input validity
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

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
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_PEARL_THROW, 1.0f, 1.0f);
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
        } else if (args[0].equalsIgnoreCase("suspects")) {
            flaggedGUI.openGui(player);
            return true;
        }

        sendUsage(player);
        return true;
    }

    /**
     * Handles tab completion for the given command. Based on the input arguments,
     * this method provides context-specific suggestions to assist the user in
     * completing their command input.
     *
     * @param sender  the source of the command could be a player or the console
     * @param command the command being executed
     * @param alias   the alias used for the command
     * @param args    the arguments already entered by the user
     * @return a list of possible completions based on the current input context,
     *         or an empty list if no suggestions are applicable
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("list", "suspects");
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

    /**
     * Sends a formatted usage message to the specified player, detailing the
     * correct usage instructions for the X-Ray Detection System command.
     *
     * @param player the player to whom the usage instructions will be sent
     */
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

    /**
     * Highlights the optimised tunnel paths created by the specified player by showing particles
     * at the locations of the path units of their tunnels. If the player has no associated miner data,
     * they are informed with a message.
     *
     * @param player the player whose optimised tunnel paths should be highlighted, must not be null
     */
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
