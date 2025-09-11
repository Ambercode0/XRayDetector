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

package com.ambercode.listeners;

import com.ambercode.gui.SuspicionGUI;
import com.ambercode.logging.FileLogger;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.logging.Logger;

public class GUIEventListener implements Listener {

    private final SuspicionGUI suspicionGUI;
    private final FileLogger fileLogger;
    private final Logger log;

    public GUIEventListener(@NotNull SuspicionGUI suspicionGUI, @NotNull FileLogger fileLogger) {
        this.suspicionGUI = suspicionGUI;
        this.fileLogger = fileLogger;
        this.log = suspicionGUI.getPlugin().getLogger();
    }

    /**
     * Determines if the title corresponds to the custom GUI identified by a specific base title.
     * Logs the check result for debugging purposes.
     *
     * @param title The title of the GUI to check. Can be null.
     * @return true if the title is not null and starts with the base title of the custom GUI; false otherwise.
     */
    private boolean isOurGUI(String title) {
        boolean result = title != null && title.startsWith(SuspicionGUI.GUI_BASE_TITLE);
        fileLogger.addLogMessage("[GUI] isOurGUI check: title='" + title + "' base='" + SuspicionGUI.GUI_BASE_TITLE + "' result=" + result);
        return result;
    }

    /**
     * Handles inventory click events for custom GUI interactions. This method ensures the click event
     * is processed only if it occurs within a custom GUI managed by the plugin. It also performs
     * various actions based on the clicked slot, such as navigation, sorting, deletion, and parsing
     * of clicked items.
     *
     * @param event the InventoryClickEvent triggered when a player interacts with an inventory
     */
    @EventHandler
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        final String title = event.getView().getTitle(); // final; why not.
        fileLogger.addLogMessage("[GUI] Click event: player=" + player.getName() + " title='" + title + "'");

        if (!isOurGUI(title)) {
            fileLogger.addLogMessage("[GUI] Not our GUI, ignoring");
            return;
        }

        event.setCancelled(true);
        fileLogger.addLogMessage("[GUI] Event cancelled, looking up session for UUID: " + player.getUniqueId());

        // Ensure the click is in the top (GUI) inventory, not the player's own inventory
        int raw = event.getRawSlot();
        int topSize = event.getView().getTopInventory().getSize();
        if (raw < 0 || raw >= topSize) {
            fileLogger.addLogMessage("[GUI] Click in player inventory, ignoring. RawSlot=" + raw + " TopSize=" + topSize);
            return;
        }

        // DEBUG: Let's see what sessions exist
        fileLogger.addLogMessage("[GUI] Active sessions: " + suspicionGUI.getActiveSessionsDebug());

        SuspicionGUI.GUISession session = suspicionGUI.getSession(player.getUniqueId());
        if (session == null) {
            fileLogger.addLogMessage("[GUI] Null session for " + player.getName() + " (UUID: " + player.getUniqueId() + ") in GUI titled: " + title);
            fileLogger.addLogMessage("[GUI] Available sessions: " + suspicionGUI.getActiveSessionsDebug());
            return;
        }

        ItemStack clickedItem = event.getCurrentItem();
        int slot = raw;

        fileLogger.addLogMessage("[GUI] Player=" + player.getName() +
                " Mode=" + session.mode +
                " Page=" + session.currentPage +
                " Slot=" + slot +
                " Item=" + (clickedItem == null ? "null" : clickedItem.getType().name()));

