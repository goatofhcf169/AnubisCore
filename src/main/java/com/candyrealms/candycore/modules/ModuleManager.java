package com.candyrealms.candycore.modules;

import com.candyrealms.candycore.CandyCore;
import com.candyrealms.candycore.modules.combat.CombatModule;
import com.candyrealms.candycore.modules.debug.DebugModule;
import com.candyrealms.candycore.modules.donator.DonatorModule;
import com.candyrealms.candycore.modules.expshop.ExpShopModule;
import com.candyrealms.candycore.modules.heads.HeadsModule;
import com.candyrealms.candycore.modules.shards.ShardsModule;
import com.candyrealms.candycore.modules.staff.StaffModule;
import lombok.Getter;

@Getter
public class ModuleManager {

    private final DebugModule debugModule;
    private final StaffModule staffModule;
    private final CombatModule combatModule;
    private final ExpShopModule expShopModule;
    private final ShardsModule shardsModule;
    private final DonatorModule donatorModule;
    private final HeadsModule headsModule;

    public ModuleManager(CandyCore plugin) {
        staffModule = new StaffModule(plugin);
        combatModule = new CombatModule(plugin);
        expShopModule = new ExpShopModule(plugin);
        shardsModule = new ShardsModule(plugin);
        donatorModule = new DonatorModule(plugin);
        headsModule = new HeadsModule(plugin);
        debugModule = new DebugModule();
    }
}
