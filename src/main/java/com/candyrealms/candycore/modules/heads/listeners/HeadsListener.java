package com.candyrealms.candycore.modules.heads.listeners;

import com.candyrealms.candycore.CandyCore;
import com.candyrealms.candycore.configuration.ConfigManager;
import com.candyrealms.candycore.modules.heads.HeadsModule;
import com.candyrealms.candycore.utils.ColorUtil;
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

    public HeadsListener(CandyCore plugin) {
        module = plugin.getModuleManager().getHeadsModule();
        configManager = plugin.getConfigManager();
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if(event.getEntity() == null) return;
        if(event.getEntity().getKiller() == null) return;
        if(event.getEntity().hasPermission(configManager.getHeadBypassPerm())) return;

        event.getDrops().add(module.getPlayerHead(event.getEntity()));
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
        long shards = compound.getLong("shards");
        long crystals = compound.getLong("crystals");
        double tokens = compound.getDouble("tokens");

        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.US);
        formatter.setMinimumFractionDigits(2);
        formatter.setMaximumFractionDigits(2);

        String formattedMoney = Economy.format(new BigDecimal(money));
        String formattedCrystals = formatter.format(crystals);
        String formattedShards = formatter.format(shards);
        String formattedTokens = formatter.format(tokens);

        player.setItemInHand(null);

        module.addShards(player, shards);
        module.addCrystals(player, crystals);
        module.addMoney(player, money);
        module.addTokens(player, tokens);

        List<String> message = module.getConfig().getStringList("messages.redeemed-head").stream()
                .map(s -> s.replace("%money%", formattedMoney))
                .map(s -> s.replace("%tokens%", formattedTokens))
                .map(s -> s.replace("%shards%", formattedShards))
                .map(s -> s.replace("%crystals%", formattedCrystals))
                .map(ColorUtil::color)
                .collect(Collectors.toList());

        message.forEach(player::sendMessage);

        player.playSound(player.getLocation(), Sound.ORB_PICKUP, 7, 6);

        event.setCancelled(true);
    }
}
