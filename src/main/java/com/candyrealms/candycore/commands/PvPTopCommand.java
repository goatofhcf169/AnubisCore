package com.candyrealms.candycore.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.candyrealms.candycore.AnubisCore;
import com.candyrealms.candycore.modules.pvptop.PvPTopModule;
import com.candyrealms.candycore.utils.ColorUtil;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("pvptop")
public class PvPTopCommand extends BaseCommand {

    private final AnubisCore plugin;

    public PvPTopCommand(AnubisCore plugin) {
        this.plugin = plugin;
    }

    @Default
    @Description("Open the PvP Top GUI")
    public void onOpen(Player sender) {
        PvPTopModule module = plugin.getModuleManager().getPvPTopModule();
        if (module == null) {
            sender.sendMessage(ColorUtil.color(plugin.getConfigManager().getPrefix() + "&cPvP Top module is not available."));
            return;
        }
        module.openTopGui(sender);
    }

    @Subcommand("addkoth")
    @CommandPermission("anubiscore.admin")
    @Syntax("<factionIdOrTag> [amount]")
    public void onAddKoth(CommandSender sender, String factionIdOrTag, @Default("1") int amount) {
        PvPTopModule module = plugin.getModuleManager().getPvPTopModule();
        if (module == null) return;
        String id = resolveFactionId(factionIdOrTag);
        if (id == null) {
            sender.sendMessage(ColorUtil.color(plugin.getConfigManager().getPrefix() + "&cFaction not found."));
            return;
        }
        module.addKoTHCapture(id, amount);
        sender.sendMessage(ColorUtil.color(plugin.getConfigManager().getPrefix() + "&aAdded &f" + amount + " &aKoTH capture(s) to faction &f" + factionIdOrTag + "&a."));
    }

    @Subcommand("adddtc")
    @CommandPermission("anubiscore.admin")
    @Syntax("<factionIdOrTag> [amount]")
    public void onAddDtc(CommandSender sender, String factionIdOrTag, @Default("1") int amount) {
        PvPTopModule module = plugin.getModuleManager().getPvPTopModule();
        if (module == null) return;
        String id = resolveFactionId(factionIdOrTag);
        if (id == null) {
            sender.sendMessage(ColorUtil.color(plugin.getConfigManager().getPrefix() + "&cFaction not found."));
            return;
        }
        module.addDTCWin(id, amount);
        sender.sendMessage(ColorUtil.color(plugin.getConfigManager().getPrefix() + "&aAdded &f" + amount + " &aDTC win(s) to faction &f" + factionIdOrTag + "&a."));
    }

    @Subcommand("addkill")
    @CommandPermission("anubiscore.admin")
    @Syntax("<factionIdOrTag> [amount]")
    public void onAddKill(CommandSender sender, String factionIdOrTag, @Default("1") int amount) {
        PvPTopModule module = plugin.getModuleManager().getPvPTopModule();
        if (module == null) return;
        String id = resolveFactionId(factionIdOrTag);
        if (id == null) {
            sender.sendMessage(ColorUtil.color(plugin.getConfigManager().getPrefix() + "&cFaction not found."));
            return;
        }
        module.addKoTHCapture(id, 0); // ensure section exists
        // add kills
        int current = plugin.getPvPTopCFG().getConfig().getInt("data.factions." + id + ".kills", 0);
        plugin.getPvPTopCFG().getConfig().set("data.factions." + id + ".kills", Math.max(0, current + amount));
        plugin.getPvPTopCFG().saveConfig();
        sender.sendMessage(ColorUtil.color(plugin.getConfigManager().getPrefix() + "&aAdded &f" + amount + " &akill(s) to faction &f" + factionIdOrTag + "&a."));
    }

