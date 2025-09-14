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

package com.ambercode.gui;

import com.ambercode.XRayDetector;
import com.ambercode.data.Miner;
import com.ambercode.manager.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FlaggedGUI {

    public static final String INV_TITLE = "§cFlagged Players";

    private final XRayDetector xRayDetector;
    private final PlayerDataManager playerDataManager;
    private static final int ITEMS_PER_PAGE = 45;
    private final List<Inventory> pages = new ArrayList<>();

    public FlaggedGUI(@NotNull XRayDetector xRayDetector) {
        this.xRayDetector = xRayDetector;
        this.playerDataManager = xRayDetector.getPlayerDataManager();
        createPages();
    }

    /**
     * Creates a paginated graphical user interface (GUI) displaying a list of flagged miners
     * based on their suspicion scores. The miners are filtered and sorted in descending order
     * of their suspicion scores and split across multiple pages. Each page includes details
     * such as the miner's username, suspicion score, blocks mined, and ore veins discovered.
     * Navigation between pages is provided using "Previous Page" and "Next Page" buttons.
     * <p>
     * The method performs the following steps:
     * 1. Clears the current list of pages.
     * 2. Filters the miners whose suspicion score is above a configurable threshold.
     * 3. Sorts the filtered miners by suspicion score in descending order.
     * 4. Divides the sorted miners into multiple pages, each containing a fixed number of items.
     * 5. Creates inventory GUIs for each page, populating them with player heads representing
     *    the miners and their associated details.
     * 6. Adds navigation buttons for multipage interfaces.
     */
    private void createPages() {
        List<Miner> sortedMiners = playerDataManager.getMiners().stream()
                .filter(miner -> miner.getSuspicionScore() >= xRayDetector.getStandardConfig().getAnalysisSuspectsGuiAddThreshold())
                .sorted(Comparator.comparingDouble(Miner::getSuspicionScore).reversed())
                .toList();

        int pageCount = (int) Math.ceil(sortedMiners.size() / (double) ITEMS_PER_PAGE);
        for (int page = 0; page < pageCount; page++) {
            Inventory inv = Bukkit.createInventory(null, 54, INV_TITLE + " - Page " + (page + 1));

            int startIndex = page * ITEMS_PER_PAGE;
            int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, sortedMiners.size());

            for (int i = startIndex; i < endIndex; i++) {
                Miner miner = sortedMiners.get(i);
                ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) head.getItemMeta();
                if (meta != null) {
                    meta.setOwningPlayer(Bukkit.getOfflinePlayer(miner.getUuid()));
                    meta.setDisplayName("§c" + Bukkit.getOfflinePlayer(miner.getUuid()).getName());
                    List<String> lore = new ArrayList<>();
                    lore.add("§7Suspicion Score: §c" + String.format("%.2f", miner.getSuspicionScore()));
                    lore.add("§7Mined Blocks: §f" + miner.getMinedBlocks());
                    lore.add("§7Ore Veins Found: §f" + miner.getDiscoveredOreVeins());
                    meta.setLore(lore);
                    head.setItemMeta(meta);
                }
                inv.setItem(i - startIndex, head);
            }

            if (page > 0) {
                ItemStack prev = new ItemStack(Material.ARROW);
                ItemMeta prevMeta = prev.getItemMeta();
                if (prevMeta != null) {
                    prevMeta.setDisplayName("§aPrevious Page");
                    prev.setItemMeta(prevMeta);
                }
                inv.setItem(45, prev);
            }

            if (page < pageCount - 1) {
                ItemStack next = new ItemStack(Material.ARROW);
                ItemMeta nextMeta = next.getItemMeta();
                if (nextMeta != null) {
                    nextMeta.setDisplayName("§aNext Page");
                    next.setItemMeta(nextMeta);
                }
                inv.setItem(53, next);
            }

            pages.add(inv);
        }
    }

    /**
     * Opens the graphical user interface (GUI) for the specified player.
     *
     * @param player the player for whom the GUI will be opened; must not be null
     */
    public void openGui(@NotNull Player player) {
        // Always rebuild pages to ensure the data is up to date
        pages.clear();
        createPages();

        if (!pages.isEmpty()) {
            player.openInventory(pages.getFirst());
        } else {
            player.openInventory(Bukkit.createInventory(null, 54, INV_TITLE + " - Empty"));
        }
    }
}
