package com.candyrealms.candycore.listeners;

import com.candyrealms.candycore.CandyCore;
import com.candyrealms.candycore.configuration.ConfigManager;
import com.golfing8.kore.event.StackedEntityDeathEvent;
import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MiscListeners implements Listener {

    private final CandyCore plugin;

    public MiscListeners(CandyCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if(event.getPlayer().getItemInHand() == null || event.getPlayer().getItemInHand().getType() == Material.AIR) return;

        Player player = event.getPlayer();

        ItemStack itemStack = player.getItemInHand();

        NBTItem nbtItem = new NBTItem(itemStack);

        if(!nbtItem.hasTag("candycorehelp")) return;

        player.chat("/help");
    }

    /**
     * Fixes players taking way less damage when blocking.
     */
    @EventHandler
    public void onBlockDamage(EntityDamageByEntityEvent event) {
        if(!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) return;

        Player defender = (Player) event.getEntity();

        if(!defender.isBlocking()) return;

        FileConfiguration config = plugin.getConfig();

        double damageIncrease = config.getDouble("increase-blocking-damage");

        double finalDamage = event.getDamage() * damageIncrease;

        event.setDamage(finalDamage);
    }

    /**
     * Nerf's protection 5 for balancing reasons
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if(!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player)) return;

        Player defender = (Player) event.getEntity();

        FileConfiguration config = plugin.getConfig();

        if(!config.getBoolean("protection-nerf.enabled")) return;

        double nerfPerPiece = config.getDouble("protection-nerf.nerf-per-piece");

        double finalNerfAmount = 0.0;

        List<ItemStack> itemList = Arrays.stream(defender.getInventory().getArmorContents())
                .filter(item -> (item.containsEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL)
                && item.getEnchantmentLevel(Enchantment.PROTECTION_ENVIRONMENTAL) >= 5))
                .collect(Collectors.toList());

        finalNerfAmount = itemList.size() * nerfPerPiece;

        double multiplier = 1 + (finalNerfAmount / 100);

        event.setDamage(event.getDamage() * multiplier);
    }

    /**
     * Buffs sharpness 6
     */
    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if(!(event.getDamager() instanceof Player)) return;

        FileConfiguration config = plugin.getConfig();

        if(!config.getBoolean("sharpness-buff.enabled")) return;

        Player attacker = (Player) event.getDamager();

        if(!attacker.getItemInHand().containsEnchantment(Enchantment.DAMAGE_ALL)) return;

        if(attacker.getItemInHand().getEnchantmentLevel(Enchantment.DAMAGE_ALL) != 6) return;

        double multiplier = 1 + (config.getDouble("sharpness-buff.amount")/100);

        event.setDamage(event.getDamage() * multiplier);
    }

    /**
     * Nerfs the resistance gained from god apples.
     * Contains safety checks to prevent removing naturally
     * gained resistance.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGappleUsage(PlayerItemConsumeEvent event) {
        if(event.isCancelled()) return;
        if(!plugin.getConfigManager().isNerfApples()) return;

        ItemStack item = event.getItem();
        if (item.getType() != Material.GOLDEN_APPLE || item.getDurability() != 1) return;

        Player player = event.getPlayer();

        PotionEffect activeResistance = player.getActivePotionEffects().stream()
                .filter(pe -> pe.getType().getName().equalsIgnoreCase("DAMAGE_RESISTANCE"))
                .findFirst()
                .orElse(null);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            ConfigManager config = plugin.getConfigManager();

            int resistanceTime = config.getResistanceTime();

            if(activeResistance != null && activeResistance.getAmplifier() > 0) return;

            player.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);

            int currentTime = activeResistance != null ? activeResistance.getDuration() / 20 : 0;

            if(currentTime > resistanceTime && currentTime > 0) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, activeResistance.getDuration(), 0));
                return;
            }

            if(resistanceTime <= 0) return;

            player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * resistanceTime, 0));
        }, 10L);
    }
}
