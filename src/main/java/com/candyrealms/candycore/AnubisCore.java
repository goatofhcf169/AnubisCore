package com.candyrealms.candycore;

import co.aikar.commands.PaperCommandManager;
import com.candyrealms.candycore.commands.*;
import com.candyrealms.candycore.configuration.*;
import com.candyrealms.candycore.listeners.BalanceListeners;
import com.candyrealms.candycore.listeners.FactionListeners;
import com.candyrealms.candycore.listeners.MiscListeners;
import com.candyrealms.candycore.listeners.SafeDZListeners;
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
    private DonatorCFG donatorCFG;
    private HeadsCFG headsCFG;
    private MasksCFG masksCFG;
    private ExpCFG expCFG;

    private ConfigManager configManager;

    private ModuleManager moduleManager;

    private CombatTagPlus combatTagPlus;

    private Essentials essentials;


    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();
        shardsCFG = new ShardsCFG(this);
        donatorCFG = new DonatorCFG(this);
        headsCFG = new HeadsCFG(this);
        expCFG = new ExpCFG(this);
        masksCFG = new MasksCFG(this);


        // Register Dependencies
        registerCombatTag();
        registerEssentials();

        // Registering managers
        configManager = new ConfigManager(this);
        moduleManager = new ModuleManager(this);

        // Registering the listeners
        Bukkit.getPluginManager().registerEvents(new DebugListeners(this), this);
        Bukkit.getPluginManager().registerEvents(new MiscListeners(this), this);
        Bukkit.getPluginManager().registerEvents(new CombatListener(this), this);
        Bukkit.getPluginManager().registerEvents(new BalanceListeners(this), this);
        Bukkit.getPluginManager().registerEvents(new SafeDZListeners(this), this);
        Bukkit.getPluginManager().registerEvents(new FactionListeners(), this);
        Bukkit.getPluginManager().registerEvents(new ShardsListener(), this);
        // Revive module stores death records and listens to PlayerDeathEvent
        Bukkit.getPluginManager().registerEvents(moduleManager.getReviveModule(), this);
        // Chat moderation listener
        Bukkit.getPluginManager().registerEvents(moduleManager.getChatModerationModule(), this);
        // Block risky reload commands to prevent stale instances in other plugins
        Bukkit.getPluginManager().registerEvents(moduleManager.getReloadGuardModule(), this);
        registerModules();

        // Registering the commands
        PaperCommandManager manager = new PaperCommandManager(this);
        manager.getCommandCompletions().registerAsyncCompletion("shards", c -> shardsCFG.getConfig().getConfigurationSection("shards").getKeys(false));
        manager.registerCommand(new CoreCommand(this));
        manager.registerCommand(new ExpShopCommand(this));
        manager.registerCommand(new ShardsCommand(this));
        manager.registerCommand(new DonateCommand(this));
        manager.registerCommand(new ReviveCommand(this));
        manager.registerCommand(new MuteChatCommand(this));
        manager.registerCommand(new LockChatCommand(this));
        manager.registerCommand(new SlowChatCommand(this));

        instance = this;
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
        if(headsCFG.getConfig().getBoolean("enabled")) {
            Bukkit.getPluginManager().registerEvents(new HeadsListener(this), this);
        }
    }
}
