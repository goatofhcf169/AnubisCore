package com.candyrealms.candycore.modules.dragon;

import com.candyrealms.candycore.AnubisCore;
import com.candyrealms.candycore.configuration.DragonCFG;
import com.candyrealms.candycore.utils.ColorUtil;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.DecimalFormat;
import java.util.List;
import java.util.UUID;

/**
 * Dragon event module: spawns a configurable Ender Dragon in a specific world,
 * prevents block damage, broadcasts spawn/death messages, and optionally updates
 * the dragon name (bossbar title on 1.8) as health changes.
 */
public class DragonModule implements Listener {

    private static final String DRAGON_META = "anubis_dragon_event";

    private final AnubisCore plugin;

    @Getter
    private final DragonCFG dragonCFG;

    private FileConfiguration config;

    // Cached settings
    private String worldName;
    private double dragonHealth;
    private String bossbarTitle;
    private int bossbarUpdateTicks;
    private List<String> spawnBroadcast;
    private List<String> deathBroadcastLines; // optional list style
    private String deathBroadcast;            // fallback single-line style
    private String notInWorldMessage;
    private String alreadyActiveMessage;

    private UUID activeDragonId;
    private BukkitRunnable nameUpdater;
    private BukkitRunnable leashTask;

    // Spawn location and leash control
    private double spawnX, spawnY, spawnZ;
    private float spawnYaw, spawnPitch;
    private boolean leashEnabled;
    private double leashRadius;
    private double leashCenterX, leashCenterY, leashCenterZ;
    private int leashCheckTicks;

    // Altitude control
    private boolean altitudeEnabled;
    private double altitudeMaxY;
    private double altitudeMinY;
    private double altitudeDownStep;

    public DragonModule(AnubisCore plugin) {
        this.plugin = plugin;
        this.dragonCFG = new DragonCFG(plugin);
        this.config = dragonCFG.getConfig();
        cache();
    }

    public void reload() {
        dragonCFG.reloadConfig();
        config = dragonCFG.getConfig();
        cache();
        // Refresh updater name pattern if running
        if (nameUpdater != null) {
            nameUpdater.cancel();
            nameUpdater = null;
        }
        // Reattach updater if a dragon is active
        EnderDragon dragon = getActiveDragon();
        if (dragon != null) {
            startNameUpdater(dragon);
            updateDragonName(dragon);
        }
    }

    private void cache() {
        worldName = config.getString("world", "world_the_end");
        dragonHealth = config.getDouble("dragon.health", 500.0);
        bossbarTitle = config.getString("bossbar.title", "&5&lEvent Dragon");
        bossbarUpdateTicks = Math.max(0, config.getInt("bossbar.update-interval-ticks", 10));
        spawnBroadcast = config.getStringList("messages.spawn");
        // Death message supports list (messages.death or messages.death-banner) or single string
        if (config.isList("messages.death")) {
            deathBroadcastLines = config.getStringList("messages.death");
            deathBroadcast = null;
        } else if (config.isList("messages.death-banner")) {
            deathBroadcastLines = config.getStringList("messages.death-banner");
            deathBroadcast = null;
        } else {
            deathBroadcastLines = null;
            deathBroadcast = config.getString("messages.death", "&d%player% &7from &5%faction% &7slain the &5&lEvent Dragon&7!");
        }
        notInWorldMessage = config.getString("messages.not-in-world", "&cYou can only spawn the dragon in &e%world%&c.");
        alreadyActiveMessage = config.getString("messages.already-active", "&cA dragon event is already active.");

        // Spawn location
        spawnX = config.getDouble("spawn.x", 0.5);
        spawnY = config.getDouble("spawn.y", 100.0);
        spawnZ = config.getDouble("spawn.z", 0.5);
        spawnYaw = (float) config.getDouble("spawn.yaw", 0.0);
        spawnPitch = (float) config.getDouble("spawn.pitch", 0.0);

        // Leash
        leashEnabled = config.getBoolean("leash.enabled", true);
        leashRadius = Math.max(1.0, config.getDouble("leash.radius", 75.0));
        leashCenterX = config.getDouble("leash.center.x", spawnX);
        leashCenterY = config.getDouble("leash.center.y", spawnY);
        leashCenterZ = config.getDouble("leash.center.z", spawnZ);
        leashCheckTicks = Math.max(5, config.getInt("leash.check-interval-ticks", 20));

        // Altitude
        altitudeEnabled = config.getBoolean("altitude.enabled", true);
        altitudeMaxY = config.getDouble("altitude.max-y", spawnY + 15.0);
        altitudeMinY = config.getDouble("altitude.min-y", Math.max(5.0, spawnY - 10.0));
        altitudeDownStep = Math.max(0.5, config.getDouble("altitude.down-step", 2.0));
    }

