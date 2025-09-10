package com.ambercode.gui;

import com.ambercode.data.Miner;
import com.ambercode.manager.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class SuspicionGUI {

    private static final int ITEMS_PER_PAGE = 45; // 9x5 grid
    private static final String GUI_TITLE = "§6§lX-Ray Detection - Page ";

    private final PlayerDataManager playerDataManager;
    private final Map<UUID, GUISession> activeSessions = new ConcurrentHashMap<>();
    private final Map<UUID, CachedPlayerData> playerCache = new ConcurrentHashMap<>();
    private final Map<UUID, ItemStack> headCache = new ConcurrentHashMap<>();
    private BukkitTask updateTask;
    private Plugin plugin;

    public SuspicionGUI(PlayerDataManager playerDataManager) {
        this.playerDataManager = playerDataManager;
        startUpdateTask();
    }

    public void setPlugin(Plugin plugin) {
        this.plugin = plugin;
    }

    public void openGUI(Player viewer, int page, String sortType, boolean showOnlySuspicious) {
        List<CachedPlayerData> playerData = getFilteredAndSortedPlayerData(sortType, showOnlySuspicious);

        int totalPages = Math.max(1, (int) Math.ceil((double) playerData.size() / ITEMS_PER_PAGE));
        page = Math.max(0, Math.min(page, totalPages - 1));

        Inventory inventory = Bukkit.createInventory(null, 54, GUI_TITLE + (page + 1) + "/" + totalPages);

        // Store session data
        GUISession session = new GUISession();
        session.currentPage = page;
        session.sortType = sortType;
        session.showOnlySuspicious = showOnlySuspicious;
        session.inventory = inventory;
        session.totalPages = totalPages;
        session.lastRefresh = System.currentTimeMillis();
        activeSessions.put(viewer.getUniqueId(), session);

        populateInventory(inventory, playerData, page);
        addNavigationItems(inventory, page, totalPages);

        viewer.openInventory(inventory);
    }

    private List<CachedPlayerData> getFilteredAndSortedPlayerData(String sortType, boolean showOnlySuspicious) {
        updatePlayerCache();

        return playerCache.values().stream()
                .filter(data -> !showOnlySuspicious || data.suspicionScore >= 30.0)
                .sorted((a, b) -> {
                    if (sortType.equals("name")) {
                        return a.playerName.compareToIgnoreCase(b.playerName);
                    } else {
                        return Double.compare(b.suspicionScore, a.suspicionScore); // Descending
                    }
                })
                .collect(Collectors.toList());
    }

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

    private void addNavigationItems(Inventory inventory, int currentPage, int totalPages) {
        // Previous page button
        if (currentPage > 0) {
            ItemStack prevButton = new ItemStack(Material.ARROW);
            ItemMeta meta = prevButton.getItemMeta();
            meta.setDisplayName("§a← Previous Page");
            meta.setLore(Arrays.asList("§7Click to go to page " + currentPage));
            prevButton.setItemMeta(meta);
            inventory.setItem(45, prevButton);
        }

        // Page indicator
        ItemStack pageIndicator = new ItemStack(Material.BOOK);
        ItemMeta meta = pageIndicator.getItemMeta();
        meta.setDisplayName("§6Page " + (currentPage + 1) + " of " + totalPages);
        meta.setLore(Arrays.asList(
                "§7Showing players " + (currentPage * ITEMS_PER_PAGE + 1) +
                        " to " + Math.min((currentPage + 1) * ITEMS_PER_PAGE, playerCache.size())
        ));
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
        }

        // Sort by suspicion button
        ItemStack sortSuspicion = new ItemStack(Material.REDSTONE);
        ItemMeta suspicionMeta = sortSuspicion.getItemMeta();
        suspicionMeta.setDisplayName("§cSort by Suspicion");
        suspicionMeta.setLore(Arrays.asList("§7Click to sort by suspicion score"));
        sortSuspicion.setItemMeta(suspicionMeta);
        inventory.setItem(47, sortSuspicion);

        // Sort by name button
        ItemStack sortName = new ItemStack(Material.NAME_TAG);
        ItemMeta nameMeta = sortName.getItemMeta();
        nameMeta.setDisplayName("§bSort by Name");
        nameMeta.setLore(Arrays.asList("§7Click to sort alphabetically"));
        sortName.setItemMeta(nameMeta);
        inventory.setItem(48, sortName);

        // Refresh button
        ItemStack refresh = new ItemStack(Material.EMERALD);
        ItemMeta refreshMeta = refresh.getItemMeta();
        refreshMeta.setDisplayName("§aRefresh Data");
        refreshMeta.setLore(Arrays.asList("§7Click to refresh player data"));
        refresh.setItemMeta(refreshMeta);
        inventory.setItem(51, refresh);

        // Filter button
        ItemStack filter = new ItemStack(Material.HOPPER);
        ItemMeta filterMeta = filter.getItemMeta();
        filterMeta.setDisplayName("§eToggle Filter");
        filterMeta.setLore(Arrays.asList("§7Click to show only suspicious players"));
        filter.setItemMeta(filterMeta);
        inventory.setItem(52, filter);
    }

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
        if (plugin != null) {
            updateTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
                // Update cache in async
                updatePlayerCache();

                // Update open GUIs on main thread
                Bukkit.getScheduler().runTask(plugin, () -> {
                    for (Map.Entry<UUID, GUISession> entry : activeSessions.entrySet()) {
                        Player player = Bukkit.getPlayer(entry.getKey());
                        if (player != null && player.isOnline()) {
                            GUISession session = entry.getValue();
                            if (System.currentTimeMillis() - session.lastRefresh > 3000) { // Update every 3 seconds
                                refreshGUIForPlayer(player, session);
                                session.lastRefresh = System.currentTimeMillis();
                            }
                        }
                    }
                });
            }, 60L, 60L); // Run every 3 seconds (60 ticks)
        }
    }

    private void refreshGUIForPlayer(Player player, GUISession session) {
        List<CachedPlayerData> playerData = getFilteredAndSortedPlayerData(session.sortType, session.showOnlySuspicious);
        populateInventory(session.inventory, playerData, session.currentPage);
    }

    private String formatTime(long timestamp) {
        if (timestamp == 0) return "Never";

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

    // Inner classes
    public static class GUISession {
        public int currentPage;
        public String sortType;
        public boolean showOnlySuspicious;
        public Inventory inventory;
        public int totalPages;
        public long lastRefresh;
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