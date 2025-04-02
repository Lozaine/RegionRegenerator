package com.minecraft.regionregen;

import com.minecraft.regionregen.commands.RegionCommand;
import com.minecraft.regionregen.managers.RegionManager;
import com.minecraft.regionregen.utils.ConfigManager;
import com.minecraft.regionregen.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class RegionRegenPlugin extends JavaPlugin {

    private static RegionRegenPlugin instance;
    private ConfigManager configManager;
    private RegionManager regionManager;

    @Override
    public void onEnable() {
        instance = this;

        // Initialize managers
        this.configManager = new ConfigManager(this);
        this.regionManager = new RegionManager(this);

        // Load configuration
        configManager.loadConfig();

        // Load regions
        regionManager.loadRegions();

        // Register commands
        getCommand("regionregen").setExecutor(new RegionCommand(this));

        // Log plugin startup
        getLogger().info("RegionRegen plugin has been enabled!");

        // Check for WorldEdit dependency
        if (Bukkit.getPluginManager().getPlugin("WorldEdit") == null) {
            getLogger().log(Level.SEVERE, "WorldEdit not found! This plugin requires WorldEdit to function properly.");
            getLogger().log(Level.SEVERE, "Disabling RegionRegen...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Setup metrics if needed

        MessageUtils.log("&aRegionRegen v" + getDescription().getVersion() + " has been enabled!");
    }

    @Override
    public void onDisable() {
        // Save any pending data
        if (regionManager != null) {
            regionManager.saveRegions();
        }

        MessageUtils.log("&cRegionRegen has been disabled!");
        instance = null;
    }

    /**
     * Get the plugin instance
     * @return The plugin instance
     */
    public static RegionRegenPlugin getInstance() {
        return instance;
    }

    /**
     * Get the config manager
     * @return The config manager
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }

    /**
     * Get the region manager
     * @return The region manager
     */
    public RegionManager getRegionManager() {
        return regionManager;
    }
}