    public boolean hasActiveDragon() {
        return getActiveDragon() != null;
    }

    public EnderDragon getActiveDragon() {
        // 1.8 compatibility: Bukkit#getEntity(UUID) does not exist.
        // Resolve by scanning the configured world for matching UUID first.
        if (activeDragonId != null) {
            World w = Bukkit.getWorld(worldName);
            if (w != null) {
                for (Entity e : w.getEntities()) {
                    if (e instanceof EnderDragon && e.isValid() && activeDragonId.equals(e.getUniqueId())) {
                        return (EnderDragon) e;
                    }
                }
            }
        }
        // Fallback: search by our metadata tag (handles reloads)
        EnderDragon byMeta = findDragonByMeta();
        if (byMeta != null) {
            activeDragonId = byMeta.getUniqueId();
            return byMeta;
        }
        activeDragonId = null;
        return null;
    }

    public void despawnActiveDragon() {
        EnderDragon dragon = getActiveDragon();
        if (dragon == null) {
            dragon = findDragonByMeta();
        }
        if (dragon != null) dragon.remove();
        activeDragonId = null;
        if (nameUpdater != null) {
            nameUpdater.cancel();
            nameUpdater = null;
        }
        if (leashTask != null) {
            leashTask.cancel();
            leashTask = null;
        }
    }

    public void spawnDragon(Player sender) {
        if (hasActiveDragon()) {
            sender.sendMessage(ColorUtil.color(alreadyActiveMessage));
            return;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            sender.sendMessage(ColorUtil.color("&cConfigured dragon world '&e" + worldName + "&c' not found."));
            return;
        }

        if (!sender.getWorld().getName().equalsIgnoreCase(worldName)) {
            sender.sendMessage(ColorUtil.color(notInWorldMessage.replace("%world%", worldName)));
            return;
        }

        Location spawnLoc = new Location(world, spawnX, spawnY, spawnZ, spawnYaw, spawnPitch);

        EnderDragon dragon = world.spawn(spawnLoc, EnderDragon.class);
        dragon.setMetadata(DRAGON_META, new FixedMetadataValue(plugin, true));

        try {
            dragon.setMaxHealth(dragonHealth);
        } catch (Throwable ignored) {}
        try {
            dragon.setHealth(Math.min(dragonHealth, dragon.getMaxHealth()));
        } catch (Throwable ignored) {}

        updateDragonName(dragon);
        dragon.setCustomNameVisible(true);

        activeDragonId = dragon.getUniqueId();
        startNameUpdater(dragon);
        startLeashTask(dragon);

        // Broadcast spawn messages
        if (spawnBroadcast != null && !spawnBroadcast.isEmpty()) {
            for (String line : spawnBroadcast) {
                Bukkit.broadcastMessage(com.candyrealms.candycore.utils.CenterChatUtil.center(line));
            }
        }
    }

    private void startNameUpdater(EnderDragon dragon) {
        if (bossbarUpdateTicks <= 0) return;
        nameUpdater = new BukkitRunnable() {
            @Override
            public void run() {
                EnderDragon d = getActiveDragon();
                if (d == null || d.isDead() || !d.isValid()) {
                    cancel();
                    return;
                }
                updateDragonName(d);
            }
        };
        nameUpdater.runTaskTimer(plugin, bossbarUpdateTicks, bossbarUpdateTicks);
    }

    private void updateDragonName(EnderDragon dragon) {
        double health = 0;
        double max = 0;
        try { health = dragon.getHealth(); } catch (Throwable ignored) {}
        try { max = dragon.getMaxHealth(); } catch (Throwable ignored) { max = dragonHealth; }
        double pct = (max > 0) ? (health / max) * 100.0 : 0;
        DecimalFormat df0 = new DecimalFormat("#");
        DecimalFormat df1 = new DecimalFormat("#.#");

        String title = bossbarTitle
                .replace("%health%", df0.format(health))
                .replace("%max%", df0.format(max))
                .replace("%percent%", df1.format(pct));

        dragon.setCustomName(ColorUtil.color(title));
    }

