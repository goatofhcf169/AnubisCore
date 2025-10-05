package com.candyrealms.candycore.modules;

import com.candyrealms.candycore.AnubisCore;
import com.candyrealms.candycore.modules.combat.CombatModule;
import com.candyrealms.candycore.modules.debug.DebugModule;
import com.candyrealms.candycore.modules.donator.DonatorModule;
import com.candyrealms.candycore.modules.expshop.ExpShopModule;
import com.candyrealms.candycore.modules.heads.HeadsModule;
import com.candyrealms.candycore.modules.shards.ShardsModule;
import com.candyrealms.candycore.modules.revive.ReviveModule;
import com.candyrealms.candycore.modules.chat.ChatModerationModule;
import com.candyrealms.candycore.modules.safety.ReloadGuardModule;
import lombok.Getter;

@Getter
public class ModuleManager {

    private final DebugModule debugModule;
    private final CombatModule combatModule;
    private final ExpShopModule expShopModule;
    private final ShardsModule shardsModule;
    private final DonatorModule donatorModule;
    private final HeadsModule headsModule;
    private final ReviveModule reviveModule;
    private final ChatModerationModule chatModerationModule;
    private final ReloadGuardModule reloadGuardModule;

    public ModuleManager(AnubisCore plugin) {
        combatModule = new CombatModule(plugin);
        expShopModule = new ExpShopModule(plugin);
        shardsModule = new ShardsModule(plugin);
        donatorModule = new DonatorModule(plugin);
        headsModule = new HeadsModule(plugin);
        reviveModule = new ReviveModule(plugin);
        chatModerationModule = new ChatModerationModule(plugin);
        reloadGuardModule = new ReloadGuardModule(plugin);
        debugModule = new DebugModule();
    }
}
