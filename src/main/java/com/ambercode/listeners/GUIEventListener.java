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

import java.util.UUID;
import java.util.logging.Logger;

public class GUIEventListener implements Listener {

    private final SuspicionGUI suspicionGUI;
    private final FileLogger fileLogger;
    private final Logger log;

    public GUIEventListener(SuspicionGUI suspicionGUI, FileLogger fileLogger) {
        this.suspicionGUI = suspicionGUI;
        this.fileLogger = fileLogger;
        this.log = suspicionGUI.getPlugin().getLogger();
    }

    private boolean isOurGUI(String title) {
        boolean result = title != null && title.startsWith(SuspicionGUI.GUI_BASE_TITLE);
        fileLogger.addLogMessage("[GUI] isOurGUI check: title='" + title + "' base='" + SuspicionGUI.GUI_BASE_TITLE + "' result=" + result);
        return result;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = event.getView().getTitle();
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

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        String title = event.getView().getTitle();
        if (isOurGUI(title)) {
            event.setCancelled(true);
            fileLogger.addLogMessage("[GUI] Prevented inventory drag in GUI: " + title);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        String title = event.getView().getTitle();
        fileLogger.addLogMessage("[GUI] Inventory close event: player=" + player.getName() + " title='" + title + "'");

        if (isOurGUI(title) && suspicionGUI.getSession(player.getUniqueId()).inventory == event.getInventory()) {
            fileLogger.addLogMessage("[GUI] Closing session for " + player.getName() + " UUID=" + player.getUniqueId());
            suspicionGUI.closeSession(player.getUniqueId());
            fileLogger.addLogMessage("[GUI] Session closed for " + player.getName() + " GUI=" + title);
        }
    }

    private void openSameGUI(Player player, SuspicionGUI.GUISession session, int newPage) {
        fileLogger.addLogMessage("[GUI] Re-opening same GUI. Mode=" + session.mode + " NewPage=" + newPage);
        switch (session.mode) {
            case PLAYERS -> suspicionGUI.openGUI(player, newPage, session.sortType, session.showOnlySuspicious);
            case STRUCTURES -> suspicionGUI.openStructuresGUI(player, session.minerUUID, newPage);
            case UNITS -> suspicionGUI.openUnitsGUI(player, session.structureUUID, newPage);
        }
    }
}