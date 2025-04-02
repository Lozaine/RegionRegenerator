package com.minecraft.regionregen.gui;

import com.minecraft.regionregen.RegionRegenPlugin;
import com.minecraft.regionregen.models.Region;
import com.minecraft.regionregen.utils.MessageUtils;
import org.bukkit.Location;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * GUI for managing regions
 */
public class RegionGUI {

    private static final String INVENTORY_TITLE = "Region Manager";
    private static final int ROWS = 6; // 6 rows (54 slots)
    private static final int REGIONS_PER_PAGE = 28;

    private static final Map<UUID, Integer> playerPages = new HashMap<>();
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * Open the main region management GUI for a player
     *
     * @param player The player to show the GUI to
     */
    public static void openMainGUI(Player player) {
        openMainGUI(player, 1);
    }

    /**
     * Open the main region management GUI for a player at a specific page
     *
     * @param player The player to show the GUI to
     * @param page The page number to display
     */
    public static void openMainGUI(Player player, int page) {
        // Get the regions
        List<Region> regions = new ArrayList<>(RegionRegenPlugin.getInstance().getRegionManager().getAllRegions());

        // Sort regions alphabetically by ID
        regions.sort(Comparator.comparing(Region::getId));

        // Calculate the total pages
        int totalPages = (int) Math.ceil((double) regions.size() / REGIONS_PER_PAGE);
        if (totalPages < 1) totalPages = 1;

        // Adjust page if out of bounds
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;

        // Store current page for player
        playerPages.put(player.getUniqueId(), page);

        // Create inventory
        String title = RegionRegenPlugin.getInstance().getConfigManager().getConfig().getString("gui.main-title", INVENTORY_TITLE);
        Inventory inventory = Bukkit.createInventory(null, ROWS * 9, ChatColor.translateAlternateColorCodes('&', title));

        // Add regions for current page
        int startIndex = (page - 1) * REGIONS_PER_PAGE;
        int endIndex = Math.min(startIndex + REGIONS_PER_PAGE, regions.size());

        for (int i = startIndex; i < endIndex; i++) {
            Region region = regions.get(i);

            Material material = Material.GRASS_BLOCK;

            // Check if regeneration is in progress
            if (RegionRegenPlugin.getInstance().getRegionManager().isRegenerationInProgress(region.getId())) {
                material = Material.REDSTONE_BLOCK;
            } else if (region.getLastRegenerationDate() != null) {
                material = Material.EMERALD_BLOCK;
            }

            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.GREEN + region.getId());

            // Set lore with region info
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "World: " + ChatColor.WHITE + region.getWorldName());
            lore.add(ChatColor.GRAY + "Creator: " + ChatColor.WHITE + region.getCreatorName());
            lore.add(ChatColor.GRAY + "Volume: " + ChatColor.WHITE + region.getVolume() + " blocks");
            lore.add(ChatColor.GRAY + "Regeneration count: " + ChatColor.WHITE + region.getRegenerationCount());

            if (region.getLastRegenerationDate() != null) {
                lore.add(ChatColor.GRAY + "Last regenerated: " + ChatColor.WHITE +
                        dateFormat.format(region.getLastRegenerationDate()));
            } else {
                lore.add(ChatColor.GRAY + "Last regenerated: " + ChatColor.WHITE + "Never");
            }

            if (RegionRegenPlugin.getInstance().getRegionManager().isRegenerationInProgress(region.getId())) {
                lore.add("");
                lore.add(ChatColor.RED + "Regeneration in progress!");
            } else {
                lore.add("");
                lore.add(ChatColor.YELLOW + "Left-click to view details");
                lore.add(ChatColor.YELLOW + "Right-click to regenerate");
            }

            meta.setLore(lore);
            item.setItemMeta(meta);