        // Handle delete button
        if (slot == 50) {
            if (session.mode == SuspicionGUI.GUIMode.STRUCTURES) {
                boolean databaseResult = suspicionGUI.getPlugin().getPluginDatabase().deleteMinerData(session.minerUUID);
                suspicionGUI.openGUI(player, 0, session.sortType, session.showOnlySuspicious);
                player.closeInventory(InventoryCloseEvent.Reason.PLUGIN);
                boolean deleteMinerResult = suspicionGUI.getPlayerDataManager().removeMiner(session.minerUUID);
                if (!databaseResult || !deleteMinerResult) {
                    log.warning("Failed to delete tunnel structure data for UUID " + session.structureUUID);
                } else {
                    log.info("Successfully deleted tunnel structure data for UUID " + session.structureUUID);
                    player.sendMessage("§aSuccessfully deleted miner data");
                }
                return;
            } else if (session.mode == SuspicionGUI.GUIMode.UNITS) {
                boolean databaseResult = suspicionGUI.getPlugin().getPluginDatabase().deleteTunnelStructureData(UUID.fromString(session.structureUUID));
                suspicionGUI.openGUI(player, 0, session.sortType, session.showOnlySuspicious);
                player.closeInventory(InventoryCloseEvent.Reason.PLUGIN);
                boolean deleteStructureResult = suspicionGUI.getPlayerDataManager().removeStructure(UUID.fromString(session.structureUUID));
                if (!databaseResult || !deleteStructureResult) {
                    log.warning("Failed to delete tunnel structure data for UUID " + session.structureUUID);
                } else {
                    log.info("Successfully deleted tunnel structure data for UUID " + session.structureUUID);
                    player.sendMessage("§aSuccessfully deleted tunnel structure data");
                }
                return;
            }
        }

        // Navigation buttons
        if (slot == 45 && session.currentPage > 0) {
            fileLogger.addLogMessage("[GUI] Prev page clicked.");
            openSameGUI(player, session, session.currentPage - 1);
            return;
        }
        if (slot == 53 && session.currentPage < session.totalPages - 1) {
            fileLogger.addLogMessage("[GUI] Next page clicked.");
            openSameGUI(player, session, session.currentPage + 1);
            return;
        }
        if (slot == 51) {
            fileLogger.addLogMessage("[GUI] Refresh clicked.");
            openSameGUI(player, session, session.currentPage);
            return;
        }

        // PLAYERS-only controls
        if (session.mode == SuspicionGUI.GUIMode.PLAYERS) {
            if (slot == 47) {
                fileLogger.addLogMessage("[GUI] Sort by suspicion clicked.");
                suspicionGUI.openGUI(player, 0, "suspicion", session.showOnlySuspicious);
                return;
            }
            if (slot == 48) {
                fileLogger.addLogMessage("[GUI] Sort by name clicked.");
                suspicionGUI.openGUI(player, 0, "name", session.showOnlySuspicious);
                return;
            }
            if (slot == 52) {
                fileLogger.addLogMessage("[GUI] Toggle filter clicked.");
                suspicionGUI.openGUI(player, 0, session.sortType, !session.showOnlySuspicious);
                return;
            }
        }

