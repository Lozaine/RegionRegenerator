package com.minecraft.regionregen.utils;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.SessionManager;
import org.bukkit.entity.Player;

/**
 * Utility class for WorldEdit integration
 */
public class WorldEditUtils {

    /**
     * Get a player's current WorldEdit selection
     *
     * @param player The player to get the selection for
     * @return The WorldEdit region, or null if no selection/error
     */
    public static Region getPlayerSelection(Player player) {
        try {
            // Convert Bukkit Player to WorldEdit BukkitPlayer
            BukkitPlayer bukkitPlayer = BukkitAdapter.adapt(player);

            // Get the player's session
            SessionManager sessionManager = WorldEdit.getInstance().getSessionManager();
            LocalSession session = sessionManager.get(bukkitPlayer);

            // Get the selection
            return session.getSelection(bukkitPlayer.getWorld());
        } catch (IncompleteRegionException e) {
            // Player doesn't have a complete selection
            MessageUtils.sendMessage(player, "§cYou don't have a complete WorldEdit selection.");
            return null;
        } catch (Exception e) {
            // Other exceptions
            MessageUtils.sendMessage(player, "§cError getting your WorldEdit selection: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Check if a player has a valid WorldEdit selection
     *
     * @param player The player to check
     * @return True if the player has a valid selection
     */
    public static boolean hasValidSelection(Player player) {
        return getPlayerSelection(player) != null;
    }

    /**
     * Calculate the volume of a WorldEdit region
     *
     * @param region The region to calculate volume for
     * @return The volume (number of blocks) in the region
     */
    public static long calculateVolume(Region region) {
        if (region == null) {
            return 0;
        }

        return region.getVolume();
    }
}