    private void startLeashTask(EnderDragon dragon) {
        if (!leashEnabled) return;
        if (leashTask != null) leashTask.cancel();
        final double radiusSq = leashRadius * leashRadius;
        final Location center = new Location(dragon.getWorld(), leashCenterX, leashCenterY, leashCenterZ);
        leashTask = new BukkitRunnable() {
            @Override
            public void run() {
                EnderDragon d = getActiveDragon();
                if (d == null || d.isDead() || !d.isValid()) {
                    cancel();
                    return;
                }
                if (!d.getWorld().getName().equalsIgnoreCase(worldName)) return;
                Location loc = d.getLocation();
                double dx = loc.getX() - center.getX();
                double dy = loc.getY() - center.getY();
                double dz = loc.getZ() - center.getZ();
                double distSq = dx*dx + dy*dy + dz*dz;
                if (distSq > radiusSq) {
                    double dist = Math.sqrt(distSq);
                    if (dist == 0) dist = 1;
                    double nx = dx / dist;
                    double ny = dy / dist;
                    double nz = dz / dist;
                    double clamp = Math.max(1.0, leashRadius - 2.0);
                    Location back = center.clone().add(nx * clamp, ny * clamp, nz * clamp);
                    back.setYaw(loc.getYaw());
                    back.setPitch(loc.getPitch());
                    d.teleport(back);
                }

                // Altitude enforcement: keep the dragon a bit lower than usual
                if (altitudeEnabled) {
                    double y = d.getLocation().getY();
                    if (y > altitudeMaxY) {
                        double newY = Math.max(altitudeMaxY - 1.0, y - altitudeDownStep);
                        Location down = d.getLocation().clone();
                        down.setY(newY);
                        d.teleport(down);
                    } else if (y < altitudeMinY) {
                        double newY = Math.min(altitudeMinY + 1.0, y + altitudeDownStep);
                        Location up = d.getLocation().clone();
                        up.setY(newY);
                        d.teleport(up);
                    }
                }
            }
        };
        leashTask.runTaskTimer(plugin, leashCheckTicks, leashCheckTicks);
    }