        // Content area
        if (slot < 45 && clickedItem != null && clickedItem.hasItemMeta()) {
            fileLogger.addLogMessage("[GUI] Content area clicked, parsing item...");
            SuspicionGUI.ClickInfo info = suspicionGUI.parseClickedItem(clickedItem);

            if (info != null) {
                fileLogger.addLogMessage("[GUI] Parsed ClickInfo: type=" + info.type + " id=" + info.id);
                switch (info.type) {
                    case "miner" -> {
                        try {
                            UUID minerUUID = UUID.fromString(info.id);
                            fileLogger.addLogMessage("[GUI] Opening structures for miner=" + minerUUID);
                            // Schedule for the next tick to avoid race condition
                            Bukkit.getScheduler().runTask(suspicionGUI.getPlugin(), () -> {
                                suspicionGUI.openStructuresGUI(player, minerUUID, 0);
                            });
                        } catch (Exception e) {
                            fileLogger.addLogMessage("[GUI] Failed to parse miner UUID: " + info.id + " Error: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                    case "structure" -> {
                        fileLogger.addLogMessage("[GUI] Opening units for structure=" + info.id);
                        // Schedule for the next tick to avoid race condition
                        Bukkit.getScheduler().runTask(suspicionGUI.getPlugin(), () -> {
                            suspicionGUI.openUnitsGUI(player, info.id, 0);
                        });
                    }
                    case "unit" -> {
                        fileLogger.addLogMessage("[GUI] Unit clicked (no drill-down). ID=" + info.id);
                        player.sendMessage("§7Tunnel unit clicked. No further drill-down.");
                    }
                    default -> fileLogger.addLogMessage("[GUI] Unknown ClickInfo type=" + info.type);
                }
                return;
            } else {
                fileLogger.addLogMessage("[GUI] Could not parse ClickInfo from item: " + clickedItem.getType() + " - " +
                        (clickedItem.hasItemMeta() ? clickedItem.getItemMeta().getDisplayName() : "no meta"));
            }

            // Fallback for heads without PDC
            if (clickedItem.getType() == Material.PLAYER_HEAD) {
                fileLogger.addLogMessage("[GUI] Trying fallback skull method...");
                if (clickedItem.getItemMeta() instanceof SkullMeta skullMeta) {
                    if (skullMeta.getOwningPlayer() != null) {
                        UUID minerUUID = skullMeta.getOwningPlayer().getUniqueId();
                        fileLogger.addLogMessage("[GUI] Fallback skull click. Opening structures for miner=" + minerUUID);
                        suspicionGUI.openStructuresGUI(player, minerUUID, 0);
                    } else {
                        fileLogger.addLogMessage("[GUI] Fallback skull had no owning player.");
                    }
                }
            }
        } else {
            fileLogger.addLogMessage("[GUI] Click outside content area or no item meta. Slot=" + slot);
        }
    }

    /**
     * Handles the inventory drag event to prevent items from being dragged in a custom GUI.
     * Cancels the drag event if the inventory being interacted with matches the custom GUI title.
     *
     * @param event The InventoryDragEvent triggered by a player dragging items in an inventory.
     */
    @EventHandler
    public void onInventoryDrag(@NotNull InventoryDragEvent event) {
        String title = event.getView().getTitle();
        if (isOurGUI(title)) {
            event.setCancelled(true);
            fileLogger.addLogMessage("[GUI] Prevented inventory drag in GUI: " + title);
        }
    }

    /**
     * Handles the InventoryCloseEvent when a player closes an inventory.
     * Logs the event details and manages the session associated with a specific GUI if it's recognized.
     *
     * @param event The InventoryCloseEvent triggered when a player closes an inventory.
     */
    @EventHandler
    public void onInventoryClose(@NotNull InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        String title = event.getView().getTitle();
        fileLogger.addLogMessage("[GUI] Inventory close event: player=" + player.getName() + " title='" + title + "'");

        if (isOurGUI(title) && suspicionGUI.getSession(player.getUniqueId()).inventory == event.getInventory()) {
            fileLogger.addLogMessage("[GUI] Closing session for " + player.getName() + " UUID=" + player.getUniqueId());
            suspicionGUI.closeSession(player.getUniqueId());
            fileLogger.addLogMessage("[GUI] Session closed for " + player.getName() + " GUI=" + title);
        }
    }

    /**
     * Re-opens the same GUI for a player based on the provided session and page.
     *
     * @param player the player for whom the GUI should be re-opened
     * @param session the current GUI session containing mode, sorting, and display settings
     * @param newPage the new page number to display in the GUI
     */
    private void openSameGUI(@NotNull Player player, @NotNull SuspicionGUI.GUISession session, int newPage) {
        fileLogger.addLogMessage("[GUI] Re-opening same GUI. Mode=" + session.mode + " NewPage=" + newPage);
        switch (session.mode) {
            case PLAYERS -> suspicionGUI.openGUI(player, newPage, session.sortType, session.showOnlySuspicious);
            case STRUCTURES -> suspicionGUI.openStructuresGUI(player, session.minerUUID, newPage);
            case UNITS -> suspicionGUI.openUnitsGUI(player, session.structureUUID, newPage);
        }
    }
}