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
import com.ambercode.data.TunnelStructure;
import com.ambercode.data.TunnelUnit;
import com.ambercode.logging.FileLogger;
import com.ambercode.manager.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * The {@code SuspicionGUI} class is responsible for managing and displaying multiple graphical
 * user interfaces (GUIs) related to player activity and suspicious behavior detection in the
 * game environment. It includes tools for navigating, sorting, and filtering data about players,
 * their activities, and other related structures.
 * <p>
 * This class integrates with the {@link PlayerDataManager} to access and update player metrics,
 * and it uses the provided plugin instance for scheduling tasks and interacting with the broader
 * game server environment. The GUIs provide paginated navigation and are dynamically updated
 * based on player activities and server events.
 * <p>
 * Primary features:
 * - Player Data GUI: Displays player statistics, suspicion scores, and activity summaries.
 * - Structures GUI: Shows tunnel structures created by miners, with details such as length, ores mined, and timestamps.
 * - Units GUI: Represents individual structural or functional units related to player activities.
 * - Flexible navigation and control mechanisms, including sorting and filtering options.
 * - Automatic caching and periodic updates for efficient performance and synchronized data.
 */
public class SuspicionGUI {

    // Exposed base title so listeners can detect GUI inventories
    public static final String GUI_BASE_TITLE = "§6§lX-Ray";

    private static final int ITEMS_PER_PAGE = 45; // 9x5 grid (top 5 rows)
    public static final String GUI_TITLE_PLAYERS = GUI_BASE_TITLE + " Detection - Page ";
    public static final String GUI_TITLE_STRUCTURES = GUI_BASE_TITLE + " Structures - Page ";
    public static final String GUI_TITLE_UNITS = GUI_BASE_TITLE + " Units - Page ";

    private final PlayerDataManager playerDataManager;
    private final Map<UUID, GUISession> activeSessions = new ConcurrentHashMap<>();
    private final Map<UUID, CachedPlayerData> playerCache = new ConcurrentHashMap<>();
    private final Map<UUID, ItemStack> headCache = new ConcurrentHashMap<>();
    private BukkitTask updateTask;
    private final FileLogger fileLogger;
    private final Plugin plugin;

    // NamespacedKey for PDC storage (initialized in setPlugin)
    private final NamespacedKey keyType;
    private final NamespacedKey keyId;

    public SuspicionGUI(PlayerDataManager playerDataManager, XRayDetector plugin) {
        this.playerDataManager = playerDataManager;
        this.plugin = plugin;
        this.fileLogger = plugin.getFileLogger();
        this.keyType = new NamespacedKey(plugin, "suspiciongui_type");
        this.keyId = new NamespacedKey(plugin, "suspiciongui_id");
        startUpdateTask();
    }

    public Plugin getPlugin() {
        return plugin;
    }

    // GUI modes
    public enum GUIMode {
        PLAYERS,
        STRUCTURES,
        UNITS
    }