    @EventHandler
    public void onDragonExplode(EntityExplodeEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof EnderDragon)) return;
        if (!hasOurMeta(entity)) return;
        event.blockList().clear();
        event.setCancelled(true);
    }

    @EventHandler
    public void onDragonChangeBlock(EntityChangeBlockEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof EnderDragon)) return;
        if (!hasOurMeta(entity)) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onDragonDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof EnderDragon)) return;
        EnderDragon dragon = (EnderDragon) event.getEntity();
        if (!hasOurMeta(dragon)) return;

        // Stop updater
        if (nameUpdater != null) {
            nameUpdater.cancel();
            nameUpdater = null;
        }

        Player killer = resolveKiller(event);
        String playerName = killer != null ? killer.getName() : "Unknown";
        String factionName = "NoFaction";
        try {
            // MassiveCraft Factions (optional)
            if (killer != null && Bukkit.getPluginManager().getPlugin("Factions") != null) {
                com.massivecraft.factions.FPlayer fp = com.massivecraft.factions.FPlayers.getInstance().getByPlayer(killer);
                if (fp != null && fp.getFaction() != null) {
                    String tag = fp.getFaction().getTag();
                    if (tag != null && !tag.isEmpty()) factionName = tag;
                }
            }
        } catch (Throwable ignored) { }

        // Broadcast death message (list style preferred)
        if (deathBroadcastLines != null && !deathBroadcastLines.isEmpty()) {
            for (String line : deathBroadcastLines) {
                String formatted = line
                        .replace("%player%", playerName)
                        .replace("%faction%", factionName);
                Bukkit.broadcastMessage(com.candyrealms.candycore.utils.CenterChatUtil.center(formatted));
            }
        } else if (deathBroadcast != null && !deathBroadcast.isEmpty()) {
            String msg = ColorUtil.color(deathBroadcast
                    .replace("%player%", playerName)
                    .replace("%faction%", factionName));
            Bukkit.broadcastMessage(msg);
        }

        // Rewards
        try {
            if (config.getBoolean("rewards.enabled", true)) {
                List<String> killerCmds = config.getStringList("rewards.killer-commands");
                if (killer != null && killerCmds != null && !killerCmds.isEmpty()) {
                    dispatchCommandsForPlayer(killerCmds, killer, factionName);
                }

                // Optional faction rewards: run for each online member of killer's faction
                if (killer != null && config.getBoolean("rewards.faction.run-for-online-members", false)
                        && Bukkit.getPluginManager().getPlugin("Factions") != null) {
                    List<String> factionCmds = config.getStringList("rewards.faction.commands");
                    if (factionCmds != null && !factionCmds.isEmpty()) {
                        try {
                            com.massivecraft.factions.FPlayer fp = com.massivecraft.factions.FPlayers.getInstance().getByPlayer(killer);
                            if (fp != null && fp.getFaction() != null) {
                                for (Player member : fp.getFaction().getOnlinePlayers()) {
                                    dispatchCommandsForMember(factionCmds, killer, member, fp.getFaction().getTag());
                                }
                            }
                        } catch (Throwable ignored) { }
                    }
                }
            }
        } catch (Throwable ignored) { }

        // Clear active pointer
        activeDragonId = null;
        if (leashTask != null) {
            leashTask.cancel();
            leashTask = null;
        }
    }

    private boolean hasOurMeta(Entity entity) {
        if (entity == null) return false;
        if (!entity.hasMetadata(DRAGON_META)) return false;
        List<MetadataValue> list = entity.getMetadata(DRAGON_META);
        for (MetadataValue v : list) {
            if (v.getOwningPlugin() == plugin) return true;
        }
        return false;
    }

    private Player resolveKiller(EntityDeathEvent event) {
        if (event == null || !(event.getEntity() instanceof EnderDragon)) return null;
        EnderDragon ed = (EnderDragon) event.getEntity();
        try {
            Player p = ed.getKiller();
            if (p != null) return p;
        } catch (Throwable ignored) {}
        try {
            EntityDamageEvent last = ed.getLastDamageCause();
            if (last instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent ebe = (EntityDamageByEntityEvent) last;
                Entity damager = ebe.getDamager();
                if (damager instanceof Player) return (Player) damager;
                if (damager instanceof Projectile) {
                    org.bukkit.projectiles.ProjectileSource src = ((Projectile) damager).getShooter();
                    if (src instanceof Player) return (Player) src;
                }
                if (damager instanceof Tameable) {
                    AnimalTamer owner = ((Tameable) damager).getOwner();
                    if (owner instanceof Player) return (Player) owner;
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private void dispatchCommandsForPlayer(List<String> commands, Player target, String factionTag) {
        if (commands == null || commands.isEmpty() || target == null) return;
        String world = target.getWorld() != null ? target.getWorld().getName() : "world";
        String x = String.valueOf(target.getLocation().getBlockX());
        String y = String.valueOf(target.getLocation().getBlockY());
        String z = String.valueOf(target.getLocation().getBlockZ());
        String uuid = target.getUniqueId().toString();
        for (String raw : commands) {
            if (raw == null || raw.trim().isEmpty()) continue;
            String cmd = raw.startsWith("/") ? raw.substring(1) : raw;
            cmd = cmd.replace("%player%", target.getName())
                     .replace("%uuid%", uuid)
                     .replace("%faction%", factionTag != null ? factionTag : "NoFaction")
                     .replace("%world%", world)
                     .replace("%x%", x)
                     .replace("%y%", y)
                     .replace("%z%", z);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), ColorUtil.color(cmd));
        }
    }

    private void dispatchCommandsForMember(List<String> commands, Player killer, Player member, String factionTag) {
        if (commands == null || commands.isEmpty() || member == null) return;
        String world = member.getWorld() != null ? member.getWorld().getName() : "world";
        String x = String.valueOf(member.getLocation().getBlockX());
        String y = String.valueOf(member.getLocation().getBlockY());
        String z = String.valueOf(member.getLocation().getBlockZ());
        String uuid = member.getUniqueId().toString();
        for (String raw : commands) {
            if (raw == null || raw.trim().isEmpty()) continue;
            String cmd = raw.startsWith("/") ? raw.substring(1) : raw;
            cmd = cmd.replace("%member%", member.getName())
                     .replace("%player%", killer != null ? killer.getName() : "Unknown")
                     .replace("%uuid%", uuid)
                     .replace("%faction%", factionTag != null ? factionTag : "NoFaction")
                     .replace("%world%", world)
                     .replace("%x%", x)
                     .replace("%y%", y)
                     .replace("%z%", z);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), ColorUtil.color(cmd));
        }
    }

    private EnderDragon findDragonByMeta() {
        World w = Bukkit.getWorld(worldName);
        if (w != null) {
            try {
                for (Entity e : w.getEntities()) {
                    if (e instanceof EnderDragon && hasOurMeta(e) && e.isValid()) {
                        return (EnderDragon) e;
                    }
                }
            } catch (Throwable ignored) {}
        }
        // As a fallback, search all worlds (in case the world name changed)
        try {
            for (World world : Bukkit.getWorlds()) {
                for (Entity e : world.getEntities()) {
                    if (e instanceof EnderDragon && hasOurMeta(e) && e.isValid()) {
                        return (EnderDragon) e;
                    }
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    @EventHandler
    public void onDragonCreatePortal(org.bukkit.event.entity.EntityCreatePortalEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof EnderDragon)) return;
        if (!hasOurMeta(entity)) return;
        event.setCancelled(true);
    }
}
