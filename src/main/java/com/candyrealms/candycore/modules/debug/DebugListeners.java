package com.candyrealms.candycore.modules.debug;


import com.candyrealms.candycore.AnubisCore;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.text.DecimalFormat;

public class DebugListeners implements Listener {

    private final DebugModule module;

    public DebugListeners(AnubisCore plugin) {
        module = plugin.getModuleManager().getDebugModule();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageByEntityEvent event) {
        if(!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) return;

        if(!module.hasDebug(event.getDamager().getUniqueId())) return;

        Player attacker = (Player) event.getDamager();
        Player defender = (Player) event.getEntity();

        DecimalFormat formatter = new DecimalFormat("#.##");

        String healthBefore = formatter.format(defender.getHealth());
        String healthAfter = formatter.format(defender.getHealth() - event.getFinalDamage());

        double heartsDealt = defender.getHealth() - (defender.getHealth() - event.getFinalDamage());

        String formattedHeartsDealt = formatter.format(heartsDealt);

        module.sendDamageDebugMSG(attacker, healthBefore, healthAfter, formattedHeartsDealt);
    }


}
