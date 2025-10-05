package com.candyrealms.candycore.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import com.candyrealms.candycore.CandyCore;
import com.candyrealms.candycore.configuration.ConfigManager;
import com.candyrealms.candycore.modules.debug.DebugModule;
import com.candyrealms.candycore.modules.heads.HeadsModule;
import com.candyrealms.candycore.utils.ColorUtil;
import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@CommandAlias("acore|core|ascoracore")
public class CoreCommand extends BaseCommand {

    private final CandyCore plugin;

    private final ConfigManager configManager;

    public CoreCommand(CandyCore plugin) {
        this.plugin = plugin;

        configManager = plugin.getConfigManager();
    }

    @Subcommand("reload")
    @CommandPermission("candycore.admin")
    @Description("reloads the configuration values")
    public void onReload(CommandSender sender) {
        configManager.reloadConfig();
        plugin.getExpCFG().reloadConfig();
        plugin.getShardsCFG().reloadConfig();
        plugin.getDonatorCFG().reloadConfig();
        plugin.getHeadsCFG().reloadConfig();

        plugin.getModuleManager().getDonatorModule().reload();
        plugin.getModuleManager().getShardsModule().reload();
        plugin.getModuleManager().getHeadsModule().reloadConfig();
        plugin.getModuleManager().getExpShopModule().reload();

        if(!(sender instanceof Player)) return;

        configManager.sendReloadMessage((Player) sender);
    }

    @Subcommand("givehead")
    @CommandPermission("candycore.admin")
    @CommandAlias("givehead")
    @CommandCompletion("@players")
    @Description("gives you a head item")
    @Syntax("<player>")
    public void onGiveHead(Player sender, OnlinePlayer receiver) {
        HeadsModule module = plugin.getModuleManager().getHeadsModule();

        sender.getInventory().addItem(module.getPlayerHead(receiver.getPlayer()));
    }

    @Subcommand("debug|damagedebug")
    @CommandPermission("candycore.admin")
    @Description("toggles the damage debug mode")
    public void onDebug(Player sender) {
        DebugModule module = plugin.getModuleManager().getDebugModule();

        if(!module.hasDebug(sender.getUniqueId())) {
            module.addDebugPlayer(sender.getUniqueId());
            sender.sendMessage(ColorUtil.color(configManager.getPrefix() + "&fYou have toggled the debug mode &a&non&f."));
            sender.playSound(sender.getLocation(), Sound.LEVEL_UP, 7, 7);
            return;
        }

        module.removeDebugPlayer(sender.getUniqueId());
        sender.sendMessage(ColorUtil.color(configManager.getPrefix() + "&fYou have toggled the debug mode &c&noff&f."));
        sender.playSound(sender.getLocation(), Sound.NOTE_BASS_DRUM, 7, 7);
    }

    @Subcommand("givehelp")
    @CommandPermission("candycore.admin")
    @CommandCompletion("@players")
    public void onGiveHelp(CommandSender sender, OnlinePlayer player) {
        ItemStack itemStack = plugin.getConfigManager().getHelpItem();
        NBTItem nbtItem = new NBTItem(itemStack);

        nbtItem.setString("candycorehelp", "help");

        player.getPlayer().getInventory().addItem(nbtItem.getItem());
    }

    @Default
    @Subcommand("help")
    @CommandPermission("candycore.admin")
    public void onHelp(CommandSender sender) {
        sender.sendMessage(" ");
        sender.sendMessage(ColorUtil.color("&d&lAscora&5&lCore &f- &eHelp Commands"));
        sender.sendMessage(" ");
        sender.sendMessage(ColorUtil.color("&d- /acore reload &f- &eReloads the configuration"));
        sender.sendMessage(ColorUtil.color("&d- /acore debug &f- &eEnables damage debug mode"));
        sender.sendMessage(ColorUtil.color("&d- /chat &f- &eShows the chat module commands"));
        sender.sendMessage(ColorUtil.color("&d- /staff &f- &eToggles the staff module"));
        sender.sendMessage(ColorUtil.color("&d- /expshop &f- &eOpens the exp shop menu"));
        sender.sendMessage(ColorUtil.color("&d- /giveshard &f- &eGives an enchantment shard"));
        sender.sendMessage(" ");

        if(!(sender instanceof Player)) return;

        Player player = (Player) sender;
        player.playSound(player.getLocation(), Sound.NOTE_PIANO, 7, 6);
    }
}
