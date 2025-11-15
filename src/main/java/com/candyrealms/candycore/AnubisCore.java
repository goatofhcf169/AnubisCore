package com.candyrealms.candycore;

import co.aikar.commands.PaperCommandManager;
import com.candyrealms.candycore.commands.*;
import com.candyrealms.candycore.configuration.*;
import com.candyrealms.candycore.listeners.BalanceListeners;
import com.candyrealms.candycore.listeners.FactionListeners;
import com.candyrealms.candycore.listeners.MiscListeners;
import com.candyrealms.candycore.listeners.DeathMessagesListener;
import com.candyrealms.candycore.modules.ModuleManager;
import com.candyrealms.candycore.modules.combat.listeners.CombatListener;
import com.candyrealms.candycore.modules.debug.DebugListeners;
import com.candyrealms.candycore.modules.heads.listeners.HeadsListener;
import com.candyrealms.candycore.modules.shards.listeners.ShardsListener;
import com.candyrealms.candycore.utils.ColorUtil;
import com.earth2me.essentials.Essentials;
import lombok.Getter;
import net.minelink.ctplus.CombatTagPlus;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class AnubisCore extends JavaPlugin {

    private static AnubisCore instance;

    private ShardsCFG shardsCFG;
    private HeadsCFG headsCFG;
    private MasksCFG masksCFG;
    private ExpCFG expCFG;
    private DeathMessagesCFG deathMessagesCFG;
    private PvPTopCFG pvPTopCFG;

    private ConfigManager configManager;

    private ModuleManager moduleManager;

    private CombatTagPlus combatTagPlus;

    private Essentials essentials;


    @Override
    public void onEnable() {
        // Plugin startup logic
        Bukkit.getConsoleSender().sendMessage(ColorUtil.color("&4[&cAnubisCore&4] &fEnabling..."));
        saveDefaultConfig();
        shardsCFG = new ShardsCFG(this);
        headsCFG = new HeadsCFG(this);
        expCFG = new ExpCFG(this);
        masksCFG = new MasksCFG(this);
        deathMessagesCFG = new DeathMessagesCFG(this);
        pvPTopCFG = new PvPTopCFG(this);


        // Register Dependencies
        registerCombatTag();
        registerEssentials();

        // Registering managers
        configManager = new ConfigManager(this);
        moduleManager = new ModuleManager(this);

        // Registering the listeners
        Bukkit.getPluginManager().registerEvents(new DebugListeners(this), this);
        Bukkit.getPluginManager().registerEvents(new MiscListeners(this), this);
        if (getCombatTagPlus() != null && moduleManager.getCombatModule() != null) {
            Bukkit.getPluginManager().registerEvents(new CombatListener(this), this);
        }
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            Bukkit.getPluginManager().registerEvents(new BalanceListeners(this), this);
            try {
                new com.candyrealms.candycore.placeholders.FactionsExpansion(this).register();
                new com.candyrealms.candycore.placeholders.AnubisExpansion(this).register();
                Bukkit.getConsoleSender().sendMessage(ColorUtil.color("&4[&cAnubisCore&4] &fRegistered %factions_grace_time% and %anubis_grace_time% placeholders."));
            } catch (Throwable t) {
                Bukkit.getConsoleSender().sendMessage(ColorUtil.color("&4[&cAnubisCore&4] &cFailed to register grace placeholders: " + t.getClass().getSimpleName())) ;
            }
        }
        if (Bukkit.getPluginManager().getPlugin("FactionsKore") != null && Bukkit.getPluginManager().getPlugin("Factions") != null) {
            Bukkit.getPluginManager().registerEvents(new FactionListeners(), this);
        }
        Bukkit.getPluginManager().registerEvents(new ShardsListener(), this);
        // PvP Top: track kills (requires Factions)
        if (Bukkit.getPluginManager().getPlugin("Factions") != null) {
            Bukkit.getPluginManager().registerEvents(new com.candyrealms.candycore.modules.pvptop.PvPTopListener(this), this);
        }
        // PvP Top: KoTH capture via FactionsKore
        if (Bukkit.getPluginManager().getPlugin("FactionsKore") != null) {
            Bukkit.getPluginManager().registerEvents(new com.candyrealms.candycore.modules.pvptop.PvPTopKothListener(this), this);
        }
        // PvP Top: DestroyTheCore auto-detection
        if (Bukkit.getPluginManager().getPlugin("DestroyTheCore") != null) {
            Bukkit.getPluginManager().registerEvents(new com.candyrealms.candycore.modules.pvptop.PvPTopDTCListener(this), this);
        }
        // Revive module stores death records and listens to PlayerDeathEvent
        Bukkit.getPluginManager().registerEvents(moduleManager.getReviveModule(), this);
        // Chat moderation listener
        Bukkit.getPluginManager().registerEvents(moduleManager.getChatModerationModule(), this);
        // Block risky reload commands to prevent stale instances in other plugins
        Bukkit.getPluginManager().registerEvents(moduleManager.getReloadGuardModule(), this);
        // Dragon event listener
        Bukkit.getPluginManager().registerEvents(moduleManager.getDragonModule(), this);
        // GrindMobs event listener
        Bukkit.getPluginManager().registerEvents(moduleManager.getGrindMobsModule(), this);
        // Coins booster listener
        registerModules();

        // Registering the commands
        PaperCommandManager manager = new PaperCommandManager(this);
        manager.getCommandCompletions().registerAsyncCompletion("shards", c -> shardsCFG.getConfig().getConfigurationSection("shards").getKeys(false));
        manager.registerCommand(new CoreCommand(this));
        manager.registerCommand(new ExpShopCommand(this));
        manager.registerCommand(new ShardsCommand(this));
        manager.registerCommand(new ReviveCommand(this));
        manager.registerCommand(new MuteChatCommand(this));
        manager.registerCommand(new LockChatCommand(this));
        manager.registerCommand(new SlowChatCommand(this));
        manager.registerCommand(new ClearChatCommand(this));
        manager.registerCommand(new PvPTopCommand(this));
        manager.registerCommand(new DragonCommand(this));
        manager.registerCommand(new GrindMobsCommand(this));

        instance = this;
        Bukkit.getConsoleSender().sendMessage(ColorUtil.color("&4[&cAnubisCore&4] &aEnabled."));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        // No-op
    }

    public AnubisCore getInst() {
        return instance;
    }

    private void registerCombatTag() {
        if(Bukkit.getPluginManager().getPlugin("CombatTagPlus") == null) return;

        combatTagPlus = (CombatTagPlus) Bukkit.getPluginManager().getPlugin("CombatTagPlus");
        Bukkit.getConsoleSender().sendMessage(ColorUtil.color("&4[&cAnubisCore&4] &fEnabling CombatTag Support!"));
    }

    private void registerEssentials() {
        if(Bukkit.getPluginManager().getPlugin("Essentials") == null) return;

        essentials = (Essentials) Bukkit.getPluginManager().getPlugin("Essentials");
        Bukkit.getConsoleSender().sendMessage(ColorUtil.color("&4[&cAnubisCore&4] &fEnabling Essentials Support!"));
    }

    private void registerModules() {
        if(headsCFG.getConfig().getBoolean("enabled") && getEssentials() != null) {
            Bukkit.getPluginManager().registerEvents(new HeadsListener(this), this);
        }
        Bukkit.getPluginManager().registerEvents(new DeathMessagesListener(this), this);
    }
}
