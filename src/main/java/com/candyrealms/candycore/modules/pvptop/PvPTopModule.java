package com.candyrealms.candycore.modules.pvptop;

import com.candyrealms.candycore.AnubisCore;
import com.candyrealms.candycore.configuration.PvPTopCFG;
import com.candyrealms.candycore.utils.ColorUtil;
import com.candyrealms.candycore.utils.CompatUtil;
import com.candyrealms.candycore.utils.ItemCreator;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;
import java.util.stream.Collectors;

public class PvPTopModule {

    private final AnubisCore plugin;
    private final PvPTopCFG cfg;

    private FileConfiguration config;

    public PvPTopModule(AnubisCore plugin) {
        this.plugin = plugin;
        this.cfg = plugin.getPvPTopCFG();
        this.config = cfg.getConfig();
    }

    public void reload() {
        this.config = cfg.getConfig();
    }

    // Data management
    public void addKillFor(Player killer) {
        if (!isEnabled()) return;
        String factionId = getFactionId(killer);
        if (factionId == null || factionId.isEmpty()) return;

        // FIX: cache tag so GUI never shows numeric/uuid ids
        config.set("data.factions." + factionId + ".tag", resolveFactionTag(factionId));

        int current = config.getInt("data.factions." + factionId + ".kills", 0);
        config.set("data.factions." + factionId + ".kills", current + 1);
        cfg.saveConfig();
        sendAwardMessage("kill", factionId, killer != null ? killer.getName() : "", 1);
    }

    public void addKoTHCapture(String factionId, int amount) {
        addKoTHCapture(factionId, amount, "");
    }

    public void addKoTHCapture(String factionId, int amount, String playerName) {
        if (!isEnabled()) return;
        if (factionId == null || factionId.isEmpty()) return;

        // FIX: cache tag on write
        config.set("data.factions." + factionId + ".tag", resolveFactionTag(factionId));

        int current = config.getInt("data.factions." + factionId + ".koths", 0);
        config.set("data.factions." + factionId + ".koths", Math.max(0, current + amount));
        cfg.saveConfig();
        sendAwardMessage("koth", factionId, playerName, amount);
    }

    public void addDTCWin(String factionId, int amount) {
        addDTCWin(factionId, amount, "");
    }

    public void addDTCWin(String factionId, int amount, String playerName) {
        if (!isEnabled()) return;
        if (factionId == null || factionId.isEmpty()) return;

        // FIX: cache tag on write
        config.set("data.factions." + factionId + ".tag", resolveFactionTag(factionId));

        int current = config.getInt("data.factions." + factionId + ".dtc", 0);
        config.set("data.factions." + factionId + ".dtc", Math.max(0, current + amount));
        cfg.saveConfig();
        sendAwardMessage("dtc", factionId, playerName, amount);
    }

    public boolean isEnabled() {
        return config.getBoolean("enabled", true);
    }

    public void openTopGui(Player viewer) {
        if (!isEnabled()) {
            viewer.sendMessage(ColorUtil.color(plugin.getConfigManager().getPrefix() + "&cPvP Top is disabled."));
            return;
        }

        String title = config.getString("gui.title", "&8PvP Top");
        int rows = Math.max(1, Math.min(6, config.getInt("gui.rows", 5)));

        Gui gui = Gui.gui()
                .title(Component.text(ColorUtil.color(title)))
                .rows(rows)
                .create();
        gui.setDefaultClickAction(e -> e.setCancelled(true));

        // Filler bottom row
        String fillerMat = config.getString("gui.filler", "");
        if (fillerMat != null && !fillerMat.isEmpty()) {
            Material mat = material(fillerMat, CompatUtil.glassPaneBlack().getType());
            gui.getFiller().fillBottom(ItemBuilder.from(new ItemStack(mat, 1, (short) (isLegacyGlass(mat) ? 15 : 0))).asGuiItem());
        } else {
            gui.getFiller().fillBottom(ItemBuilder.from(CompatUtil.glassPaneBlack()).asGuiItem());
        }

        // Top entries placed into predetermined slots
        List<FactionEntry> top = computeTop(10);
        List<Integer> slots = configuredSlots(rows);
        for (int i = 0; i < 10; i++) {
            int rank = i + 1;
            int slot = (i < slots.size()) ? slots.get(i) : (rank); // fallback to first slots
            int r = ((slot - 1) / 9) + 1;
            int c = ((slot - 1) % 9) + 1;

            ItemStack item;
            if (i < top.size()) {
                item = buildFactionItem(top.get(i), rank);
            } else {
                item = buildEmptyItem(rank);
            }
            gui.setItem(r, c, new GuiItem(item));
        }

        // Close item center bottom
        String closeName = ColorUtil.color(config.getString("gui.close.name", "&cClose"));
        Material closeMat = material(config.getString("gui.close.material", "BARRIER"), Material.BARRIER);
        ItemStack close = new ItemCreator(closeMat, closeName).getItem();
        gui.setItem(rows, 5, ItemBuilder.from(close).asGuiItem(e -> e.getWhoClicked().closeInventory()));

        gui.open(viewer);
    }

