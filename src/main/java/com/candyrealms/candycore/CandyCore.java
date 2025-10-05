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
import com.candyrealms.candycore.modules.staff.listeners.ChatListeners;
import com.candyrealms.candycore.modules.elitearmor.ArmorListeners;
import com.candyrealms.candycore.modules.staff.listeners.StaffListeners;
import com.candyrealms.candycore.utils.ColorUtil;
import com.earth2me.essentials.Essentials;
import lombok.Getter;
import net.minelink.ctplus.CombatTagPlus;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class CandyCore extends JavaPlugin {

    private static CandyCore instance;

    private ShardsCFG shardsCFG;
    private StaffCFG staffCFG;
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
        staffCFG = new StaffCFG(this);
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
        Bukkit.getPluginManager().registerEvents(new ChatListeners(this), this);
        Bukkit.getPluginManager().registerEvents(new DebugListeners(this), this);
        Bukkit.getPluginManager().registerEvents(new MiscListeners(this), this);
        Bukkit.getPluginManager().registerEvents(new CombatListener(this), this);
        Bukkit.getPluginManager().registerEvents(new BalanceListeners(this), this);
        Bukkit.getPluginManager().registerEvents(new StaffListeners(this), this);
        Bukkit.getPluginManager().registerEvents(new SafeDZListeners(this), this);
        Bukkit.getPluginManager().registerEvents(new FactionListeners(), this);
        Bukkit.getPluginManager().registerEvents(new ShardsListener(), this);
        Bukkit.getPluginManager().registerEvents(new ArmorListeners(), this);
        registerModules();

        // Registering the commands
        PaperCommandManager manager = new PaperCommandManager(this);
        manager.getCommandCompletions().registerAsyncCompletion("shards", c -> shardsCFG.getConfig().getConfigurationSection("shards").getKeys(false));
        manager.registerCommand(new CoreCommand(this));
        manager.registerCommand(new ChatCommand(this));
        manager.registerCommand(new ExpShopCommand(this));
        manager.registerCommand(new ShardsCommand(this));
        manager.registerCommand(new DonateCommand(this));
        manager.registerCommand(new StaffCommand(this));

        instance = this;
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getModuleManager().getStaffModule().getStaffModeModule().onRestart();
    }

    public CandyCore getInst() {
        return instance;
    }

    private void registerCombatTag() {
        if(Bukkit.getPluginManager().getPlugin("CombatTagPlus") == null) return;

        combatTagPlus = (CombatTagPlus) Bukkit.getPluginManager().getPlugin("CombatTagPlus");
        Bukkit.getConsoleSender().sendMessage(ColorUtil.color("&4[&cAscoraCore&4] &fEnabling CombatTag Support!"));
    }

    private void registerEssentials() {
        if(Bukkit.getPluginManager().getPlugin("Essentials") == null) return;

        essentials = (Essentials) Bukkit.getPluginManager().getPlugin("Essentials");
        Bukkit.getConsoleSender().sendMessage(ColorUtil.color("&4[&cAscoraCore&4] &fEnabling Essentials Support!"));
    }

    private void registerModules() {
        if(headsCFG.getConfig().getBoolean("enabled")) {
            Bukkit.getPluginManager().registerEvents(new HeadsListener(this), this);
        }
    }
}
