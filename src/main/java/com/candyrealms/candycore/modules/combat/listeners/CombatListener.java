package com.candyrealms.candycore.modules.combat.listeners;

import com.candyrealms.candycore.AnubisCore;
import com.candyrealms.candycore.modules.combat.CombatModule;
import com.candyrealms.candycore.modules.combat.PrinterAbuseManager;
import com.candyrealms.candycore.modules.combat.events.CombatTagExpireEvent;
import com.candyrealms.candycore.utils.ColorUtil;
import com.golfing8.kore.event.PlayerPrinterEnterEvent;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import net.minelink.ctplus.event.PlayerCombatTagEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class CombatListener implements Listener {

    private final PrinterAbuseManager printerAbuseManager;

    private final CombatModule combatModule;

    public CombatListener(AnubisCore plugin) {
        combatModule = plugin.getModuleManager().getCombatModule();
        printerAbuseManager = combatModule.getPrinterAbuseManager();
    }

    @EventHandler
    public void onCombat(PlayerCombatTagEvent event) {
        combatModule.addTaggedPlayers(event.getVictim(), event.getAttacker());
    }

    @EventHandler
    public void onExpire(CombatTagExpireEvent event) {
        printerAbuseManager.addCooldownPlayer(event.getPlayer());
    }

    @EventHandler
    public void onPrinter(PlayerPrinterEnterEvent event) {
        if(event.getPlayer().hasPermission("anubiscore.admin")) return;

        if(!printerAbuseManager.hasCooldown(event.getPlayer())) return;

        boolean nearbyEnemy = false;

        for(Entity entity : event.getPlayer().getNearbyEntities(20, 5, 20)) {
            if(!(entity instanceof Player)) continue;
            Player nearbyPlayer = (Player) entity;

            FPlayer nearbyFPlayer = FPlayers.getInstance().getByPlayer(nearbyPlayer);

            if(!nearbyFPlayer.hasFaction()) continue;
            if(nearbyPlayer.hasPermission("anubiscore.admin")) continue;

            nearbyEnemy = true;
        }

        if(!nearbyEnemy) return;

        event.getPlayer().sendMessage(ColorUtil.color("&d&lAnubis&5&lCore &fYou &c&nCANNOT&f enter printer quickly after leaving combat!"));
        event.setCancelled(true);
    }
}
