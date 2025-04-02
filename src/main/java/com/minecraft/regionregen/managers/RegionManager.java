package com.minecraft.regionregen.managers;

import com.minecraft.regionregen.RegionRegenPlugin;
import com.minecraft.regionregen.utils.MessageUtils;
import com.minecraft.regionregen.utils.WorldEditUtils;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Manages all regions registered in the plugin
 */
public class RegionManager {

    private final RegionRegenPlugin plugin;
    private final Map<String, com.minecraft.regionregen.models.Region> regions = new HashMap<>();
    private final File regionsFile;
    private final Map<String, Boolean> regenerationInProgress = new HashMap<>();

    /**
     * Create a new RegionManager
     *
     * @param plugin The plugin instance
     */
    public RegionManager(RegionRegenPlugin plugin) {
        this.plugin = plugin;
        this.regionsFile = new File(plugin.getDataFolder(), "regions.yml");
    }

    /**
     * Load all regions from storage
     */
    public void loadRegions() {
        if (!regionsFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                regionsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to create regions file", e);
                return;
            }
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(regionsFile);

        if (!config.contains("regions")) {
            // No regions saved yet
            return;
        }

        for (String key : config.getConfigurationSection("regions").getKeys(false)) {
            try {
                Map<String, Object> regionMap = config.getConfigurationSection("regions." + key).getValues(true);
                com.minecraft.regionregen.models.Region region = new com.minecraft.regionregen.models.Region(regionMap);
                regions.put(region.getId(), region);
                MessageUtils.debug("Loaded region: " + region.getId());
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load region: " + key, e);
            }
        }

        MessageUtils.log("Loaded " + regions.size() + " regions.");
    }

    /**
     * Save all regions to storage
     */
    public void saveRegions() {
        FileConfiguration config = new YamlConfiguration();

        for (com.minecraft.regionregen.models.Region region : regions.values()) {
            String path = "regions." + region.getId();
            Map<String, Object> serialized = region.serialize();

            for (Map.Entry<String, Object> entry : serialized.entrySet()) {
                config.set(path + "." + entry.getKey(), entry.getValue());
            }
        }

        try {
            config.save(regionsFile);
            MessageUtils.debug("Saved " + regions.size() + " regions.");
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save regions", e);
        }
    }

    /**
     * Create a new region from a player's WorldEdit selection
     *
     * @param player The player creating the region
     * @param id The ID of the region
     * @return The created region, or null if creation failed
     */
    public com.minecraft.regionregen.models.Region createRegion(Player player, String id) {
        // Check if a region with this ID already exists
        if (regions.containsKey(id)) {
            return null;
        }

        // Get player's WorldEdit selection
        com.sk89q.worldedit.regions.Region selection = WorldEditUtils.getPlayerSelection(player);
        if (selection == null) {
            return null;
        }

        // Convert WorldEdit region to our Region format
        BlockVector3 minPoint = selection.getMinimumPoint();
        BlockVector3 maxPoint = selection.getMaximumPoint();

        Location min = new Location(
                player.getWorld(),
                minPoint.getX(),
                minPoint.getY(),
                minPoint.getZ()
        );

        Location max = new Location(
                player.getWorld(),
                maxPoint.getX(),
                maxPoint.getY(),
                maxPoint.getZ()
        );

        com.minecraft.regionregen.models.Region region = new com.minecraft.regionregen.models.Region(id, player.getWorld().getName(), min, max, player.getName());
        regions.put(id, region);

        // Save if configured to do so on modification
        if (plugin.getConfigManager().getConfig().getBoolean("storage.save-on-modify", true)) {
            saveRegions();
        }

        return region;
    }

    /**
     * Delete a region
     *
     * @param id The ID of the region to delete
     * @return True if the region was deleted, false if it wasn't found
     */
    public boolean deleteRegion(String id) {
        if (!regions.containsKey(id)) {
            return false;
        }

        regions.remove(id);

        // Save if configured to do so on modification
        if (plugin.getConfigManager().getConfig().getBoolean("storage.save-on-modify", true)) {
            saveRegions();
        }

        return true;
    }

    /**
     * Get a region by ID
     *
     * @param id The ID of the region
     * @return The region, or null if not found
     */
    public com.minecraft.regionregen.models.Region getRegion(String id) {
        return regions.get(id);
    }

    /**
     * Get all regions
     *
     * @return A collection of all regions
     */
    public Collection<com.minecraft.regionregen.models.Region> getAllRegions() {
        return regions.values();
    }