            // Add to inventory (calculate position)
            int row = (i - startIndex) / 7;
            int col = (i - startIndex) % 7;
            inventory.setItem(row * 9 + col + 1, item);
        }

        // Add pagination controls if needed
        if (totalPages > 1) {
            // Previous page button
            if (page > 1) {
                ItemStack prevItem = new ItemStack(Material.ARROW);
                ItemMeta prevMeta = prevItem.getItemMeta();
                prevMeta.setDisplayName(ChatColor.YELLOW + "Previous Page");
                prevItem.setItemMeta(prevMeta);
                inventory.setItem(45, prevItem);
            }

            // Next page button
            if (page < totalPages) {
                ItemStack nextItem = new ItemStack(Material.ARROW);
                ItemMeta nextMeta = nextItem.getItemMeta();
                nextMeta.setDisplayName(ChatColor.YELLOW + "Next Page");
                nextItem.setItemMeta(nextMeta);
                inventory.setItem(53, nextItem);
            }

            // Page indicator
            ItemStack pageItem = new ItemStack(Material.PAPER);
            ItemMeta pageMeta = pageItem.getItemMeta();
            pageMeta.setDisplayName(ChatColor.GREEN + "Page " + page + " of " + totalPages);
            pageItem.setItemMeta(pageMeta);
            inventory.setItem(49, pageItem);
        }

        // Add region creation button
        ItemStack createItem = new ItemStack(Material.NETHER_STAR);
        ItemMeta createMeta = createItem.getItemMeta();
        createMeta.setDisplayName(ChatColor.GREEN + "Create New Region");
        List<String> createLore = new ArrayList<>();
        createLore.add(ChatColor.GRAY + "Click to create a new region");
        createLore.add(ChatColor.GRAY + "from your WorldEdit selection");
        createMeta.setLore(createLore);
        createItem.setItemMeta(createMeta);
        inventory.setItem(4, createItem);

        // Add refresh button
        ItemStack refreshItem = new ItemStack(Material.CLOCK);
        ItemMeta refreshMeta = refreshItem.getItemMeta();
        refreshMeta.setDisplayName(ChatColor.AQUA + "Refresh");
        refreshItem.setItemMeta(refreshMeta);
        inventory.setItem(8, refreshItem);

        // Open inventory for player
        player.openInventory(inventory);
    }

    /**
     * Open the region detail GUI for a player
     *
     * @param player The player to show the GUI to
     * @param region The region to display details for
     */
    public static void openRegionDetailGUI(Player player, Region region) {
        String title = RegionRegenPlugin.getInstance().getConfigManager().getConfig()
                .getString("gui.info-title", "Region Info: %region%")
                .replace("%region%", region.getId());

        Inventory inventory = Bukkit.createInventory(null, 3 * 9, ChatColor.translateAlternateColorCodes('&', title));

        // Region info item
        ItemStack infoItem = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = infoItem.getItemMeta();
        infoMeta.setDisplayName(ChatColor.GREEN + "Region Info: " + region.getId());

        List<String> infoLore = new ArrayList<>();
        infoLore.add(ChatColor.GRAY + "World: " + ChatColor.WHITE + region.getWorldName());
        infoLore.add(ChatColor.GRAY + "Creator: " + ChatColor.WHITE + region.getCreatorName());
        infoLore.add(ChatColor.GRAY + "Created: " + ChatColor.WHITE + dateFormat.format(region.getCreationDate()));
        infoLore.add(ChatColor.GRAY + "Volume: " + ChatColor.WHITE + region.getVolume() + " blocks");
        infoLore.add(ChatColor.GRAY + "Min: " + ChatColor.WHITE + formatLocation(region.getMinPoint()));
        infoLore.add(ChatColor.GRAY + "Max: " + ChatColor.WHITE + formatLocation(region.getMaxPoint()));
        infoLore.add(ChatColor.GRAY + "Regeneration count: " + ChatColor.WHITE + region.getRegenerationCount());

        if (region.getLastRegenerationDate() != null) {
            infoLore.add(ChatColor.GRAY + "Last regenerated: " + ChatColor.WHITE +
                    dateFormat.format(region.getLastRegenerationDate()));
        } else {
            infoLore.add(ChatColor.GRAY + "Last regenerated: " + ChatColor.WHITE + "Never");
        }

        infoMeta.setLore(infoLore);
        infoItem.setItemMeta(infoMeta);
        inventory.setItem(4, infoItem);

        // Regenerate button
        if (!RegionRegenPlugin.getInstance().getRegionManager().isRegenerationInProgress(region.getId())) {
            ItemStack regenerateItem = new ItemStack(Material.REDSTONE);
            ItemMeta regenerateMeta = regenerateItem.getItemMeta();
            regenerateMeta.setDisplayName(ChatColor.RED + "Regenerate Region");

            List<String> regenerateLore = new ArrayList<>();
            regenerateLore.add(ChatColor.GRAY + "Click to regenerate this region");
            regenerateLore.add(ChatColor.RED + "Warning: This cannot be undone!");

            regenerateMeta.setLore(regenerateLore);
            regenerateItem.setItemMeta(regenerateMeta);
            inventory.setItem(12, regenerateItem);
        } else {
            ItemStack inProgressItem = new ItemStack(Material.BARRIER);
            ItemMeta inProgressMeta = inProgressItem.getItemMeta();
            inProgressMeta.setDisplayName(ChatColor.RED + "Regeneration in Progress");
            inProgressItem.setItemMeta(inProgressMeta);
            inventory.setItem(12, inProgressItem);
        }

        // Teleport button
        ItemStack teleportItem = new ItemStack(Material.ENDER_PEARL);
        ItemMeta teleportMeta = teleportItem.getItemMeta();
        teleportMeta.setDisplayName(ChatColor.LIGHT_PURPLE + "Teleport to Region");
        teleportItem.setItemMeta(teleportMeta);
        inventory.setItem(14, teleportItem);

        // Delete button
        ItemStack deleteItem = new ItemStack(Material.LAVA_BUCKET);
        ItemMeta deleteMeta = deleteItem.getItemMeta();
        deleteMeta.setDisplayName(ChatColor.DARK_RED + "Delete Region");

        List<String> deleteLore = new ArrayList<>();
        deleteLore.add(ChatColor.GRAY + "Click to delete this region");
        deleteLore.add(ChatColor.RED + "Warning: This cannot be undone!");

        deleteMeta.setLore(deleteLore);
        deleteItem.setItemMeta(deleteMeta);
        inventory.setItem(16, deleteItem);

        // Back button
        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.setDisplayName(ChatColor.YELLOW + "Back to List");
        backItem.setItemMeta(backMeta);
        inventory.setItem(22, backItem);

        // Open inventory for player
        player.openInventory(inventory);
    }

    /**
     * Open the create region GUI for a player
     *
     * @param player The player to show the GUI to
     */
    public static void openCreateRegionGUI(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 3 * 9, "Create Region");

        // Info item
        ItemStack infoItem = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = infoItem.getItemMeta();
        infoMeta.setDisplayName(ChatColor.YELLOW + "Create a New Region");

        List<String> infoLore = new ArrayList<>();
        infoLore.add(ChatColor.GRAY + "Type a name for your region in chat");
        infoLore.add(ChatColor.GRAY + "Make sure you have a WorldEdit selection");
        infoLore.add(ChatColor.GRAY + "Click the green button to create");

        infoMeta.setLore(infoLore);
        infoItem.setItemMeta(infoMeta);
        inventory.setItem(4, infoItem);

        // Create button
        ItemStack createItem = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta createMeta = createItem.getItemMeta();
        createMeta.setDisplayName(ChatColor.GREEN + "Create Region");
        createItem.setItemMeta(createMeta);
        inventory.setItem(11, createItem);

        // Cancel button
        ItemStack cancelItem = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta cancelMeta = cancelItem.getItemMeta();
        cancelMeta.setDisplayName(ChatColor.RED + "Cancel");
        cancelItem.setItemMeta(cancelMeta);
        inventory.setItem(15, cancelItem);

        // Open inventory for player
        player.openInventory(inventory);

        // Close inventory and prompt for region name
        new BukkitRunnable() {
            @Override
            public void run() {
                player.closeInventory();
                MessageUtils.sendMessage(player, "§eType the name for your new region in chat, or type §ccancel §eto abort:");

                // Handle chat input in a separate class or listener
            }
        }.runTaskLater(RegionRegenPlugin.getInstance(), 20L);
    }

    /**
     * Format a location for display
     *
     * @param loc The location to format
     * @return Formatted location string
     */
    private static String formatLocation(Location loc) {
        return String.format("%.0f, %.0f, %.0f", loc.getX(), loc.getY(), loc.getZ());
    }

    /**
     * Get the current page a player is viewing
     *
     * @param player The player
     * @return The page number, or 1 if not set
     */
    public static int getCurrentPage(Player player) {
        return playerPages.getOrDefault(player.getUniqueId(), 1);
    }
}