    private ItemStack buildFactionItem(FactionEntry entry, int rank) {
        String nameTpl = config.getString("gui.item.name", "&d%faction_tag% &7- &f%points% pts");
        String name = applyPlaceholders(nameTpl, entry, rank);
        List<String> loreRaw = new ArrayList<>(config.getStringList("gui.item.lore"));
        List<String> lore = loreRaw.stream().map(s -> applyPlaceholders(s, entry, rank)).collect(Collectors.toList());

        // Create head item
        String placeholderTexture = config.getString("gui.placeholder_texture", "");
        ItemCreator creator = new ItemCreator(headMaterial(), name, 1, 3,
                (entry.leaderName == null || entry.leaderName.isEmpty()) ? placeholderTexture : "",
                lore);
        ItemStack stack = creator.getItem();

        // If we have a leader, set skull owner
        if (entry.leaderName != null && !entry.leaderName.isEmpty()) {
            try {
                if (stack.getItemMeta() instanceof SkullMeta) {
                    SkullMeta sm = (SkullMeta) stack.getItemMeta();
                    try {
                        sm.setOwner(entry.leaderName);
                    } catch (Throwable t) {
                        try {
                            OfflinePlayer op = Bukkit.getOfflinePlayer(entry.leaderName);
                            java.lang.reflect.Method m = sm.getClass().getMethod("setOwningPlayer", OfflinePlayer.class);
                            m.invoke(sm, op);
                        } catch (Exception ignored) {}
                    }
                    stack.setItemMeta(sm);
                }
            } catch (Throwable ignored) {}
        }
        return stack;
    }

    private ItemStack buildEmptyItem(int rank) {
        String placeholderTexture = config.getString("gui.placeholder_texture", "");
        String nameTpl = config.getString("gui.empty.name", "&7[#%rank%] Empty");
        List<String> loreRaw = config.getStringList("gui.empty.lore");
        FactionEntry empty = new FactionEntry("", "No Faction", "", 0, 0, 0, 0);
        String name = applyPlaceholders(nameTpl, empty, rank);
        List<String> lore = loreRaw.stream().map(s -> applyPlaceholders(s, empty, rank)).collect(Collectors.toList());
        return new ItemCreator(headMaterial(), name, 1, 3, placeholderTexture, lore).getItem();
    }

    private Material headMaterial() {
        return CompatUtil.mat("SKULL_ITEM", "PLAYER_HEAD");
    }

    private boolean isLegacyGlass(Material m) {
        return m != null && (m.name().equals("STAINED_GLASS_PANE") || m.name().equals("STAINED_GLASS"));
    }

    private Material material(String matStr, Material def) {
        try {
            Material m = Material.matchMaterial(matStr);
            return (m != null) ? m : def;
        } catch (Throwable t) {
            return def;
        }
    }

