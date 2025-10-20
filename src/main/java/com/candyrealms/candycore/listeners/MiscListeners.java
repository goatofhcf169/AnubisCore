package com.candyrealms.candycore.listeners;

import com.candyrealms.candycore.AnubisCore;
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
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Projectile;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MiscListeners implements Listener {

    private final AnubisCore plugin;

    public MiscListeners(AnubisCore plugin) {
        this.plugin = plugin;

        // Periodically remove orphaned fishing hooks to prevent NPEs in NMS tick
        // Run more frequently to catch invalid hooks quickly
        new BukkitRunnable() {
            @Override
            public void run() {
                cleanupOrphanFishingHooks();
            }
        }.runTaskTimer(plugin, 20L, 20L);

        // Initial sweep on enable to clear any stale hooks from prior sessions
        Bukkit.getScheduler().runTask(plugin, this::cleanupOrphanFishingHooks);

        // Aggressive guard for world "staffseries" where the issue occurs: every tick remove null-owner hooks
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    org.bukkit.World w = Bukkit.getWorld("staffseries");
                    if (w == null) return;
                    for (Entity e : w.getEntities()) {
                        if (e.getType() != EntityType.FISHING_HOOK) continue;
                        if (hasNullNmsOwner(e)) {
                            e.remove();
                        }
                    }
                } catch (Throwable ignored) { }
            }
        }.runTaskTimer(plugin, 1L, 1L);
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

        // Use a fixed multiplier for blocking damage instead of config
        double damageIncrease = 1.25D; // 25% more damage while blocking

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

    // Earliest guard: catch hook launch before NMS ticks
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        try {
            if (event.getEntity().getType() != EntityType.FISHING_HOOK) return;
            Projectile proj = event.getEntity();
            Entity raw = proj;
            ProjectileSource src = proj.getShooter();
            if (!(src instanceof Player) || hasNullNmsOwner(raw)) { raw.remove(); return; }
            Player p = (Player) src;
            if (!p.isOnline() || !p.isValid() || p.isDead() || p.getWorld() == null || !p.getWorld().equals(raw.getWorld())) {
                raw.remove();
                return;
            }
            // Recheck one tick later to catch mid-tick state changes
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    if (!raw.isValid()) return;
                    ProjectileSource s2 = proj.getShooter();
                    if (!(s2 instanceof Player) || hasNullNmsOwner(raw)) { raw.remove(); return; }
                    Player p2 = (Player) s2;
                    if (!p2.isOnline() || !p2.isValid() || p2.isDead() || p2.getWorld() == null || !p2.getWorld().equals(raw.getWorld())) {
                        raw.remove();
                    }
                } catch (Throwable ignored) { }
            });
        } catch (Throwable ignored) { }
    }

    // Defensive: if a hook is spawned with an invalid shooter, remove it immediately
    @EventHandler(ignoreCancelled = false)
    public void onPlayerFish(PlayerFishEvent event) {
        try {
            // Work with hook as generic Entity/Projectile to support API variance
            Entity raw = event.getHook();
            if (raw == null) return;
            Projectile proj = (raw instanceof Projectile) ? (Projectile) raw : null;
            if (proj == null) { raw.remove(); return; }
            // If another plugin cancels the cast after the hook spawned, remove safely
            if (event.isCancelled()) { raw.remove(); return; }
            ProjectileSource src = proj.getShooter();
            if (!(src instanceof Player)) { raw.remove(); return; }
            Player shooter = (Player) src;
            Player player = event.getPlayer();
            if (!player.getUniqueId().equals(shooter.getUniqueId())) { raw.remove(); return; }
            if (!player.isOnline() || !player.isValid() || player.isDead() || player.getWorld() == null || !player.getWorld().equals(raw.getWorld()) || hasNullNmsOwner(raw)) {
                raw.remove();
            }
            // Recheck one tick later to catch post-event teleports/cancels
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    if (!raw.isValid()) return;
                    ProjectileSource s2 = proj.getShooter();
                    if (!(s2 instanceof Player) || hasNullNmsOwner(raw)) { raw.remove(); return; }
                    Player p2 = (Player) s2;
                    if (!p2.isOnline() || !p2.isValid() || p2.isDead() || p2.getWorld() == null || !p2.getWorld().equals(raw.getWorld())) {
                        raw.remove();
                    }
                } catch (Throwable ignored) { }
            });
        } catch (Throwable ignored) { }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cleanupHooksFor(event.getPlayer());
    }

    @EventHandler
    public void onKick(PlayerKickEvent event) {
        cleanupHooksFor(event.getPlayer());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        cleanupHooksFor(event.getPlayer());
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        cleanupHooksFor(event.getPlayer());
    }

    // Remove fish hooks when a player dies to avoid dangling owner references
    @org.bukkit.event.EventHandler
    public void onDeath(org.bukkit.event.entity.PlayerDeathEvent event) {
        cleanupHooksFor(event.getEntity());
    }

    // Sweep newly loaded chunks for orphan hooks which can cause NMS tick NPEs
    @org.bukkit.event.EventHandler
    public void onChunkLoad(org.bukkit.event.world.ChunkLoadEvent event) {
        try {
            for (org.bukkit.entity.Entity entity : event.getChunk().getEntities()) {
                if (entity.getType() != EntityType.FISHING_HOOK) continue;
                if (!(entity instanceof Projectile)) { entity.remove(); continue; }
                Projectile proj = (Projectile) entity;
                ProjectileSource src = proj.getShooter();
                if (src == null || hasNullNmsOwner(entity)) {
                    entity.remove();
                    continue;
                }
                if (!(src instanceof Player)) { entity.remove(); continue; }
                Player p = (Player) src;
                if (!p.isOnline() || !p.isValid() || p.isDead() || p.getWorld() == null || !p.getWorld().equals(entity.getWorld())) {
                    entity.remove();
                }
            }
        } catch (Throwable ignored) { }
    }

    private void cleanupHooksFor(Player player) {
        try {
            for (org.bukkit.entity.Entity entity : player.getWorld().getEntities()) {
                if (entity.getType() != EntityType.FISHING_HOOK) continue;
                if (!(entity instanceof Projectile)) { entity.remove(); continue; }
                Projectile proj = (Projectile) entity;
                ProjectileSource src = proj.getShooter();
                if (src == null || hasNullNmsOwner(entity)) {
                    entity.remove();
                    continue;
                }
                // Only players are valid shooters for FishHook in 1.8; anything else is unsafe
                if (!(src instanceof Player)) { entity.remove(); continue; }
                if (((Player) src).getUniqueId().equals(player.getUniqueId())) {
                    entity.remove();
                }
            }
        } catch (Throwable ignored) { }
    }

    private void cleanupOrphanFishingHooks() {
        try {
            for (org.bukkit.World world : Bukkit.getWorlds()) {
                for (org.bukkit.entity.Entity entity : world.getEntities()) {
                    if (entity.getType() != EntityType.FISHING_HOOK) continue;
                    if (!(entity instanceof Projectile)) { entity.remove(); continue; }
                    Projectile proj = (Projectile) entity;
                    ProjectileSource src = proj.getShooter();
                    if (src == null || hasNullNmsOwner(entity)) {
                        entity.remove();
                        continue;
                    }
                    // Only allow online, valid players in the same world
                    if (!(src instanceof Player)) { entity.remove(); continue; }
                    Player p = (Player) src;
                    if (!p.isOnline() || !p.isValid() || p.isDead() || p.getWorld() == null || !p.getWorld().equals(world)) {
                        entity.remove();
                        continue;
                    }
                }
            }
        } catch (Throwable ignored) { }
    }

    // NMS owner check: remove if underlying EntityFishingHook has null owner/angler
    private boolean hasNullNmsOwner(Entity hook) {
        try {
            Object craft = hook;
            java.lang.reflect.Method getHandle = craft.getClass().getMethod("getHandle");
            Object nms = getHandle.invoke(craft);
            if (nms == null) return true;
            Class<?> nmsClass = nms.getClass();
            for (java.lang.reflect.Field f : nmsClass.getDeclaredFields()) {
                Class<?> ft = f.getType();
                String name = ft.getName();
                if (name.endsWith(".EntityHuman") || name.endsWith(".EntityPlayer")) {
                    f.setAccessible(true);
                    Object owner = f.get(nms);
                    if (owner == null) return true;
                }
            }
        } catch (Throwable ignored) { }
        return false;
    }
}
