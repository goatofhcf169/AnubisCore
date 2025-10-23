package com.candyrealms.candycore.modules.coinsbooster;

import com.candyrealms.candycore.AnubisCore;
import com.candyrealms.candycore.configuration.CoinsBoosterCFG;
import com.candyrealms.candycore.utils.ColorUtil;
import com.golfing8.kore.FactionsKore;
import com.golfing8.kore.event.outpost.OutpostCaptureEvent;
import com.golfing8.kore.feature.OutpostFeature;
import com.golfing8.kore.object.Outpost;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
import me.fullpage.mantichoes.api.events.HoeUseEvent;
import me.fullpage.mantichoes.data.MPlayers;
import me.fullpage.mantichoes.wrappers.MPlayer;
import me.fullpage.manticlib.integrations.ManticHoesIntegration;
import me.fullpage.manticlib.integrations.ManticRodsIntegration;
import me.fullpage.manticlib.integrations.ManticSwordsIntegration;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.Event;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;
import java.util.Objects;

public class CoinsBoosterModule implements Listener {

    private final AnubisCore plugin;
    private final CoinsBoosterCFG cfg;
    private FileConfiguration config;

    private boolean enabled;
    private String outpostName;
    private double multiplier;
    private boolean notifyFaction;
    private boolean broadcastGain;
    private boolean broadcastLose;
    private String gainMessage;
    private String loseMessage;
    private boolean usePrefix;
    private String label; // coins label for messages

    private String boostedFactionId; // current controlling faction id for configured outpost

    // Dynamic hook flags to avoid double-registration
    private boolean rodsHooked = false;
    private boolean swordsHooked = false;

    // Polling task to detect outpost control loss/changes not covered by events
    private BukkitTask pollTask;

    // Per-source toggles and modes
    private boolean hoesEnabled;
    private boolean rodsEnabled;
    private boolean swordsEnabled;
    private String rodsMode;   // coins | money
    private String swordsMode; // coins | money
    private double hoesMultiplier;
    private double rodsMultiplier;
    private double swordsMultiplier;

    // Integrations (via ManticLib)
    private ManticHoesIntegration hoesIntegration;
    private ManticRodsIntegration rodsIntegration;
    private ManticSwordsIntegration swordsIntegration;

    public CoinsBoosterModule(AnubisCore plugin) {
        this.plugin = plugin;
        this.cfg = new CoinsBoosterCFG(plugin);
        this.config = cfg.getConfig();
        cache();
        recomputeFromOutpost();
        registerDynamicExternalHooks();
        initIntegrations();
        startOutpostPoller();
    }

    public void reload() {
        cfg.reloadConfig();
        this.config = cfg.getConfig();
        cache();
        recomputeFromOutpost();
        registerDynamicExternalHooks();
        initIntegrations();
        startOutpostPoller();
    }

