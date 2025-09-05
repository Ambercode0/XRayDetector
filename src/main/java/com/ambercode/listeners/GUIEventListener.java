package com.ambercode.listeners;

import com.ambercode.gui.SuspicionGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

public class GUIEventListener implements Listener {

    private final SuspicionGUI suspicionGUI;

    public GUIEventListener(SuspicionGUI suspicionGUI) {
        this.suspicionGUI = suspicionGUI;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();

        if (!event.getView().getTitle().startsWith("§6§lX-Ray Detection")) return;

        event.setCancelled(true);

        SuspicionGUI.GUISession session = suspicionGUI.getSession(player.getUniqueId());
        if (session == null) return;

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null) return;

        int slot = event.getSlot();

        // Handle navigation clicks
        if (slot == 45 && session.currentPage > 0) {
            // Previous page
            suspicionGUI.openGUI(player, session.currentPage - 1, session.sortType, session.showOnlySuspicious);
        } else if (slot == 53 && session.currentPage < session.totalPages - 1) {
            // Next page
            suspicionGUI.openGUI(player, session.currentPage + 1, session.sortType, session.showOnlySuspicious);
        } else if (slot == 47) {
            // Sort by suspicion
            suspicionGUI.openGUI(player, 0, "suspicion", session.showOnlySuspicious);
        } else if (slot == 48) {
            // Sort by name
            suspicionGUI.openGUI(player, 0, "name", session.showOnlySuspicious);
        } else if (slot == 51) {
            // Refresh
            suspicionGUI.openGUI(player, session.currentPage, session.sortType, session.showOnlySuspicious);
        } else if (slot == 52) {
            // Toggle filter
            suspicionGUI.openGUI(player, 0, session.sortType, !session.showOnlySuspicious);
        } else if (slot < 45) {
            // Player head clicked - could open detailed view
            if (clickedItem.getType().name().contains("PLAYER_HEAD")) {
                player.sendMessage("§7Detailed analysis feature coming soon!");
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        if (event.getView().getTitle().startsWith("§6§lX-Ray Detection")) {
            suspicionGUI.closeSession(player.getUniqueId());
        }
    }
}