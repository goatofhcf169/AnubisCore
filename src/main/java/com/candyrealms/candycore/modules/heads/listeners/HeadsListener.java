package com.candyrealms.candycore.modules.heads.listeners;

import com.candyrealms.candycore.AnubisCore;
import com.candyrealms.candycore.configuration.ConfigManager;
import com.candyrealms.candycore.modules.heads.HeadsModule;
import com.candyrealms.candycore.utils.ColorUtil;
import com.candyrealms.candycore.utils.CompatUtil;
import com.earth2me.essentials.api.Economy;
import de.tr7zw.changeme.nbtapi.NBTCompound;
import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class HeadsListener implements Listener {

    private final HeadsModule module;

    private final ConfigManager configManager;

    public HeadsListener(AnubisCore plugin) {
        module = plugin.getModuleManager().getHeadsModule();
        configManager = plugin.getConfigManager();
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if(event.getEntity() == null) return;
        if(event.getEntity().getKiller() == null) return;
        if(event.getEntity().hasPermission(configManager.getHeadBypassPerm())) return;

        boolean debug = module.getConfig().getBoolean("debug-vulcan-tokens", false);
        if (debug) {
            try {
                org.bukkit.Bukkit.getConsoleSender().sendMessage("[AnubisCore-Heads] PlayerDeathEvent victim='" + event.getEntity().getName() + "' killer='" + (event.getEntity().getKiller() != null ? event.getEntity().getKiller().getName() : "null") + "'");
            } catch (Throwable ignored) {}
        }

        ItemStack head = module.getPlayerHead(event.getEntity());
        event.getDrops().add(head);
        if (debug) {
            try {
                de.tr7zw.changeme.nbtapi.NBTItem n = new de.tr7zw.changeme.nbtapi.NBTItem(head);
                de.tr7zw.changeme.nbtapi.NBTCompound c = n.getCompound("CandyHeads");
                long money = c != null ? c.getLong("money") : 0L;
                double tokens = c != null ? c.getDouble("tokens") : 0D;
                org.bukkit.Bukkit.getConsoleSender().sendMessage("[AnubisCore-Heads] Dropped head NBT -> money=" + money + ", tokens=" + tokens);
            } catch (Throwable ignored) {}
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlace(BlockPlaceEvent event) {
        if(event.getPlayer().getItemInHand() == null
                || event.getPlayer().getItemInHand().getType() == Material.AIR) return;

        ItemStack itemStack = event.getPlayer().getItemInHand();

        NBTItem nbtItem = new NBTItem(itemStack);

        if(nbtItem.getCompound("CandyHeads") == null) return;

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if(event.getPlayer().getItemInHand() == null
                || event.getPlayer().getItemInHand().getType() == Material.AIR) return;

        if(event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack itemStack = event.getPlayer().getItemInHand();

        NBTItem nbtItem = new NBTItem(itemStack);

        if(nbtItem.getCompound("CandyHeads") == null) return;

        Player player = event.getPlayer();

        NBTCompound compound = nbtItem.getCompound("CandyHeads");

        long money = compound.getLong("money");
        double tokens = compound.getDouble("tokens");

        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.US);
        formatter.setMinimumFractionDigits(2);
        formatter.setMaximumFractionDigits(2);

        String formattedMoney = Economy.format(new BigDecimal(money));
        String formattedTokens = formatter.format(tokens);

        player.setItemInHand(null);

        module.addMoney(player, money);
        module.addTokens(player, tokens);

        try {
            if (module.getConfig().getBoolean("debug-vulcan-tokens", false)) {
                module.getClass(); // keep module ref used
                player.getServer().getLogger().info("[Heads] Redeemed head for " + player.getName() + ": money=" + money + ", tokens=" + tokens);
            }
        } catch (Throwable ignored) {}

        List<String> message = module.getConfig().getStringList("messages.redeemed-head").stream()
                .map(s -> s.replace("%money%", formattedMoney))
                .map(s -> s.replace("%tokens%", formattedTokens))
                .map(s -> s.replace("%upgradetools_tokens%", formattedTokens))
                .map(s -> s.replace("%mantichoes_tokens%", formattedTokens))
                .map(ColorUtil::color)
                .collect(Collectors.toList());

        message.forEach(player::sendMessage);

        CompatUtil.play(player, 7f, 6f, "ORB_PICKUP", "ENTITY_EXPERIENCE_ORB_PICKUP");

        event.setCancelled(true);
    }
}
