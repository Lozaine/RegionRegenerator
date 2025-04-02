package com.minecraft.regionregen.commands;

import com.minecraft.regionregen.RegionRegenPlugin;
import com.minecraft.regionregen.gui.RegionGUI;
import com.minecraft.regionregen.models.Region;
import com.minecraft.regionregen.utils.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RegionCommand implements CommandExecutor, TabCompleter {

    private final RegionRegenPlugin plugin;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public RegionCommand(RegionRegenPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player) {
                // Open GUI for player
                Player player = (Player) sender;
                RegionGUI.openMainGUI(player);
                return true;
            } else {
                // Show help for console
                showHelp(sender);
                return true;
            }
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "create":
                return handleCreateCommand(sender, args);
            case "delete":
                return handleDeleteCommand(sender, args);
            case "info":
                return handleInfoCommand(sender, args);
            case "list":
                return handleListCommand(sender, args);
            case "regenerate":
                return handleRegenerateCommand(sender, args);
            case "help":
                showHelp(sender);
                return true;
            default:
                MessageUtils.sendMessage(sender, "§cUnknown subcommand. Type §e/" + label + " help §cfor help.");
                return true;
        }
    }

    private boolean handleCreateCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtils.sendMessage(sender, "§cThis command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 2) {
            MessageUtils.sendMessage(player, "§cUsage: /regionregen create <id>");
            return true;
        }

        String regionId = args[1];

        // Check if a region with this ID already exists
        if (plugin.getRegionManager().getRegion(regionId) != null) {
            MessageUtils.sendMessage(player, "§cA region with ID §e" + regionId + " §calready exists.");
            return true;
        }

        // Create the region from player's WorldEdit selection
        Region region = plugin.getRegionManager().createRegion(player, regionId);

        if (region == null) {
            MessageUtils.sendMessage(player, "§cFailed to create region. Make sure you have a valid WorldEdit selection.");
            return true;
        }

        MessageUtils.sendMessage(player, "§aRegion §e" + regionId + " §acreated successfully!");
        MessageUtils.sendMessage(player, "§aSize: §e" + region.getVolume() + " §ablocks");
        return true;
    }

    private boolean handleDeleteCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            MessageUtils.sendMessage(sender, "§cUsage: /regionregen delete <id>");
            return true;
        }

        String regionId = args[1];
        Region region = plugin.getRegionManager().getRegion(regionId);

        if (region == null) {
            MessageUtils.sendMessage(sender, "§cRegion §e" + regionId + " §cdoes not exist.");
            return true;
        }

        if (plugin.getRegionManager().isRegenerationInProgress(regionId)) {
            MessageUtils.sendMessage(sender, "§cCannot delete region §e" + regionId + " §cwhile regeneration is in progress.");
            return true;
        }

        boolean deleted = plugin.getRegionManager().deleteRegion(regionId);

        if (deleted) {
            MessageUtils.sendMessage(sender, "§aRegion §e" + regionId + " §adeleted successfully!");
        } else {
            MessageUtils.sendMessage(sender, "§cFailed to delete region §e" + regionId + "§c.");
        }

        return true;
    }

    private boolean handleInfoCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            MessageUtils.sendMessage(sender, "§cUsage: /regionregen info <id>");
            return true;
        }

        String regionId = args[1];
        Region region = plugin.getRegionManager().getRegion(regionId);

        if (region == null) {
            MessageUtils.sendMessage(sender, "§cRegion §e" + regionId + " §cdoes not exist.");
            return true;
        }

        MessageUtils.sendMessage(sender, "§8=== §eRegion: §6" + region.getId() + " §8===");
        MessageUtils.sendMessage(sender, "§7World: §f" + region.getWorldName());
        MessageUtils.sendMessage(sender, "§7Creator: §f" + region.getCreatorName());
        MessageUtils.sendMessage(sender, "§7Created: §f" + dateFormat.format(region.getCreationDate()));
        MessageUtils.sendMessage(sender, "§7Volume: §f" + region.getVolume() + " blocks");
        MessageUtils.sendMessage(sender, "§7Regeneration count: §f" + region.getRegenerationCount());

        if (region.getLastRegenerationDate() != null) {
            MessageUtils.sendMessage(sender, "§7Last regenerated: §f" + dateFormat.format(region.getLastRegenerationDate()));
        } else {
            MessageUtils.sendMessage(sender, "§7Last regenerated: §fNever");
        }

        if (plugin.getRegionManager().isRegenerationInProgress(region.getId())) {
            MessageUtils.sendMessage(sender, "§cRegeneration in progress!");
        }

        return true;
    }

    private boolean handleListCommand(CommandSender sender, String[] args) {
        List<Region> regions = new ArrayList<>(plugin.getRegionManager().getAllRegions());

        if (regions.isEmpty()) {
            MessageUtils.sendMessage(sender, "§cNo regions defined yet.");
            return true;
        }

        MessageUtils.sendMessage(sender, "§8=== §eRegions §8(§6" + regions.size() + "§8) ===");

        for (Region region : regions) {
            String statusIndicator = plugin.getRegionManager().isRegenerationInProgress(region.getId()) ? " §c[REGENERATING]" : "";
            MessageUtils.sendMessage(sender, "§6" + region.getId() + " §7- §f" + region.getWorldName() +
                    " §7(§f" + region.getVolume() + " blocks§7)" + statusIndicator);
        }

        return true;
    }

    private boolean handleRegenerateCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            MessageUtils.sendMessage(sender, "§cUsage: /regionregen regenerate <id>");
            return true;
        }

        String regionId = args[1];
        Region region = plugin.getRegionManager().getRegion(regionId);

        if (region == null) {
            MessageUtils.sendMessage(sender, "§cRegion §e" + regionId + " §cdoes not exist.");
            return true;
        }

        if (plugin.getRegionManager().isRegenerationInProgress(regionId)) {
            MessageUtils.sendMessage(sender, "§cRegeneration of region §e" + regionId + " §cis already in progress.");
            return true;
        }

        MessageUtils.sendMessage(sender, "§aStarting regeneration of region §e" + regionId + "§a...");

        Player player = (sender instanceof Player) ? (Player) sender : null;
        plugin.getRegionManager().regenerateRegion(region, player);

        return true;
    }

    private void showHelp(CommandSender sender) {
        MessageUtils.sendMessage(sender, "§8=== §eRegionRegen Help §8===");
        MessageUtils.sendMessage(sender, "§6/regionregen §7- Open the region management GUI");
        MessageUtils.sendMessage(sender, "§6/regionregen create <id> §7- Create a new region");
        MessageUtils.sendMessage(sender, "§6/regionregen delete <id> §7- Delete a region");
        MessageUtils.sendMessage(sender, "§6/regionregen info <id> §7- Show info about a region");
        MessageUtils.sendMessage(sender, "§6/regionregen list §7- List all regions");
        MessageUtils.sendMessage(sender, "§6/regionregen regenerate <id> §7- Regenerate a region");
        MessageUtils.sendMessage(sender, "§6/regionregen help §7- Show this help message");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("create", "delete", "info", "list", "regenerate", "help")
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("delete") || subCommand.equals("info") || subCommand.equals("regenerate")) {
                return plugin.getRegionManager().getAllRegions()
                        .stream()
                        .map(Region::getId)
                        .filter(s -> s.startsWith(args[1]))
                        .collect(Collectors.toList());
            }
        }

        return new ArrayList<>();
    }
}