    // Messaging
    private void sendAwardMessage(String type, String factionId, String playerName, int amount) {
        String base = "messages." + type;
        if (!config.getBoolean(base + ".enabled", true)) return;

        int weight;
        switch (type) {
            case "koth": weight = kothWeight(); break;
            case "dtc": weight = dtcWeight(); break;
            default: weight = killWeight(); break;
        }
        int added = Math.max(0, amount) * weight;

        int kills = config.getInt("data.factions." + factionId + ".kills", 0);
        int koths = config.getInt("data.factions." + factionId + ".koths", 0);
        int dtc = config.getInt("data.factions." + factionId + ".dtc", 0);
        int points = (kills * killWeight()) + (koths * kothWeight()) + (dtc * dtcWeight());

        String tag = resolveFactionTag(factionId);
        String fmt = config.getString(base + ".format", defaultFormat(type));
        String msg = String.valueOf(fmt)
                .replace("%prefix%", plugin.getConfigManager().getPrefix())
                .replace("%player%", playerName == null ? "" : playerName)
                .replace("%faction_tag%", tag)
                .replace("%kills%", String.valueOf(kills))
                .replace("%koths%", String.valueOf(koths))
                .replace("%dtc%", String.valueOf(dtc))
                .replace("%points%", String.valueOf(points))
                .replace("%points_added%", String.valueOf(added));

        // Deliver only to the acting player, not globally
        if (playerName != null && !playerName.isEmpty()) {
            org.bukkit.entity.Player target = org.bukkit.Bukkit.getPlayerExact(playerName);
            if (target == null) {
                try { target = org.bukkit.Bukkit.getPlayer(playerName); } catch (Throwable ignored) {}
            }
            if (target != null) {
                target.sendMessage(ColorUtil.color(msg));
            }
        }
    }

    private String defaultFormat(String type) {
        if ("koth".equalsIgnoreCase(type)) {
            return "%prefix% &dPvPTop &7| &f%faction_tag% &fcaptured KoTH &7(+%points_added% pts, total &f%points%&7)";
        }
        if ("dtc".equalsIgnoreCase(type)) {
            return "%prefix% &dPvPTop &7| &f%player% &fdestroyed the core for &f%faction_tag% &7(+%points_added% pts, total &f%points%&7)";
        }
        return "%prefix% &dPvPTop &7| &f%player% &fkilled a player for &f%faction_tag% &7(+%points_added% pts, total &f%points%&7)";
    }

    private List<Integer> configuredSlots(int rows) {
        List<Integer> list = new ArrayList<>();
        for (Object o : config.getList("gui.slots", Collections.emptyList())) {
            if (o == null) continue;
            try {
                int v = Integer.parseInt(String.valueOf(o));
                if (v > 0) list.add(v);
            } catch (NumberFormatException ignored) {}
        }
        if (list.size() >= 10) return list.subList(0, 10);

        // Default: 10 centered slots across row 2 and 3
        List<Integer> def = Arrays.asList(11, 12, 13, 14, 15, 20, 21, 22, 23, 24);
        return def;
    }

    private String applyPlaceholders(String s, FactionEntry e, int rank) {
        return ColorUtil.color(String.valueOf(s)
                .replace("%rank%", String.valueOf(rank))
                .replace("%position%", String.valueOf(rank))
                .replace("%faction_tag%", e.tag)
                .replace("%leader%", (e.leaderName == null || e.leaderName.isEmpty()) ? "None" : e.leaderName)
                .replace("%kills%", String.valueOf(e.kills))
                .replace("%kill_points%", String.valueOf(e.kills * killWeight()))
                .replace("%koths%", String.valueOf(e.koths))
                .replace("%koth_points%", String.valueOf(e.koths * kothWeight()))
                .replace("%dtc%", String.valueOf(e.dtc))
                .replace("%dtc_points%", String.valueOf(e.dtc * dtcWeight()))
                .replace("%points%", String.valueOf(e.points))
        );
    }