    /**
     * Check if regeneration is in progress for a region
     *
     * @param regionId The region ID to check
     * @return True if regeneration is in progress
     */
    public boolean isRegenerationInProgress(String regionId) {
        return regenerationInProgress.getOrDefault(regionId, false);
    }

    /**
     * Regenerate a region
     *
     * @param region The region to regenerate
     * @param initiator The player who initiated the regeneration (can be null)
     * @return A CompletableFuture that completes when regeneration is done
     */
    public CompletableFuture<Boolean> regenerateRegion(com.minecraft.regionregen.models.Region region, Player initiator) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        // Check if regeneration is already in progress
        if (isRegenerationInProgress(region.getId())) {
            future.complete(false);
            return future;
        }

        regenerationInProgress.put(region.getId(), true);

        World world = Bukkit.getWorld(region.getWorldName());
        if (world == null) {
            MessageUtils.log("§cCannot regenerate region " + region.getId() +
                    ": world " + region.getWorldName() + " does not exist.");
            regenerationInProgress.put(region.getId(), false);
            future.complete(false);
            return future;
        }

        // Notify nearby players if configured
        if (plugin.getConfigManager().getConfig().getBoolean("regeneration.notify-nearby-players", true)) {
            notifyNearbyPlayers(region);
        }

        // Log the regeneration
        MessageUtils.log("§aStarting regeneration of region: " + region.getId());
        if (initiator != null) {
            MessageUtils.sendMessage(initiator, "§aStarting regeneration of region: §e" + region.getId());
        }

        // Get the blocks per tick from config
        int blocksPerTick = plugin.getConfigManager().getConfig().getInt("regeneration.blocks-per-tick", 50);

        // Start a task to regenerate the region
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    // Get locations from the region
                    Location min = region.getMinPoint();
                    Location max = region.getMaxPoint();

                    // Create a WorldEdit session
                    try (EditSession editSession = WorldEdit.getInstance().newEditSession(new BukkitWorld(world))) {
                        // Create a cuboid region
                        CuboidRegion cuboidRegion = new CuboidRegion(
                                BukkitAdapter.adapt(world),
                                BlockVector3.at(min.getX(), min.getY(), min.getZ()),
                                BlockVector3.at(max.getX(), max.getY(), max.getZ())
                        );

                        // Regenerate the region using WorldEdit's built-in functionality
                        // Calculate chunk coordinates
                        int minChunkX = (int)min.getX() >> 4;
                        int minChunkZ = (int)min.getZ() >> 4;
                        int maxChunkX = (int)max.getX() >> 4;
                        int maxChunkZ = (int)max.getZ() >> 4;

                        // Regenerate chunks one by one
                        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
                            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                                world.regenerateChunk(cx, cz);
                            }
                        }

                        Operations.complete(editSession.commit());

                        // Record the regeneration
                        region.recordRegeneration();

                        // Save the updated region data
                        if (plugin.getConfigManager().getConfig().getBoolean("storage.save-on-modify", true)) {
                            saveRegions();
                        }

                        // Log completion
                        MessageUtils.log("§aCompleted regeneration of region: " + region.getId());
                        if (initiator != null && initiator.isOnline()) {
                            MessageUtils.sendMessage(initiator, "§aCompleted regeneration of region: §e" + region.getId());
                        }

                        regenerationInProgress.put(region.getId(), false);
                        future.complete(true);
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Error regenerating region: " + region.getId(), e);
                    if (initiator != null && initiator.isOnline()) {
                        MessageUtils.sendMessage(initiator, "§cError regenerating region: §e" + region.getId());
                    }
                    regenerationInProgress.put(region.getId(), false);
                    future.complete(false);
                }
            }
        }.runTaskAsynchronously(plugin);

        return future;
    }

    /**
     * Notify players near a region that it's being regenerated
     *
     * @param region The region being regenerated
     */
    private void notifyNearbyPlayers(com.minecraft.regionregen.models.Region region) {
        World world = Bukkit.getWorld(region.getWorldName());
        if (world == null) return;

        int radius = plugin.getConfigManager().getConfig().getInt("regeneration.notification-radius", 100);
        Location center = region.getMinPoint().clone().add(
                (region.getMaxPoint().getX() - region.getMinPoint().getX()) / 2,
                (region.getMaxPoint().getY() - region.getMinPoint().getY()) / 2,
                (region.getMaxPoint().getZ() - region.getMinPoint().getZ()) / 2
        );

        for (Player player : world.getPlayers()) {
            if (player.getLocation().distance(center) <= radius) {
                MessageUtils.sendMessage(player, "§eA nearby region (§6" + region.getId() + "§e) is being regenerated!");
            }
        }
    }
}