    private void cache() {
        enabled = config.getBoolean("enabled", true);
        outpostName = String.valueOf(config.getString("outpost-name", "Sand"));
        multiplier = Math.max(1.0, config.getDouble("multiplier", 1.5));
        notifyFaction = config.getBoolean("messages.notify-faction", true);
        broadcastGain = config.getBoolean("messages.broadcast-gain", false);
        broadcastLose = config.getBoolean("messages.broadcast-lose", false);
        gainMessage = config.getString("messages.gain", "&aYour faction captured &d%outpost%&a and now has &f%multiplier%x &a%label% boost!");
        loseMessage = config.getString("messages.lose", "&cYour faction lost control of &d%outpost%&c. %label% boost ended.");
        usePrefix = config.getBoolean("messages.use-prefix", true);
        label = config.getString("messages.label", "coins");

        // Per-source settings (fallback to global multiplier)
        hoesEnabled = config.getBoolean("hoes.enabled", true);
        rodsEnabled = config.getBoolean("rods.enabled", true);
        swordsEnabled = config.getBoolean("swords.enabled", true);
        rodsMode = String.valueOf(config.getString("rods.mode", "coins"));
        swordsMode = String.valueOf(config.getString("swords.mode", "coins"));
        hoesMultiplier = config.isSet("hoes.multiplier") ? Math.max(1.0, config.getDouble("hoes.multiplier")) : multiplier;
        rodsMultiplier = config.isSet("rods.multiplier") ? Math.max(1.0, config.getDouble("rods.multiplier")) : multiplier;
        swordsMultiplier = config.isSet("swords.multiplier") ? Math.max(1.0, config.getDouble("swords.multiplier")) : multiplier;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHoeUse(HoeUseEvent event) {
        if (!enabled || !hoesEnabled) return;
        Player p = event.getPlayer();
        String fId = getFactionId(p);
        if (fId == null || fId.isEmpty()) return;
        if (boostedFactionId != null && boostedFactionId.equalsIgnoreCase(fId)) {
            // Use top-up mode to guarantee multiplier even if setTokens is ignored
            double base = event.getTokens();
            if (base <= 0.0) {
                try { base = Math.max(base, (double) event.getTokensGained()); } catch (Throwable ignored) {}
            }
            final double extra = base > 0.0 ? (base * Math.max(0.0, (hoesMultiplier - 1.0))) : 0.0;
            if (extra <= 0.0) return;

            // Apply after the original plugin has awarded tokens
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    if (hoesIntegration != null && hoesIntegration.isActive()) {
                        hoesIntegration.giveTokens(p.getUniqueId(), extra);
                    } else {
                        MPlayer mp = MPlayers.get(p.getUniqueId());
                        if (mp != null) mp.addTokens(extra);
                    }
                } catch (Throwable ignored) {}
            });
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOutpostCapture(OutpostCaptureEvent event) {
        if (!enabled) return;
        Outpost outpost = event.getOutpost();
        if (outpost == null) return;
        if (outpostName == null || outpostName.isEmpty()) return;
        String name = safe(outpost.getName());
        if (!name.equalsIgnoreCase(safe(outpostName))) return;

        final String before = boostedFactionId;
        // Delay to allow FactionsKore to update controlling/lastControlling fields
        Bukkit.getScheduler().runTask(plugin, () -> {
            // Prefer controller from event; fallback to feature lookup if unavailable
            String after = extractControllerIdFromEvent(event);
            if (after == null || after.isEmpty()) {
                after = resolveCurrentControllerId();
            }
            // Only switch and notify the captor
            if (!Objects.equals(before, after)) {
                if (after != null) {
                    Player captor = resolveCapturingPlayer(event);
                    if (captor != null) {
                        String capFactionId = getFactionId(captor);
                        if (capFactionId != null && capFactionId.equalsIgnoreCase(after)) {
                            // Send only to the player who captured it
                            String msg = format(gainMessage, outpost);
                            captor.sendMessage(msg);
                        }
                    }
                }
            }
            boostedFactionId = after;
        });
    }

    private void notifyFactionGain(String factionId, Outpost outpost) {
        String msg = format(gainMessage, outpost);
        if (broadcastGain) {
            Bukkit.broadcastMessage(ColorUtil.color(msg));
        }
        if (notifyFaction) {
            sendToFaction(factionId, msg);
        }
    }

    private void notifyFactionLoss(String factionId, Outpost outpost) {
        String msg = format(loseMessage, outpost);
        if (broadcastLose) {
            Bukkit.broadcastMessage(ColorUtil.color(msg));
        }
        if (notifyFaction) {
            sendToFaction(factionId, msg);
        }
    }

    private String format(String template, Outpost outpost) {
        String base = String.valueOf(template)
                .replace("%outpost%", safe(outpost.getName()))
                .replace("%multiplier%", String.valueOf(multiplier))
                .replace("%label%", String.valueOf(label));
        return ColorUtil.color((usePrefix ? plugin.getConfigManager().getPrefix() + " " : "") + base);
    }

    private void sendToFaction(String factionId, String message) {
        try {
            Faction f = Factions.getInstance().getFactionById(factionId);
            if (f == null) return;
            Collection<Player> online = f.getOnlinePlayers();
            if (online == null) return;
            for (Player p : online) {
                if (p != null) p.sendMessage(message);
            }
        } catch (Throwable ignored) {}
    }

    private void registerDynamicExternalHooks() {
        // Only register once per runtime
        if (!rodsHooked && rodsEnabled && Bukkit.getPluginManager().getPlugin("ManticRods") != null) {
            rodsHooked = registerBoostEvent("me.fullpage.manticrods.api.events.RodUseEvent", rodsMode, rodsMultiplier);
        }
        if (!swordsHooked && swordsEnabled && Bukkit.getPluginManager().getPlugin("ManticSwords") != null) {
            swordsHooked = registerBoostEvent("me.fullpage.manticsword.api.events.SwordUseEvent", swordsMode, swordsMultiplier);
        }
    }