    public String getActiveSessionsDebug() {
        if (activeSessions.isEmpty()) {
            return "No active sessions";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Active sessions (").append(activeSessions.size()).append("): ");
        for (Map.Entry<UUID, GUISession> entry : activeSessions.entrySet()) {
            UUID uuid = entry.getKey();
            GUISession session = entry.getValue();
            String playerName = Bukkit.getOfflinePlayer(uuid).getName();
            sb.append("[").append(playerName).append("(").append(uuid).append(")=").append(session.mode).append("] ");
        }
        return sb.toString();
    }

    /**
     * Opens a graphical user interface (GUI) for the specified player to display a list of player data.
     * The GUI allows navigation across pages, sorting, and filtering based on the provided parameters.
     *
     * @param viewer                The player for whom the GUI is being opened. This player will view the inventory.
     * @param page                  The page number of the GUI that should be displayed. Page numbers start from 0.
     * @param sortType              The type of sorting to be applied to the player data. This could influence
     *                              how the list of players is ordered in the GUI.
     * @param showOnlySuspicious    A flag indicating whether only suspicious players should be shown in the GUI.
     *                              If true, only suspicious data will be displayed; otherwise, all data will be included.
     */
    public void openGUI(@NotNull Player viewer, int page, @NotNull String sortType, boolean showOnlySuspicious) {
        fileLogger.addLogMessage("[GUI] Opening GUI for " + viewer.getName() + " UUID=" + viewer.getUniqueId());

        List<CachedPlayerData> playerData = getFilteredAndSortedPlayerData(sortType, showOnlySuspicious);

        int totalPages = Math.max(1, (int) Math.ceil((double) playerData.size() / ITEMS_PER_PAGE));
        page = Math.max(0, Math.min(page, totalPages - 1));

        Inventory inventory = Bukkit.createInventory(null, 54, GUI_TITLE_PLAYERS + (page + 1) + "/" + totalPages);

        // Store session data
        GUISession session = new GUISession();
        session.mode = GUIMode.PLAYERS;
        session.currentPage = page;
        session.sortType = sortType;
        session.showOnlySuspicious = showOnlySuspicious;
        session.inventory = inventory;
        session.totalPages = totalPages;
        session.lastRefresh = System.currentTimeMillis();

        fileLogger.addLogMessage("[GUI] Storing session for UUID=" + viewer.getUniqueId());
        activeSessions.put(viewer.getUniqueId(), session);
        fileLogger.addLogMessage("[GUI] Session stored. Active sessions now: " + 0);

        populateInventory(inventory, playerData, page);
        addNavigationItems(inventory, page, totalPages, playerData.size(), session.mode);

        fileLogger.addLogMessage("[GUI] Opening inventory for " + viewer.getName());
        viewer.openInventory(inventory);
    }

    /**
     * Opens the Structures GUI for a player displaying the miner's created tunnel structures.
     * This method handles the initialization, pagination, and GUI session management.
     *
     * @param viewer    The player who is viewing the GUI.
     * @param minerUUID The unique identifier of the miner whose structures are being displayed.
     * @param page      The page number to display in the GUI.
     */
    public void openStructuresGUI(Player viewer, UUID minerUUID, int page) {
        if (plugin == null) {
            viewer.sendMessage("§cGUI system not initialized yet. Please try again in a moment.");
            return;
        }

        fileLogger.addLogMessage("[GUI] Opening structures GUI for " + viewer.getName() + " UUID=" + viewer.getUniqueId() + " minerUUID=" + minerUUID);

        Miner miner = playerDataManager.getMiner(minerUUID);
        if (miner == null) {
            viewer.sendMessage("§cError: Miner data not found!");
            return;
        }

        List<TunnelStructure> structures = miner.getCreatedTunnels();
        if (structures == null) {
            structures = new ArrayList<>();
        }

        int totalPages = Math.max(1, (int) Math.ceil((double) structures.size() / ITEMS_PER_PAGE));
        page = Math.max(0, Math.min(page, totalPages - 1));

        Inventory inventory = Bukkit.createInventory(null, 54, GUI_TITLE_STRUCTURES + (page + 1) + "/" + totalPages);

        // Store session data - AFTER creating inventory but BEFORE opening
        GUISession session = new GUISession();
        session.mode = GUIMode.STRUCTURES;
        session.minerUUID = minerUUID;
        session.currentPage = page;
        session.inventory = inventory;
        session.totalPages = totalPages;
        session.lastRefresh = System.currentTimeMillis();

        fileLogger.addLogMessage("[GUI] Storing STRUCTURES session for UUID=" + viewer.getUniqueId());
        activeSessions.put(viewer.getUniqueId(), session);
        fileLogger.addLogMessage("[GUI] Session stored. Active sessions now: " + getActiveSessionsDebug());

        populateStructures(inventory, structures, page);
        addNavigationItems(inventory, page, totalPages, structures.size(), session.mode);

        fileLogger.addLogMessage("[GUI] Opening structures inventory for " + viewer.getName());
        viewer.openInventory(inventory);
    }

    public void openUnitsGUI(Player viewer, String structureUUID, int page) {
        if (plugin == null) {
            viewer.sendMessage("§cGUI system not initialized yet. Please try again in a moment.");
            return;
        }

        fileLogger.addLogMessage("[GUI] Opening units GUI for " + viewer.getName() + " UUID=" + viewer.getUniqueId() + " structureUUID=" + structureUUID);

        TunnelStructure structure;
        try {
            structure = playerDataManager.getTunnelStructure(UUID.fromString(structureUUID));
        } catch (Exception e) {
            viewer.sendMessage("§cError: Invalid structure UUID!");
            fileLogger.addLogMessage("[GUI] Failed to parse structure UUID: " + structureUUID + " Error: " + e.getMessage());
            return;
        }

        if (structure == null) {
            viewer.sendMessage("§cError: Structure data not found!");
            return;
        }

        List<TunnelUnit> units = structure.getMainTunnelPath().getUnits();
        if (units == null) {
            units = new ArrayList<>();
        }

        int totalPages = Math.max(1, (int) Math.ceil((double) units.size() / ITEMS_PER_PAGE));
        page = Math.max(0, Math.min(page, totalPages - 1));

        Inventory inventory = Bukkit.createInventory(null, 54, GUI_TITLE_UNITS + (page + 1) + "/" + totalPages);

        // Store session data - AFTER creating inventory but BEFORE opening
        GUISession session = new GUISession();
        session.mode = GUIMode.UNITS;
        session.structureUUID = structureUUID;
        session.currentPage = page;
        session.inventory = inventory;
        session.totalPages = totalPages;
        session.lastRefresh = System.currentTimeMillis();

        fileLogger.addLogMessage("[GUI] Storing UNITS session for UUID=" + viewer.getUniqueId());
        activeSessions.put(viewer.getUniqueId(), session);
        fileLogger.addLogMessage("[GUI] Session stored. Active sessions now: " + getActiveSessionsDebug());

        populateUnits(inventory, units, page);
        addNavigationItems(inventory, page, totalPages, units.size(), session.mode);

        fileLogger.addLogMessage("[GUI] Opening units inventory for " + viewer.getName());
        viewer.openInventory(inventory);
    }

    /**
     * Populates the given inventory with player heads based on the provided player data and the page number.
     * The inventory will display a limited number of items per page, and empty slots will be cleared.
     *
     * @param inventory the inventory to be populated with items
     * @param playerData the list of player data used for creating player head items
     * @param page the page number used to determine which subset of data to display in the inventory
     */
    private void populateInventory(Inventory inventory, List<CachedPlayerData> playerData, int page) {
        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, playerData.size());

        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            int dataIndex = startIndex + i;
            if (dataIndex < endIndex) {
                CachedPlayerData data = playerData.get(dataIndex);
                ItemStack playerHead = getOrCreatePlayerHead(data);
                inventory.setItem(i, playerHead);
            } else {
                inventory.setItem(i, null); // Clear empty slots
            }
        }
    }

    /**
     * Populates the provided inventory with details of the specified tunnel structures, paginated
     * based on the given page number. For each structure in the specified page, this method creates
     * an inventory item that contains metadata about the structure, including its UUID, total blocks,
     * total ores, length, and creation timestamp.
     *
     * @param inventory The Inventory to be populated with structure information. Cannot be null.
     * @param structures The list of TunnelStructure objects to be displayed in the inventory. Cannot be null.
     * @param page The page number (zero-based index) that determines which structures to display in the inventory.
     */
    private void populateStructures(@NotNull Inventory inventory, @NotNull List<TunnelStructure> structures, int page) {
        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, structures.size());

        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            int dataIndex = startIndex + i;
            if (dataIndex < endIndex) {
                TunnelStructure structure = structures.get(dataIndex);

                // Use a generic display; if you have a concrete TunnelStructure class, cast and use its methods directly.
                String uuid = structure.getUuid().toString();
                int totalBlocks = structure.getMainTunnelPath().getUnits().size();
                int totalOres = (int) structure.getMainTunnelPath().getUnits().stream().filter(TunnelUnit::isOre).count();
                double length = structure.getMainTunnelPath().getUnits().size();
                long createdAt = structure.getMainTunnelPath().getUnits().getFirst().getMinedAt();

                TunnelUnit tempUnit = structure.getMainTunnelPath().getUnits().getFirst();
                ItemStack item = tempUnit.getWorldName().contains("nether") ? new ItemStack(Material.NETHERRACK) : new ItemStack(Material.STONE);
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName("§6Structure §f" + uuid);

                List<String> lore = new ArrayList<>();
                lore.add("§bUUID: §f" + uuid);
                lore.add("§bWorld: §f" + tempUnit.getWorldName());
                lore.add("§bTotal Blocks: §f" + totalBlocks);
                lore.add("§bTotal Ores: §f" + totalOres);
                lore.add("§bLength: §f" + String.format("%.1f", length));
                lore.add("§bCreated: §f" + (createdAt > 0 ? formatTime(createdAt) : "Unknown"));
                lore.add("");
                lore.add("§eClick to view tunnel units");

                meta.setLore(lore);

                // store type/id into PDC
                if (keyType != null && keyId != null) {
                    meta.getPersistentDataContainer().set(keyType, PersistentDataType.STRING, "structure");
                    meta.getPersistentDataContainer().set(keyId, PersistentDataType.STRING, uuid == null ? "" : uuid);
                }

                item.setItemMeta(meta);
                inventory.setItem(i, item);
            } else {
                inventory.setItem(i, null);
            }
        }
    }

    /**
     * Populates the provided inventory with items representing TunnelUnit objects.
     * Each item displays relevant information (e.g., coordinates, material, exposure status)
     * and metadata about the TunnelUnit. The inventory is populated based on the specified
     * page, with a fixed number of items displayed per page.
     *
     * @param inventory the inventory to be populated with TunnelUnit items
     * @param units the list of TunnelUnit objects to display in the inventory
     * @param page the current page to display, determining the subset of TunnelUnit objects used
     */
    @SuppressWarnings("unchecked")
    private void populateUnits(@NotNull Inventory inventory, @NotNull List<TunnelUnit> units, int page) {
        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, units.size());

        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            int dataIndex = startIndex + i;
            if (dataIndex < endIndex) {
                TunnelUnit unit = units.get(dataIndex);

                String id = Integer.toString(dataIndex);
                int x = unit.getX();
                int z = unit.getZ();
                String materialName = unit.getMaterial().name();
                boolean exposed = unit.isExposedToAir();
                long minedAt = unit.getMinedAt();
                long createdAt = unit.getMinedAt();

                Material mat = Material.matchMaterial(materialName);
                if (mat == null) mat = Material.STONE;

                ItemStack item = new ItemStack(mat);
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName("§fUnit #" + id);

                List<String> lore = new ArrayList<>();
                lore.add("§bCoords: §fX=" + x + " Z=" + z);
                lore.add("§bMaterial: §f" + (materialName == null ? "Unknown" : materialName));
                lore.add("§bExposed: §f" + (exposed ? "§aYes" : "§cNo"));
                lore.add("§bMined At: §f" + (minedAt > 0 ? formatTime(minedAt) : "Never"));
                lore.add("§bCreated: §f" + (createdAt > 0 ? formatTime(createdAt) : "Unknown"));

                meta.setLore(lore);

                // store type/id
                if (keyType != null && keyId != null) {
                    meta.getPersistentDataContainer().set(keyType, PersistentDataType.STRING, "unit");
                    meta.getPersistentDataContainer().set(keyId, PersistentDataType.STRING, id == null ? "" : id);
                }

                item.setItemMeta(meta);
                inventory.setItem(i, item);
            } else {
                inventory.setItem(i, null);
            }
        }
    }

    // ---- Helpers for item creation ----


    /**
     * Retrieves a player's custom head item from the cache or creates a new one if it does not exist
     * or if the data is marked as dirty. The head item includes the player's display name, lore, and
     * other metadata.
     *
     * @param data the cached player data containing the player's UUID, name, and other metadata
     * @return the ItemStack representing the player's custom head
     */
    @NotNull
    private ItemStack getOrCreatePlayerHead(@NotNull CachedPlayerData data) {
        UUID playerUUID = data.playerUUID;

        // Check cache first
        ItemStack cachedHead = headCache.get(playerUUID);
        if (cachedHead != null && !data.isDirty) {
            return cachedHead.clone();
        }

        // Create new head
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUUID);
        meta.setOwningPlayer(offlinePlayer);
        meta.setDisplayName("§6" + data.playerName);

        List<String> lore = createPlayerLore(data);
        meta.setLore(lore);

        // store PDC so listener can detect miner id
        if (keyType != null && keyId != null) {
            meta.getPersistentDataContainer().set(keyType, PersistentDataType.STRING, "miner");
            meta.getPersistentDataContainer().set(keyId, PersistentDataType.STRING, playerUUID.toString());
        }

        head.setItemMeta(meta);

        // Cache the head
        headCache.put(playerUUID, head.clone());

        return head;
    }

    /**
     * Creates a list of strings representing the lore for a player's item, containing
     * various details about the player's activity and status.
     *
     * @param data the cached player data containing information such as the player's name,
     *             suspicion score, ore veins found, mining sessions, average path efficiency,
     *             last-seen time, and whether the player is suspicious, or not
     */
    @NotNull
    private List<String> createPlayerLore(@NotNull CachedPlayerData data) {
        List<String> lore = new ArrayList<>();

        lore.add("§7Player: §f" + data.playerName);
        lore.add("");

        // Suspicion score with color coding
        String suspicionColor = getSuspicionColor(data.suspicionScore);
        lore.add("§7Suspicion Score: " + suspicionColor + String.format("%.1f", data.suspicionScore) + "/100");

        lore.add("§7Ore Veins Found: §f" + data.oreVeinsFound);
        lore.add("§7Mining Sessions: §f" + data.miningSessions);

        if (data.avgPathEfficiency >= 0) {
            lore.add("§7Avg Path Efficiency: §f" + String.format("%.1f", data.avgPathEfficiency) + "%");
        }

        lore.add("§7Last Seen: §f" + formatTime(data.lastSeen));
        lore.add("");

        // Status indicators
        if (data.suspicionScore >= 80) {
            lore.add("§c§l⚠ HIGHLY SUSPICIOUS");
        } else if (data.suspicionScore >= 50) {
            lore.add("§6§l⚠ SUSPICIOUS");
        } else if (data.suspicionScore >= 30) {
            lore.add("§e§l⚠ WATCH");
        } else {
            lore.add("§a§l✓ NORMAL");
        }

        lore.add("");
        lore.add("§eClick to view detailed analysis");
        lore.add("§7Right-click for quick actions");

        return lore;
    }

    /**
     * Determines the color code representing the suspicion level based on the provided score.
     *
     * @param score the numerical score indicating the suspicion level
     * @return the color code as a string based on the score
     */
    private String getSuspicionColor(double score) {
        if (score >= 80) return "§c§l";
        if (score >= 50) return "§6";
        if (score >= 30) return "§e";
        return "§a";
    }


    /**
     * Adds navigation and control items to the provided inventory based on the current page,
     * total number of pages, total items, and the specified GUI mode. Navigation items include
     * buttons for moving to the previous or next page, a page indicator, and additional control
     * buttons specific to certain GUI modes.
     *
     * @param inventory the inventory to which the navigation items will be added
     * @param currentPage the index of the current page being displayed (0-based)
     * @param totalPages the total number of pages available
     * @param totalItems the total number of items across all pages
     * @param mode the GUI mode determining additional control items to display
     */
    private void addNavigationItems(@NotNull Inventory inventory, int currentPage, int totalPages, int totalItems, @NotNull GUIMode mode) {
        // Previous page button
        if (currentPage > 0) {
            ItemStack prevButton = new ItemStack(Material.ARROW);
            ItemMeta meta = prevButton.getItemMeta();
            meta.setDisplayName("§a← Previous Page");
            meta.setLore(Arrays.asList("§7Click to go to page " + currentPage));
            prevButton.setItemMeta(meta);
            inventory.setItem(45, prevButton);
        } else {
            inventory.setItem(45, null);
        }

        // Page indicator
        ItemStack pageIndicator = new ItemStack(Material.BOOK);
        ItemMeta meta = pageIndicator.getItemMeta();
        meta.setDisplayName("§6Page " + (currentPage + 1) + " of " + totalPages);

        int start = totalItems == 0 ? 0 : currentPage * ITEMS_PER_PAGE + 1;
        int end = Math.min((currentPage + 1) * ITEMS_PER_PAGE, totalItems);
        String showing = totalItems == 0 ? "§7Showing: §fNone" : "§7Showing: §f" + start + " to " + end + " of " + totalItems;

        meta.setLore(Arrays.asList(showing));
        pageIndicator.setItemMeta(meta);
        inventory.setItem(49, pageIndicator);

        // Next page button
        if (currentPage < totalPages - 1) {
            ItemStack nextButton = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = nextButton.getItemMeta();
            nextMeta.setDisplayName("§aNext Page →");
            nextMeta.setLore(Arrays.asList("§7Click to go to page " + (currentPage + 2)));
            nextButton.setItemMeta(nextMeta);
            inventory.setItem(53, nextButton);
        } else {
            inventory.setItem(53, null);
        }

        // Mode-specific control buttons (only show when in PLAYERS mode)
        if (mode == GUIMode.PLAYERS) {
            ItemStack sortSuspicion = new ItemStack(Material.REDSTONE);
            ItemMeta suspicionMeta = sortSuspicion.getItemMeta();
            suspicionMeta.setDisplayName("§cSort by Suspicion");
            suspicionMeta.setLore(Arrays.asList("§7Click to sort by suspicion score"));
            sortSuspicion.setItemMeta(suspicionMeta);
            inventory.setItem(47, sortSuspicion);

            ItemStack sortName = new ItemStack(Material.NAME_TAG);
            ItemMeta nameMeta = sortName.getItemMeta();
            nameMeta.setDisplayName("§bSort by Name");
            nameMeta.setLore(Arrays.asList("§7Click to sort alphabetically"));
            sortName.setItemMeta(nameMeta);
            inventory.setItem(48, sortName);

            ItemStack filter = new ItemStack(Material.HOPPER);
            ItemMeta filterMeta = filter.getItemMeta();
            filterMeta.setDisplayName("§eToggle Filter");
            filterMeta.setLore(Arrays.asList("§7Click to show only suspicious players"));
            filter.setItemMeta(filterMeta);
            inventory.setItem(52, filter);
        } else {
            inventory.setItem(47, null);
            inventory.setItem(48, null);
            inventory.setItem(52, null);
        }

        // Refresh button (common)
        ItemStack refresh = new ItemStack(Material.EMERALD);
        ItemMeta refreshMeta = refresh.getItemMeta();
        refreshMeta.setDisplayName("§aRefresh Data");
        refreshMeta.setLore(Arrays.asList("§7Click to refresh"));
        refresh.setItemMeta(refreshMeta);
        inventory.setItem(51, refresh);
    }

    /**
     * Updates the player cache with the latest data for all miners managed by the
     * {@code playerDataManager}. This method retrieves miner data, such as UUID,
     * suspicion score, and cached metrics, and populates or updates entries in
     * the {@code playerCache} map. It identifies changes to key player metrics
     * and marks these entries as "dirty" if any relevant data has been modified.
     * <p>
     * The method performs the following steps for each miner:
     * - Retrieves the UUID and name of the player.
     * - Retrieves information from the miner, such as the suspicion score.
     * - Constructs a new {@code CachedPlayerData} instance with updated values
     *   and estimates fixed metrics (e.g., ore veins found, mining sessions).
     * - Compares the new data with previously cached data to determine if
     *   the entry has changed.
     * - Updates the player cache with the new data.
     * <p>
     * This cache is used to track player data efficiently, ensuring that GUIs
     * and other systems relying on cached player details always have the latest
     * information.
     * <p>
     * Note: Certain player metrics, such as "ore veins found," may currently
     * use placeholder values until implemented with actual data.
     */
    private void updatePlayerCache() {
        for (Miner miner : playerDataManager.getMiners()) {
            UUID uuid = miner.getUuid();
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            String playerName = offlinePlayer.getName();
            if (playerName == null) playerName = "Unknown";

            CachedPlayerData cachedData = new CachedPlayerData();
            cachedData.playerUUID = uuid;
            cachedData.playerName = playerName;
            cachedData.suspicionScore = miner.getSuspicionScore();
            cachedData.lastSeen = offlinePlayer.getLastPlayed();
            cachedData.lastUpdated = System.currentTimeMillis();

            cachedData.oreVeinsFound = 3; // or whatever method you have TODO: fix this with actual real data :)
            cachedData.miningSessions = 6; // or whatever method you have
            cachedData.avgPathEfficiency = 44f; // or whatever method you have

            // Check if data changed
            CachedPlayerData oldData = playerCache.get(uuid);
            cachedData.isDirty = oldData == null ||
                    oldData.suspicionScore != cachedData.suspicionScore ||
                    oldData.oreVeinsFound != cachedData.oreVeinsFound;

            playerCache.put(uuid, cachedData);
        }
    }

    /**
     * Retrieves the active GUI session associated with the specified player's UUID.
     * If no session exists for the player, this method returns null.
     *
     * @param playerUUID the unique identifier (UUID) for the player
     * @return the {@code GUISession} associated with the given player's UUID,
     *         or {@code null} if no session exists
     */
    @Nullable
    public GUISession getSession(@NotNull UUID playerUUID) {
        return activeSessions.get(playerUUID);
    }

    /**
     * Closes and removes the session associated with the provided player's UUID.
     * This is typically used to clean up resources or end tracking for a specific player.
     *
     * @param playerUUID the unique identifier of the player whose session is to be closed
     */
    public void closeSession(@NotNull UUID playerUUID) {
        activeSessions.remove(playerUUID);
    }

    /**
     * Starts the update task responsible for asynchronously updating player data and refreshing GUI sessions.
     * <p>
     * This method ensures that only one update task is running at any given time by canceling any existing task
     * before starting a new one. The update task performs the following operations periodically:
     * <p>
     * 1. Calls `updatePlayerCache()` asynchronously to update cached player data.
     * 2. Schedules a synchronous task to refresh the GUI sessions for all active players.
     *    Only players currently online, with sessions that have not been refreshed within the last 3 seconds,
     *    will have their GUIs updated.
     * <p>
     * The task is executed with an initial delay and interval of 60 ticks (3 seconds).
     * <p>
     * This method is critical for maintaining the consistency of data displayed in the GUIs by ensuring
     * efficient synchronization between the cache and the player's session views. The task operates across
     * both asynchronous and synchronous contexts to balance performance and thread safety.
     * <p>
     * Preconditions:
     * - The `plugin` field must not be null; otherwise, the method will exit without performing any operations.
     */
    private void startUpdateTask() {
        if (plugin == null) return;

        // If already running, cancel first so we don't schedule duplicates
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }

        updateTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            // Update cache asynchronously
            updatePlayerCache();

            // Update open GUIs on the main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (Map.Entry<UUID, GUISession> entry : activeSessions.entrySet()) {
                    Player player = Bukkit.getPlayer(entry.getKey());
                    if (player != null && player.isOnline()) {
                        GUISession session = entry.getValue();
                        if (System.currentTimeMillis() - session.lastRefresh > 3000) { // Update every ~3 seconds
                            refreshGUIForPlayer(player, session);
                            session.lastRefresh = System.currentTimeMillis();
                        }
                    }
                }
            });
        }, 60L, 60L); // 60 ticks initial delay, 60 ticks period
    }

    /**
     * Filters and sorts player data from the cache based on the specified criteria.
     * The method retrieves player data from the cache, filters it to include only
     * players that match the provided "showOnlySuspicious" flag, and sorts the data
     * either by player name (alphabetically) or suspicion score (descending), depending
     * on the specified sort type.
     *
     * @param sortType the type of sorting to apply. Acceptable values are "name" for
     *                 alphabetical sorting by player name and any other value for sorting
     *                 by suspicion score in descending order.
     * @param showOnlySuspicious whether to include only players with a suspicion score
     *                           greater than or equal to 15.0. If false, all players
     *                           will be included.
     * @return a list of {@code CachedPlayerData} objects that match the filtering criteria
     *         and are sorted as specified.
     */
    @NotNull
    private List<CachedPlayerData> getFilteredAndSortedPlayerData(@NotNull String sortType, boolean showOnlySuspicious) {
        updatePlayerCache();
        return playerCache.values().stream().filter(data -> !showOnlySuspicious || data.suspicionScore >= 15.0).sorted((a, b) -> {
            if (sortType.equals("name")) {
                return a.playerName.compareToIgnoreCase(b.playerName);
            } else {
                return Double.compare(b.suspicionScore, a.suspicionScore); // Descending
            }
        }).collect(Collectors.toList());
    }

    /**
     * Refreshes and updates the player's GUI based on the provided session information.
     * The method dynamically adjusts the inventory and navigation items depending on the
     * current GUI mode (PLAYERS, STRUCTURES, or UNITS) and the associated data.
     *
     * @param player The player whose GUI needs to be refreshed.
     * @param session The session object containing GUI state, configuration, and the data relevant to the current mode.
     */
    private void refreshGUIForPlayer(Player player, GUISession session) {
        if (session.mode == GUIMode.PLAYERS) {
            List<CachedPlayerData> playerData = getFilteredAndSortedPlayerData(session.sortType, session.showOnlySuspicious);
            // UPDATE total pages in case data changed
            session.totalPages = Math.max(1, (int) Math.ceil((double) playerData.size() / ITEMS_PER_PAGE));
            // Make sure current page is still valid
            session.currentPage = Math.min(session.currentPage, session.totalPages - 1);

            populateInventory(session.inventory, playerData, session.currentPage);
            addNavigationItems(session.inventory, session.currentPage, session.totalPages, playerData.size(), session.mode);
        } else if (session.mode == GUIMode.STRUCTURES) {
            List<TunnelStructure> structures = playerDataManager.getMiner(session.minerUUID).getCreatedTunnels();
            session.totalPages = Math.max(1, (int) Math.ceil((double) structures.size() / ITEMS_PER_PAGE));
            session.currentPage = Math.min(session.currentPage, session.totalPages - 1);

            populateStructures(session.inventory, structures, session.currentPage);
            addNavigationItems(session.inventory, session.currentPage, session.totalPages, structures.size(), session.mode);
        } else if (session.mode == GUIMode.UNITS) {
            List<TunnelUnit> units = playerDataManager.getTunnelStructure(UUID.fromString(session.structureUUID)).getMainTunnelPath().getUnits();
            session.totalPages = Math.max(1, (int) Math.ceil((double) units.size() / ITEMS_PER_PAGE));
            session.currentPage = Math.min(session.currentPage, session.totalPages - 1);

            populateUnits(session.inventory, units, session.currentPage);
            addNavigationItems(session.inventory, session.currentPage, session.totalPages, units.size(), session.mode);
        }
    }

    /**
     * Formats the provided timestamp as a relative time string, indicating the time elapsed
     * since the given timestamp compared to the current system time. Returns a time format in days, hours,
     * minutes, or "Just now" if the difference is less than a minute. If the timestamp is invalid
     * or zero, it returns "Never".
     *
     * @param timestamp the timestamp in milliseconds since epoch to format
     * @return a formatted string representing the time elapsed since the given timestamp
     */
    @NotNull
    private String formatTime(long timestamp) {
        if (timestamp <= 0) return "Never";

        long diff = System.currentTimeMillis() - timestamp;
        long minutes = diff / (60 * 1000);
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) return days + "d ago";
        if (hours > 0) return hours + "h ago";
        if (minutes > 0) return minutes + "m ago";
        return "Just now";
    }

    public void shutdown() {
        if (updateTask != null) {
            updateTask.cancel();
        }
        activeSessions.clear();
        playerCache.clear();
        headCache.clear();
    }

    /**
     * Info returned to the listener when parsing a clicked item.
     */
    public static class ClickInfo {
        public String type; // miner | structure | unit
        public String id;   // UUID or id string
    }

    /**
     * Attempts to parse an ItemStack placed by this GUI and return its type/id.
     * Listener should use this instead of inspecting displayName or lore.
     */
    public ClickInfo parseClickedItem(ItemStack item) {
        if (item == null || !item.hasItemMeta() || plugin == null || keyType == null || keyId == null) return null;
        ItemMeta meta = item.getItemMeta();
        String t = meta.getPersistentDataContainer().get(keyType, PersistentDataType.STRING);
        String id = meta.getPersistentDataContainer().get(keyId, PersistentDataType.STRING);
        if (t == null || id == null) return null;
        ClickInfo info = new ClickInfo();
        info.type = t;
        info.id = id;
        return info;
    }

    // ---- Inner classes ----

    public static class GUISession {
        public GUIMode mode = GUIMode.PLAYERS;
        public int currentPage;
        public String sortType;
        public boolean showOnlySuspicious;
        public Inventory inventory;
        public int totalPages;
        public long lastRefresh;

        // drilldown context
        public UUID minerUUID;
        public String structureUUID;
    }

    private static class CachedPlayerData {
        public UUID playerUUID;
        public String playerName;
        public double suspicionScore;
        public int oreVeinsFound;
        public int miningSessions;
        public double avgPathEfficiency;
        public long lastSeen;
        public long lastUpdated;
        public boolean isDirty;
    }
}