    // Removal commands
    @Subcommand("removekill")
    @CommandPermission("anubiscore.admin")
    @Syntax("<factionIdOrTag> [amount]")
    public void onRemoveKill(CommandSender sender, String factionIdOrTag, @Default("1") int amount) {
        PvPTopModule module = plugin.getModuleManager().getPvPTopModule();
        if (module == null) return;
        String id = resolveFactionId(factionIdOrTag);
        if (id == null) {
            sender.sendMessage(ColorUtil.color(plugin.getConfigManager().getPrefix() + "&cFaction not found."));
            return;
        }
        int current = plugin.getPvPTopCFG().getConfig().getInt("data.factions." + id + ".kills", 0);
        int newVal = Math.max(0, current - Math.max(0, amount));
        plugin.getPvPTopCFG().getConfig().set("data.factions." + id + ".kills", newVal);
        plugin.getPvPTopCFG().saveConfig();
        sender.sendMessage(ColorUtil.color(plugin.getConfigManager().getPrefix() + "&eRemoved &f" + amount + " &eKill(s) from faction &f" + factionIdOrTag + "&e. New kills: &f" + newVal));
    }

    @Subcommand("removekoth")
    @CommandPermission("anubiscore.admin")
    @Syntax("<factionIdOrTag> [amount]")
    public void onRemoveKoth(CommandSender sender, String factionIdOrTag, @Default("1") int amount) {
        PvPTopModule module = plugin.getModuleManager().getPvPTopModule();
        if (module == null) return;
        String id = resolveFactionId(factionIdOrTag);
        if (id == null) {
            sender.sendMessage(ColorUtil.color(plugin.getConfigManager().getPrefix() + "&cFaction not found."));
            return;
        }
        int current = plugin.getPvPTopCFG().getConfig().getInt("data.factions." + id + ".koths", 0);
        int newVal = Math.max(0, current - Math.max(0, amount));
        plugin.getPvPTopCFG().getConfig().set("data.factions." + id + ".koths", newVal);
        plugin.getPvPTopCFG().saveConfig();
        sender.sendMessage(ColorUtil.color(plugin.getConfigManager().getPrefix() + "&eRemoved &f" + amount + " &eKoTH capture(s) from faction &f" + factionIdOrTag + "&e. New KoTHs: &f" + newVal));
    }

    @Subcommand("removedtc")
    @CommandPermission("anubiscore.admin")
    @Syntax("<factionIdOrTag> [amount]")
    public void onRemoveDtc(CommandSender sender, String factionIdOrTag, @Default("1") int amount) {
        PvPTopModule module = plugin.getModuleManager().getPvPTopModule();
        if (module == null) return;
        String id = resolveFactionId(factionIdOrTag);
        if (id == null) {
            sender.sendMessage(ColorUtil.color(plugin.getConfigManager().getPrefix() + "&cFaction not found."));
            return;
        }
        int current = plugin.getPvPTopCFG().getConfig().getInt("data.factions." + id + ".dtc", 0);
        int newVal = Math.max(0, current - Math.max(0, amount));
        plugin.getPvPTopCFG().getConfig().set("data.factions." + id + ".dtc", newVal);
        plugin.getPvPTopCFG().saveConfig();
        sender.sendMessage(ColorUtil.color(plugin.getConfigManager().getPrefix() + "&eRemoved &f" + amount + " &eDTC win(s) from faction &f" + factionIdOrTag + "&e. New DTCs: &f" + newVal));
    }

    @Subcommand("resetall")
    @CommandPermission("anubiscore.admin")
    @Description("Reset all PvPTop data")
    public void onResetAll(CommandSender sender) {
        PvPTopModule module = plugin.getModuleManager().getPvPTopModule();
        if (module == null) return;
        module.resetAllData();
        sender.sendMessage(ColorUtil.color(plugin.getConfigManager().getPrefix() + "&aPvPTop data has been reset."));
    }

    private String resolveFactionId(String input) {
        try {
            // Try as ID
            Faction byId = Factions.getInstance().getFactionById(input);
            if (byId != null) return byId.getId();
        } catch (Throwable ignored) {}
        try {
            // Try by tag (iterate)
            for (Faction f : Factions.getInstance().getAllFactions()) {
                try {
                    String tag = f.getTag();
                    if (tag != null && tag.equalsIgnoreCase(input)) {
                        return f.getId();
                    }
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
        return null;
    }
}
