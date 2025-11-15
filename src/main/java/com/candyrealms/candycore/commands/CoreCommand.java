package com.candyrealms.candycore.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import com.candyrealms.candycore.AnubisCore;
import com.candyrealms.candycore.configuration.ConfigManager;
import com.candyrealms.candycore.modules.debug.DebugModule;
import com.candyrealms.candycore.modules.heads.HeadsModule;
import com.candyrealms.candycore.utils.ColorUtil;
import com.candyrealms.candycore.utils.CompatUtil;
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
        plugin.getHeadsCFG().reloadConfig();
        plugin.getDeathMessagesCFG().reloadConfig();
        // Reload PvPTop with data preservation (PvPTopCFG merges data on reload)
        plugin.getPvPTopCFG().reloadConfig();

        // Stop GrindMobs if running, then reload modules and configs
        if (plugin.getModuleManager().getGrindMobsModule() != null && plugin.getModuleManager().getGrindMobsModule().isActive()) {
            plugin.getModuleManager().getGrindMobsModule().stop();
        }

        plugin.getModuleManager().getShardsModule().reload();
        plugin.getModuleManager().getHeadsModule().reloadConfig();
        plugin.getModuleManager().getExpShopModule().reload();
        if (plugin.getModuleManager().getPvPTopModule() != null) {
            plugin.getModuleManager().getPvPTopModule().reload();
        }
        // Dragon event
        if (plugin.getModuleManager().getDragonModule() != null) {
            plugin.getModuleManager().getDragonModule().reload();
        }
        // GrindMobs event
        if (plugin.getModuleManager().getGrindMobsModule() != null) {
            plugin.getModuleManager().getGrindMobsModule().reload();
        }

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
            CompatUtil.play((Player) sender, 7f, 7f, "LEVEL_UP", "ENTITY_PLAYER_LEVELUP");
            return;
        }

        module.removeDebugPlayer(sender.getUniqueId());
        sender.sendMessage(ColorUtil.color(configManager.getPrefix() + "&fYou have toggled the debug mode &c&noff&f."));
        CompatUtil.play((Player) sender, 7f, 7f, "NOTE_BASS_DRUM", "BLOCK_NOTE_BLOCK_BASEDRUM");
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
    public void onHelp(CommandSender sender) {
        boolean isPlayer = sender instanceof Player;
        boolean isAdmin = sender.hasPermission("anubiscore.admin");

        sender.sendMessage(ColorUtil.color("&6&l&m----------------------------------------"));
        sender.sendMessage(ColorUtil.color("&6&l AnubisCore &eCommands"));
        sender.sendMessage(ColorUtil.color("&6&l&m----------------------------------------"));

        // Player commands
        sender.sendMessage(ColorUtil.color("&6Player:"));
        sender.sendMessage(ColorUtil.color(" &6/pvptop &eOpen PvP Top"));
        sender.sendMessage(ColorUtil.color(" &6/expshop &eOpen XP Shop"));
        sender.sendMessage(ColorUtil.color(" &6/pots &eGet healing potions"));
        sender.sendMessage(ColorUtil.color(" &6/refill &eRefill empty slots with potions"));

        // Chat moderation (only show if sender can use any of them)
        boolean canMute = sender.hasPermission("anubiscore.mutechat");
        boolean canLock = sender.hasPermission("anubiscore.lockchat");
        boolean canSlow = sender.hasPermission("anubiscore.slowchat");
        boolean canClear = sender.hasPermission("anubiscore.chatclear");
        if (canMute || canLock || canSlow || canClear) {
            sender.sendMessage(ColorUtil.color("&6Chat:"));
            if (canMute)  sender.sendMessage(ColorUtil.color(" &6/mutechat &eToggle global chat mute"));
            if (canLock)  sender.sendMessage(ColorUtil.color(" &6/lockchat &eToggle chat lock"));
            if (canSlow)  sender.sendMessage(ColorUtil.color(" &6/slowchat &e<duration|off> Set slow mode"));
            if (canClear) sender.sendMessage(ColorUtil.color(" &6/clearchat &eClear chat"));
        }

        // Admin commands
        if (isAdmin) {
            sender.sendMessage(ColorUtil.color("&6Admin:"));
            sender.sendMessage(ColorUtil.color(" &6/anubiscore reload &eReload configuration"));
            sender.sendMessage(ColorUtil.color(" &6/anubiscore debug &eToggle damage debug"));
            sender.sendMessage(ColorUtil.color(" &6/givehead &e<player> Give head item"));
            sender.sendMessage(ColorUtil.color(" &6/anubiscore givehelp &e<player> Give help item"));
            sender.sendMessage(ColorUtil.color(" &6/shards give &e<player> <shard> Give a shard"));
            sender.sendMessage(ColorUtil.color(" &6/revive &e<player> Open revive menu"));
            // Dragon event admin
            sender.sendMessage(ColorUtil.color(" &6/dragon spawn &eSpawn event dragon at configured location"));
            sender.sendMessage(ColorUtil.color(" &6/dragon despawn &eForce-despawn the event dragon"));

            // PvPTop admin
            sender.sendMessage(ColorUtil.color(" &6/pvptop addkill &e<factionIdOrTag> [amount]"));
            sender.sendMessage(ColorUtil.color(" &6/pvptop removekill &e<factionIdOrTag> [amount]"));
            sender.sendMessage(ColorUtil.color(" &6/pvptop addkoth &e<factionIdOrTag> [amount]"));
            sender.sendMessage(ColorUtil.color(" &6/pvptop removekoth &e<factionIdOrTag> [amount]"));
            sender.sendMessage(ColorUtil.color(" &6/pvptop adddtc &e<factionIdOrTag> [amount]"));
            sender.sendMessage(ColorUtil.color(" &6/pvptop removedtc &e<factionIdOrTag> [amount]"));
            sender.sendMessage(ColorUtil.color(" &6/pvptop resetall &eReset PvPTop data"));
        }

        sender.sendMessage(ColorUtil.color("&6&l&m----------------------------------------"));

        if (isPlayer) {
            Player player = (Player) sender;
            CompatUtil.play(player, 7f, 6f, "NOTE_PIANO", "BLOCK_NOTE_BLOCK_HARP");
        }
    }
}
