package com.candyrealms.candycore.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import com.candyrealms.candycore.AnubisCore;
import com.candyrealms.candycore.configuration.ConfigManager;
import com.candyrealms.candycore.modules.debug.DebugModule;
import com.candyrealms.candycore.modules.heads.HeadsModule;
import com.candyrealms.candycore.utils.ColorUtil;
import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@CommandAlias("anubiscore|acore|core")
public class CoreCommand extends BaseCommand {

    private final AnubisCore plugin;

    private final ConfigManager configManager;

    public CoreCommand(AnubisCore plugin) {
        this.plugin = plugin;

        configManager = plugin.getConfigManager();
    }

    @Subcommand("reload")
    @CommandPermission("anubiscore.admin")
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
    @CommandPermission("anubiscore.admin")
    @CommandAlias("givehead")
    @CommandCompletion("@players")
    @Description("gives you a head item")
    @Syntax("<player>")
    public void onGiveHead(Player sender, OnlinePlayer receiver) {
        HeadsModule module = plugin.getModuleManager().getHeadsModule();

        sender.getInventory().addItem(module.getPlayerHead(receiver.getPlayer()));
    }

    @Subcommand("debug|damagedebug")
    @CommandPermission("anubiscore.admin")
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
    @CommandPermission("anubiscore.admin")
    @CommandCompletion("@players")
    public void onGiveHelp(CommandSender sender, OnlinePlayer player) {
        ItemStack itemStack = plugin.getConfigManager().getHelpItem();
        NBTItem nbtItem = new NBTItem(itemStack);

        nbtItem.setString("candycorehelp", "help");

        player.getPlayer().getInventory().addItem(nbtItem.getItem());
    }

    @Default
    @Subcommand("help")
    @CommandPermission("anubiscore.admin")
    public void onHelp(CommandSender sender) {
        sender.sendMessage(" ");
        sender.sendMessage(ColorUtil.color("&d&lAnubis&5&lCore &f- &eHelp Commands"));
        sender.sendMessage(" ");
        sender.sendMessage(ColorUtil.color("&d- /anubiscore reload &f- &eReloads the configuration"));
        sender.sendMessage(ColorUtil.color("&d- /anubiscore debug &f- &eEnables damage debug mode"));
        // Staff and Chat modules removed
        sender.sendMessage(ColorUtil.color("&d- /expshop &f- &eOpens the exp shop menu"));
        sender.sendMessage(ColorUtil.color("&d- /giveshard &f- &eGives an enchantment shard"));
        sender.sendMessage(" ");

        if(!(sender instanceof Player)) return;

        Player player = (Player) sender;
        player.playSound(player.getLocation(), Sound.NOTE_PIANO, 7, 6);
    }
}
