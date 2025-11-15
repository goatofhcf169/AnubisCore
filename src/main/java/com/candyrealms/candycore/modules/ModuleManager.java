package com.candyrealms.candycore.modules;

import com.candyrealms.candycore.AnubisCore;
import com.candyrealms.candycore.modules.combat.CombatModule;
import com.candyrealms.candycore.modules.debug.DebugModule;
import com.candyrealms.candycore.modules.expshop.ExpShopModule;
import com.candyrealms.candycore.modules.heads.HeadsModule;
import com.candyrealms.candycore.modules.shards.ShardsModule;
import com.candyrealms.candycore.modules.revive.ReviveModule;
import com.candyrealms.candycore.modules.chat.ChatModerationModule;
import com.candyrealms.candycore.modules.safety.ReloadGuardModule;
import com.candyrealms.candycore.modules.pvptop.PvPTopModule;
import com.candyrealms.candycore.modules.dragon.DragonModule;
import com.candyrealms.candycore.modules.grindmobs.GrindMobsModule;
import lombok.Getter;

@Getter
public class ModuleManager {

    private final DebugModule debugModule;
    private final CombatModule combatModule;
    private final ExpShopModule expShopModule;
    private final ShardsModule shardsModule;
    private final HeadsModule headsModule;
    private final ReviveModule reviveModule;
    private final ChatModerationModule chatModerationModule;
    private final ReloadGuardModule reloadGuardModule;
    private final PvPTopModule pvPTopModule;
    private final DragonModule dragonModule;
    private final GrindMobsModule grindMobsModule;

    public ModuleManager(AnubisCore plugin) {
        combatModule = (plugin.getCombatTagPlus() != null) ? new CombatModule(plugin) : null;
        expShopModule = new ExpShopModule(plugin);
        shardsModule = new ShardsModule(plugin);
        headsModule = new HeadsModule(plugin);
        reviveModule = new ReviveModule(plugin);
        chatModerationModule = new ChatModerationModule(plugin);
        reloadGuardModule = new ReloadGuardModule(plugin);
        pvPTopModule = new PvPTopModule(plugin);
        dragonModule = new DragonModule(plugin);
        grindMobsModule = new GrindMobsModule(plugin);
        // Coins booster requires FactionsKore and at least one Mantic economy plugin
        boolean hasFactionsKore = org.bukkit.Bukkit.getPluginManager().getPlugin("FactionsKore") != null;
        boolean hasManticHoes = org.bukkit.Bukkit.getPluginManager().getPlugin("ManticHoes") != null;
        boolean hasManticRods = org.bukkit.Bukkit.getPluginManager().getPlugin("ManticRods") != null;
        boolean hasManticSwords = org.bukkit.Bukkit.getPluginManager().getPlugin("ManticSwords") != null;
        debugModule = new DebugModule();
    }
}