    private boolean registerBoostEvent(String eventClassName, String mode, double localMultiplier) {
        try {
            final Class<? extends Event> eventClass = (Class<? extends Event>) Class.forName(eventClassName);
            Bukkit.getPluginManager().registerEvent(
                    eventClass,
                    this,
                    // Run at MONITOR to read/write final values after other plugins
                    org.bukkit.event.EventPriority.MONITOR,
                    (EventExecutor) (listener, event) -> {
                        try {
                            if (!enabled) return;
                            Object playerObj = event.getClass().getMethod("getPlayer").invoke(event);
                            if (!(playerObj instanceof Player)) return;
                            Player p = (Player) playerObj;
                            String fId = getFactionId(p);
                            if (fId == null || fId.isEmpty()) return;
                            if (boostedFactionId == null || !boostedFactionId.equalsIgnoreCase(fId)) return;

                            java.lang.reflect.Method getMoney = event.getClass().getMethod("getMoney");
                            Object moneyObj = getMoney.invoke(event);
                            if (!(moneyObj instanceof Number)) return;
                            double money = ((Number) moneyObj).doubleValue();
                            if (money <= 0.0) return;

                            // For rods/swords, convert payout into token top-up (all sources boost tokens)
                            // Rods/Swords do not award tokens themselves, so add full base * multiplier as tokens
                            final double extra = money * Math.max(0.0, localMultiplier);
                            if (extra <= 0.0) return;
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                try {
                                    if (hoesIntegration != null && hoesIntegration.isActive()) {
                                        hoesIntegration.giveTokens(p.getUniqueId(), extra);
                                    } else {
                                        MPlayer mp = MPlayers.get(p.getUniqueId());
                                        if (mp != null) mp.addTokens(extra);
                                    }
                                } catch (Throwable ignored) {}
                            });
                        } catch (Throwable ignored) {}
                    },
                    plugin
            );
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private void startOutpostPoller() {
        // Cancel previous task if any
        if (pollTask != null) {
            try { pollTask.cancel(); } catch (Throwable ignored) {}
            pollTask = null;
        }
        // Poll every 20 ticks (~1s) to catch control loss or neutralization events
        pollTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!enabled) return;
            if (outpostName == null || outpostName.isEmpty()) return;
            final String before = boostedFactionId;
            final String after = computeStableControllerIdPreserving(before);
            if (!Objects.equals(before, after)) {
                // Do not send any messages in poller; only update state.
                boostedFactionId = after;
            }
        }, 20L, 20L);
    }

    private Player resolveCapturingPlayer(OutpostCaptureEvent event) {
        try {
            try {
                java.lang.reflect.Method m = event.getClass().getMethod("getPlayer");
                Object o = m.invoke(event);
                if (o instanceof Player) return (Player) o;
            } catch (NoSuchMethodException ignored) {}
            try {
                java.lang.reflect.Method m = event.getClass().getMethod("getFPlayer");
                Object o = m.invoke(event);
                if (o instanceof FPlayer) {
                    try {
                        Player p = ((FPlayer) o).getPlayer();
                        if (p != null) return p;
                    } catch (Throwable ignored) {}
                }
            } catch (NoSuchMethodException ignored) {}
            try {
                java.lang.reflect.Method m = event.getClass().getMethod("getCaptor");
                Object o = m.invoke(event);
                if (o instanceof Player) return (Player) o;
            } catch (NoSuchMethodException ignored) {}
        } catch (Throwable ignored) {}
        return null;
    }

    private String extractControllerIdFromEvent(OutpostCaptureEvent event) {
        try {
            // Common possibilities: getFaction(), getNewController(), getController()
            String[] factionGetters = new String[]{"getFaction", "getNewController", "getController"};
            for (String m : factionGetters) {
                try {
                    java.lang.reflect.Method method = event.getClass().getMethod(m);
                    Object res = method.invoke(event);
                    if (res instanceof Faction) {
                        try { String id = ((Faction) res).getId(); if (id != null && !id.isEmpty()) return id; } catch (Throwable ignored) {}
                    }
                } catch (NoSuchMethodException ignored) {}
            }
            // ID getters: getFactionId(), getControllerId(), getNewControllerId()
            String[] idGetters = new String[]{"getFactionId", "getControllerId", "getNewControllerId"};
            for (String m : idGetters) {
                try {
                    java.lang.reflect.Method method = event.getClass().getMethod(m);
                    Object res = method.invoke(event);
                    if (res instanceof String) {
                        String id = (String) res;
                        if (id != null && !id.isEmpty()) return id;
                    }
                } catch (NoSuchMethodException ignored) {}
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private String computeStableControllerIdPreserving(String previousId) {
        try {
            OutpostFeature feature = FactionsKore.get().getFeature(OutpostFeature.class);
            if (feature == null) return previousId;
            Outpost target = null;
            try {
                java.util.Collection<Outpost> all = feature.getOutposts();
                if (all != null) {
                    for (Outpost o : all) {
                        if (o != null && safe(o.getName()).equalsIgnoreCase(safe(outpostName))) { target = o; break; }
                    }
                }
            } catch (Throwable ignored) {}
            if (target == null) target = feature.getOutpost(outpostName);
            if (target == null) return previousId;

            String controlling = target.getControllingID();
            if (controlling == null || controlling.isEmpty()) return null;

            // If the outpost is currently being captured/contested, do not switch early.
            if (isOutpostBeingCaptured(target)) {
                // Preserve previous holder until capture fully completes.
                return previousId;
            }
            return controlling;
        } catch (Throwable t) {
            return previousId;
        }
    }

    private boolean isOutpostBeingCaptured(Outpost outpost) {
        try {
            // Try common indicator methods without compile-time dependency.
            String[] boolMethods = new String[]{
                    "isBeingCaptured", "isContested", "isCapturing", "isUnderCapture"
            };
            for (String m : boolMethods) {
                try {
                    java.lang.reflect.Method method = outpost.getClass().getMethod(m);
                    if (method.getReturnType() == boolean.class || method.getReturnType() == Boolean.class) {
                        Object res = method.invoke(outpost);
                        if (res instanceof Boolean && (Boolean) res) return true;
                    }
                } catch (NoSuchMethodException ignored) {}
            }
            // Fallback: if there's a separate capturing ID and it's different than controlling, treat as capturing.
            try {
                java.lang.reflect.Method getCapturing = outpost.getClass().getMethod("getCapturingID");
                Object idObj = getCapturing.invoke(outpost);
                if (idObj instanceof String) {
                    String capturingId = (String) idObj;
                    String controllingId = outpost.getControllingID();
                    if (capturingId != null && !capturingId.isEmpty() && !capturingId.equalsIgnoreCase(controllingId)) {
                        return true;
                    }
                }
            } catch (NoSuchMethodException ignored) {}
        } catch (Throwable ignored) {}
        return false;
    }

    private void initIntegrations() {
        try {
            if (hoesIntegration == null) {
                hoesIntegration = new ManticHoesIntegration();
                hoesIntegration.setProvidingPlugin(plugin);
                hoesIntegration.onEnable();
            }
        } catch (Throwable ignored) {}
        try {
            if (rodsIntegration == null) {
                rodsIntegration = new ManticRodsIntegration();
                rodsIntegration.setProvidingPlugin(plugin);
                rodsIntegration.onEnable();
            }
        } catch (Throwable ignored) {}
        try {
            if (swordsIntegration == null) {
                swordsIntegration = new ManticSwordsIntegration();
                swordsIntegration.setProvidingPlugin(plugin);
                swordsIntegration.onEnable();
            }
        } catch (Throwable ignored) {}
    }

    private void recomputeFromOutpost() {
        try {
            boostedFactionId = resolveCurrentControllerId();
        } catch (Throwable t) {
            boostedFactionId = null;
        }
    }

    private String resolveCurrentControllerId() {
        try {
            OutpostFeature feature = FactionsKore.get().getFeature(OutpostFeature.class);
            if (feature == null) return null;
            Outpost target = null;
            try {
                java.util.Collection<Outpost> all = feature.getOutposts();
                if (all != null) {
                    for (Outpost o : all) {
                        if (o != null && safe(o.getName()).equalsIgnoreCase(safe(outpostName))) { target = o; break; }
                    }
                }
            } catch (Throwable ignored) {}
            if (target == null) target = feature.getOutpost(outpostName);
            if (target == null) return null;
            String id = target.getControllingID();
            return (id == null || id.isEmpty()) ? null : id;
        } catch (Throwable t) {
            return null;
        }
    }

    private String getFactionId(Player player) {
        try {
            FPlayer fp = FPlayers.getInstance().getByPlayer(player);
            if (fp == null) return null;
            Faction f = fp.getFaction();
            if (f == null) return null;
            String id = null;
            try { id = f.getId(); } catch (Throwable ignored) {}
            return (id == null || id.isEmpty()) ? null : id;
        } catch (Throwable t) {
            return null;
        }
    }

    private static String safe(String s) { return s == null ? "" : s; }
}
