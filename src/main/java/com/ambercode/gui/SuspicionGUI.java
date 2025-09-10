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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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

    // ---- Public GUI entry points ----

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

    public void openGUI(Player viewer, int page, String sortType, boolean showOnlySuspicious) {
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
    // ---- Populators ----

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
     * Populates the inventory with structure items.
     * Expects "structures" to be a List of your TunnelStructure model (whatever your PlayerDataManager returns).
     * The method uses reflection-like getter assumptions:
     * - structure.getUuid()
     * - structure.getTotalBlocks()
     * - structure.getTotalOres()
     * - structure.getLength()
     * - structure.getCreatedAt()  -> (epoch ms) OR something convertible to long via toString (best if it's long)
     *
     * Adjust to match your actual TunnelStructure class if needed.
     */
    @SuppressWarnings("unchecked")
    private void populateStructures(Inventory inventory, List<TunnelStructure> structures, int page) {
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

                ItemStack item = new ItemStack(Material.STONE);
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName("§6Structure §f" + uuid);

                List<String> lore = new ArrayList<>();
                lore.add("§7UUID: §f" + uuid);
                lore.add("§7Total Blocks: §f" + totalBlocks);
                lore.add("§7Total Ores: §f" + totalOres);
                lore.add("§7Length: §f" + String.format("%.1f", length));
                lore.add("§7Created: §f" + (createdAt > 0 ? formatTime(createdAt) : "Unknown"));
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
     * Populates the inventory with tunnel units.
     * Expects "units" to be List<TunnelUnit> or equivalent with getters:
     * - getId()
     * - getX()
     * - getZ()
     * - getMaterial() (String name for Bukkit Material)
     * - isExposed()
     * - getMinedAt() (epoch ms)
     * - getCreatedAt() (epoch ms)
     * <p>
     * Adapt the introspection if your concrete class differs.
     */
    @SuppressWarnings("unchecked")
    private void populateUnits(Inventory inventory, List<TunnelUnit> units, int page) {
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
                lore.add("§7Coords: §fX=" + x + " Z=" + z);
                lore.add("§7Material: §f" + (materialName == null ? "Unknown" : materialName));
                lore.add("§7Exposed: §f" + (exposed ? "§aYes" : "§cNo"));
                lore.add("§7Mined At: §f" + (minedAt > 0 ? formatTime(minedAt) : "Never"));
                lore.add("§7Created: §f" + (createdAt > 0 ? formatTime(createdAt) : "Unknown"));

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

    private ItemStack getOrCreatePlayerHead(CachedPlayerData data) {
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

    private List<String> createPlayerLore(CachedPlayerData data) {
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

    private String getSuspicionColor(double score) {
        if (score >= 80) return "§c§l";
        if (score >= 50) return "§6";
        if (score >= 30) return "§e";
        return "§a";
    }

    // Navigation row (common for all modes) — totalItems used to show "Showing X to Y of N"
    private void addNavigationItems(Inventory inventory, int currentPage, int totalPages, int totalItems, GUIMode mode) {
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

    // ---- Cache & update logic (unchanged mostly) ----

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

            // ADD THESE LINES - get the actual values from your Miner object:
            cachedData.oreVeinsFound = 3; // or whatever method you have
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

    public GUISession getSession(UUID playerUUID) {
        return activeSessions.get(playerUUID);
    }

    public void closeSession(UUID playerUUID) {
        activeSessions.remove(playerUUID);
    }

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

    private List<CachedPlayerData> getFilteredAndSortedPlayerData(String sortType, boolean showOnlySuspicious) {
        updatePlayerCache();
        return playerCache.values().stream().filter(data -> !showOnlySuspicious || data.suspicionScore >= 15.0).sorted((a, b) -> {
            if (sortType.equals("name")) {
                return a.playerName.compareToIgnoreCase(b.playerName);
            } else {
                return Double.compare(b.suspicionScore, a.suspicionScore); // Descending
            }
        }).collect(Collectors.toList());
    }

    // FIX 2: In refreshGUIForPlayer(), you need to recalculate totalPages
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

    // ---- Click parsing helper (listener calls this) ----

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