    // Ranking
    public List<FactionEntry> computeTop(int limit) {
        Map<String, FactionEntry> entries = new HashMap<>();
        if (config.getConfigurationSection("data.factions") != null) {
            for (String id : config.getConfigurationSection("data.factions").getKeys(false)) {
                // Exclude non-player/system factions and RaidOutpost
                if (shouldExcludeKey(id)) continue;
                int kills = config.getInt("data.factions." + id + ".kills", 0);
                int koths = config.getInt("data.factions." + id + ".koths", 0);
                int dtc = config.getInt("data.factions." + id + ".dtc", 0);
                String tag = resolveFactionTag(id);
                if (tag != null && tag.equalsIgnoreCase("RaidOutpost")) continue;
                String leader = resolveFactionLeaderName(id);
                int points = (kills * killWeight()) + (koths * kothWeight()) + (dtc * dtcWeight());
                entries.put(id, new FactionEntry(id, tag, leader, kills, koths, dtc, points));
            }
        }
        return entries.values().stream()
                .sorted(Comparator.comparingInt((FactionEntry e) -> e.points).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    private boolean shouldExcludeKey(String key) {
        if (key == null || key.isEmpty()) return true;
        if (key.equalsIgnoreCase("RaidOutpost")) return true; // Safety if tag accidentally used as key
        Faction f = findFactionByAny(key);
        if (f == null) return false;
        return isNonPlayerFaction(f);
    }

    private int killWeight() { return config.getInt("points.kill", 1); }
    private int kothWeight() { return config.getInt("points.koth", 25); }
    private int dtcWeight() { return config.getInt("points.dtc", 15); }

    public String getFactionId(Player player) {
        try {
            FPlayer fp = FPlayers.getInstance().getByPlayer(player);
            if (fp == null) return null;
            Faction f = fp.getFaction();
            if (f == null) return null;
            if (isNonPlayerFaction(f)) return null; // Exclude Wilderness / non-player factions
            String id = null;
            try { id = f.getId(); } catch (Throwable ignored) {}
            // Do not invent IDs; return null if not resolvable
            return (id == null || id.isEmpty()) ? null : id;
        } catch (Throwable t) {
            return null;
        }
    }

    public String getFactionId(java.util.UUID uuid) {
        try {
            if (uuid == null) return null;
            com.massivecraft.factions.FPlayer fp = FPlayers.getInstance().getById(uuid.toString());
            if (fp == null) return null;
            Faction f = fp.getFaction();
            if (f == null) return null;
            if (isNonPlayerFaction(f)) return null; // Exclude Wilderness / non-player factions
            String id = null;
            try { id = f.getId(); } catch (Throwable ignored) {}
            return (id == null || id.isEmpty()) ? null : id;
        } catch (Throwable t) {
            return null;
        }
    }

    // Determine if a faction is a non-player/system faction like Wilderness
    private boolean isNonPlayerFaction(Faction f) {
        try {
            // Prefer explicit API methods if available
            try {
                java.lang.reflect.Method m = f.getClass().getMethod("isWilderness");
                Object v = m.invoke(f);
                if (v instanceof Boolean && (Boolean) v) return true;
            } catch (NoSuchMethodException ignored) {}
            try {
                java.lang.reflect.Method m = f.getClass().getMethod("isSafeZone");
                Object v = m.invoke(f);
                if (v instanceof Boolean && (Boolean) v) return true;
            } catch (NoSuchMethodException ignored) {}
            try {
                java.lang.reflect.Method m = f.getClass().getMethod("isWarZone");
                Object v = m.invoke(f);
                if (v instanceof Boolean && (Boolean) v) return true;
            } catch (NoSuchMethodException ignored) {}

            // Fallback to tag name checks
            String tag = null;
            try { tag = f.getTag(); } catch (Throwable ignored) {}
            if (tag != null) {
                String t = tag.trim();
                if (t.equalsIgnoreCase("wilderness") || t.equalsIgnoreCase("safezone") || t.equalsIgnoreCase("warzone")
                        || t.equalsIgnoreCase("RaidOutpost")) {
                    return true;
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    // FIX: never fall back to returning the raw key; use live tag or cached tag
    private String resolveFactionTag(String key) {
        try {
            Faction f = findFactionByAny(key);
            if (f != null) {
                String tag = null;
                try { tag = f.getTag(); } catch (Throwable ignored) {}
                if (tag != null && !tag.isEmpty()) return tag;
            }
            String cached = config.getString("data.factions." + key + ".tag", "");
            if (cached != null && !cached.isEmpty()) return cached;
            return "Unknown";
        } catch (Throwable t) {
            return "Unknown";
        }
    }

    private String resolveFactionLeaderName(String key) {
        try {
            Faction f = findFactionByAny(key);
            if (f == null) return "";
            return resolveFactionLeaderName(f);
        } catch (Throwable t) {
            return "";
        }
    }

    // FIX: robust lookup supporting String, int, and UUID ids plus tag lookup
    private Faction findFactionByAny(String key) {
        if (key == null || key.isEmpty()) return null;
        try {
            Factions api = Factions.getInstance();
            Faction f;

            // String id
            try { f = api.getFactionById(key); if (f != null) return f; } catch (Throwable ignored) {}

            // Integer id (common on forks)
            Integer asInt = null;
            try { asInt = Integer.valueOf(key); } catch (Throwable ignored) {}
            if (asInt != null) {
                try {
                    java.lang.reflect.Method m = api.getClass().getMethod("getFactionById", int.class);
                    Object v = m.invoke(api, asInt.intValue());
                    if (v instanceof Faction) return (Faction) v;
                } catch (NoSuchMethodException ignored) {
                    try {
                        java.lang.reflect.Method m = api.getClass().getMethod("getFaction", int.class);
                        Object v = m.invoke(api, asInt.intValue());
                        if (v instanceof Faction) return (Faction) v;
                    } catch (Throwable ignored2) {}
                } catch (Throwable ignored) {}
            }

            // UUID id
            java.util.UUID u = parseUuidQuiet(key);
            if (u != null) {
                try {
                    java.lang.reflect.Method m = api.getClass().getMethod("getFactionById", java.util.UUID.class);
                    Object v = m.invoke(api, u);
                    if (v instanceof Faction) return (Faction) v;
                } catch (Throwable ignored) {}
                try {
                    java.lang.reflect.Method m = api.getClass().getMethod("getFaction", java.util.UUID.class);
                    Object v = m.invoke(api, u);
                    if (v instanceof Faction) return (Faction) v;
                } catch (Throwable ignored) {}
            }

            // By tag
            try { f = api.getByTag(key); if (f != null) return f; } catch (Throwable ignored) {}

            // Fallback: manual scan
            try {
                for (Faction x : api.getAllFactions()) {
                    try {
                        String tag = x.getTag();
                        if (tag != null && (tag.equals(key) || tag.equalsIgnoreCase(key))) return x;
                    } catch (Throwable ignored) {}
                    try {
                        String idStr = null;
                        try { idStr = String.valueOf(x.getId()); } catch (Throwable ignoredId) {}
                        if (idStr != null && idStr.equals(key)) return x;
                    } catch (Throwable ignored) {}
                }
            } catch (Throwable ignored) {}

            return null;
        } catch (Throwable t) {
            return null;
        }
    }

    private String resolveFactionLeaderName(Faction f) {
        // 1) Direct FPlayer access via common methods
        try {
            Object leaderObj = null;
            for (String mName : new String[]{"getFPlayerLeader", "getLeader", "getFPlayerAdmin", "getAdmin"}) {
                try {
                    java.lang.reflect.Method m = f.getClass().getMethod(mName);
                    leaderObj = m.invoke(f);
                    if (leaderObj != null) break;
                } catch (NoSuchMethodException ignored) {
                } catch (Throwable ignored) {}
            }
            if (leaderObj != null) {
                // If it's an FPlayer, use it
                if (leaderObj instanceof FPlayer) {
                    String name = ((FPlayer) leaderObj).getName();
                    if (name != null) return name;
                }
                // If it has a getName() method, call it
                try {
                    java.lang.reflect.Method getName = leaderObj.getClass().getMethod("getName");
                    Object v = getName.invoke(leaderObj);
                    if (v != null) return String.valueOf(v);
                } catch (Throwable ignored) {}
                // If it's a UUID, resolve via FPlayers or Bukkit
                if (leaderObj instanceof java.util.UUID) {
                    return nameFromUuid((java.util.UUID) leaderObj);
                }
                // If it's a String, it may be UUID or player name
                if (leaderObj instanceof String) {
                    String s = (String) leaderObj;
                    java.util.UUID u = parseUuidQuiet(s);
                    if (u != null) return nameFromUuid(u);
                    return s; // assume name
                }
            }
        } catch (Throwable ignored) {}

        // 2) Dedicated leader id/uuid methods
        try {
            for (String mName : new String[]{"getLeaderId", "getLeaderUUID", "getOwnerId", "getOwnerUUID"}) {
                try {
                    java.lang.reflect.Method m = f.getClass().getMethod(mName);
                    Object v = m.invoke(f);
                    if (v instanceof java.util.UUID) return nameFromUuid((java.util.UUID) v);
                    if (v instanceof String) {
                        java.util.UUID u = parseUuidQuiet((String) v);
                        if (u != null) return nameFromUuid(u);
                        if (v != null) return String.valueOf(v);
                    }
                } catch (NoSuchMethodException ignored) {
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}

        // 3) Scan members for a leader role
        try {
            java.util.Collection<?> members = null;
            for (String mName : new String[]{"getFPlayers", "getMembers", "getUsers"}) {
                try {
                    java.lang.reflect.Method m = f.getClass().getMethod(mName);
                    Object v = m.invoke(f);
                    if (v instanceof java.util.Collection) { members = (java.util.Collection<?>) v; break; }
                } catch (NoSuchMethodException ignored) {
                } catch (Throwable ignored) {}
            }
            if (members != null) {
                for (Object o : members) {
                    if (o == null) continue;
                    // Try isLeader()
                    try {
                        java.lang.reflect.Method isLeader = o.getClass().getMethod("isLeader");
                        Object v = isLeader.invoke(o);
                        if (v instanceof Boolean && (Boolean) v) {
                            // name via getName()
                            try {
                                java.lang.reflect.Method getName = o.getClass().getMethod("getName");
                                Object n = getName.invoke(o);
                                if (n != null) return String.valueOf(n);
                            } catch (Throwable ignored) {}
                        }
                    } catch (NoSuchMethodException ignored) {}
                    // Try role check
                    try {
                        java.lang.reflect.Method getRole = o.getClass().getMethod("getRole");
                        Object role = getRole.invoke(o);
                        String roleName = null;
                        if (role != null) {
                            try { roleName = String.valueOf(role.getClass().getMethod("name").invoke(role)); } catch (Throwable ignored) {}
                            if (roleName != null && (roleName.equalsIgnoreCase("LEADER") || roleName.equalsIgnoreCase("ADMIN"))) {
                                try {
                                    java.lang.reflect.Method getName = o.getClass().getMethod("getName");
                                    Object n = getName.invoke(o);
                                    if (n != null) return String.valueOf(n);
                                } catch (Throwable ignored) {}
                            }
                        }
                    } catch (NoSuchMethodException ignored) {}
                }
            }
        } catch (Throwable ignored) {}

        return "";
    }

    private java.util.UUID parseUuidQuiet(String s) {
        if (s == null) return null;
        try { return java.util.UUID.fromString(s); } catch (IllegalArgumentException ex) { return null; }
    }

    private String nameFromUuid(java.util.UUID uuid) {
        if (uuid == null) return "";
        try {
            com.massivecraft.factions.FPlayer byId = FPlayers.getInstance().getById(uuid.toString());
            if (byId != null && byId.getName() != null) return byId.getName();
        } catch (Throwable ignored) {}
        try {
            org.bukkit.OfflinePlayer op = org.bukkit.Bukkit.getOfflinePlayer(uuid);
            if (op != null && op.getName() != null) return op.getName();
        } catch (Throwable ignored) {}
        return "";
    }

    // Admin utilities
    public void resetAllData() {
        try {
            // Clear the factions data section
            if (config.getConfigurationSection("data") == null) {
                config.createSection("data");
            }
            // Replace with empty section to avoid lingering keys
            config.set("data.factions", null);
            config.createSection("data").createSection("factions");
            cfg.saveConfig();
        } catch (Throwable ignored) {}
    }

    public static final class FactionEntry {
        public final String id;
        public final String tag;
        public final String leaderName;
        public final int kills;
        public final int koths;
        public final int dtc;
        public final int points;

        public FactionEntry(String id, String tag, String leaderName, int kills, int koths, int dtc, int points) {
            this.id = id;
            this.tag = tag;
            this.leaderName = leaderName;
            this.kills = kills;
            this.koths = koths;
            this.dtc = dtc;
            this.points = points;
        }
    }
}
