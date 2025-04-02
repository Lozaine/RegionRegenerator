package com.minecraft.regionregen.utils;

import com.minecraft.regionregen.RegionRegenPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

/**
 * Manages plugin configuration
 */
public class ConfigManager {

    private final RegionRegenPlugin plugin;
    private FileConfiguration config;

    /**
     * Create a new ConfigManager
     *
     * @param plugin The plugin instance
     */
    public ConfigManager(RegionRegenPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Load the configuration
     */
    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        // Create directory for data files if it doesn't exist
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        // Check if auto-backup is enabled
        if (config.getBoolean("storage.auto-backup", true)) {
            setupAutoBackup();
        }
    }

    /**
     * Get the configuration
     *
     * @return The configuration
     */
    public FileConfiguration getConfig() {
        return config;
    }

    /**
     * Reload the configuration
     */
    public void reloadConfig() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    /**
     * Save the configuration
     */
    public void saveConfig() {
        plugin.saveConfig();
    }

    /**
     * Set up automatic backups of the regions file
     */
    private void setupAutoBackup() {
        long intervalTicks = config.getLong("storage.backup-interval", 60) * 20 * 60; // Convert minutes to ticks

        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            File regionsFile = new File(plugin.getDataFolder(), "regions.yml");
            if (!regionsFile.exists()) {
                return;
            }

            File backupDir = new File(plugin.getDataFolder(), "backups");
            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }

            // Create backup file with timestamp
            String timestamp = String.valueOf(System.currentTimeMillis());
            File backupFile = new File(backupDir, "regions_" + timestamp + ".yml");

            try {
                // Copy regions file to backup
                java.nio.file.Files.copy(regionsFile.toPath(), backupFile.toPath());
                MessageUtils.debug("Created backup of regions file: " + backupFile.getName());

                // Clean up old backups (keep 10 most recent)
                File[] backups = backupDir.listFiles((dir, name) -> name.startsWith("regions_") && name.endsWith(".yml"));
                if (backups != null && backups.length > 10) {
                    // Sort files by name (timestamp)
                    java.util.Arrays.sort(backups);

                    // Delete oldest files (keeping 10 newest)
                    for (int i = 0; i < backups.length - 10; i++) {
                        if (backups[i].delete()) {
                            MessageUtils.debug("Deleted old backup: " + backups[i].getName());
                        }
                    }
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to create backup of regions file", e);
            }
        }, intervalTicks, intervalTicks);
    }
}