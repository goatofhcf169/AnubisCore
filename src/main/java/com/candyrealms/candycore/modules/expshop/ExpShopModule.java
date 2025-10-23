package com.candyrealms.candycore.modules.expshop;

import com.candyrealms.candycore.AnubisCore;
import com.candyrealms.candycore.utils.ColorUtil;
import com.candyrealms.candycore.utils.CompatUtil;
import com.candyrealms.candycore.utils.ItemCreator;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExpShopModule {

    private final AnubisCore plugin;

    private FileConfiguration config;

    public ExpShopModule(AnubisCore plugin) {
        this.plugin = plugin;
        this.config = plugin.getExpCFG().getConfig();
    }

    public void openMenu(Player player) {
        // Build categories GUI per-player for dynamic placeholders like %player_xp%
        PaginatedGui gui = buildCategoriesGui(player);
        gui.open(player);
    }

    public void reload() {
        this.config = plugin.getExpCFG().getConfig();
    }

    private PaginatedGui buildCategoriesGui(Player player) {
        String title = cfg("gui.categories.title", "&8XP Shop");
        int rows = cfgInt("gui.categories.rows", 3);
        int pageSize = cfgInt("gui.categories.page_size", Math.max(1, (rows - 1) * 9));

        PaginatedGui gui = Gui.paginated()
                .title(Component.text(ColorUtil.color(title)))
                .rows(rows)
                .pageSize(pageSize)
                .create();

        // Default cancel
        gui.setDefaultClickAction(e -> e.setCancelled(true));

        // Filler bottom row
        String fillerMat = cfg("gui.categories.filler", "");
        if (!fillerMat.isEmpty()) {
            Material mat = matOrGlass(fillerMat);
            gui.getFiller().fillBottom(ItemBuilder.from(new ItemStack(mat, 1, (short) (isLegacyGlass(mat) ? 15 : 0))).asGuiItem());
        } else {
            gui.getFiller().fillBottom(ItemBuilder.from(CompatUtil.glassPaneBlack()).asGuiItem());
        }

        // Navigation buttons
        placeNav(gui, "gui.categories.nav.previous", "&bPrevious", (e) -> gui.previous());
        placeNav(gui, "gui.categories.nav.next", "&bNext", (e) -> gui.next());

        // Info item (player XP etc.)
        addInfoItem(gui, player, "gui.categories.info_item");

        // Categories
        ConfigurationSection catSec = config.getConfigurationSection("categories");
        boolean hasCategories = catSec != null && !catSec.getKeys(false).isEmpty();

        if (!hasCategories) {
            // Backwards compatibility: treat root items as a single category
            ConfigurationSection legacy = config.getConfigurationSection("items");
            if (legacy != null && !legacy.getKeys(false).isEmpty()) {
                ItemCreator creator = new ItemCreator(Material.CHEST, "&eGeneral");
                GuiItem li = new GuiItem(creator.getItem());
                li.setAction(e -> openCategory(player, "__legacy__"));
                gui.addItem(li);
            }
            return gui;
        }

        for (String catId : catSec.getKeys(false)) {
            String name = catSec.getString(catId + ".name", catId);
            String matStr = catSec.getString(catId + ".material", "CHEST");
            int data = catSec.getInt(catId + ".data", 0);
            String texture = catSec.getString(catId + ".texture", "");
            List<String> lore = catSec.getStringList(catId + ".lore");

            Material material = material(matStr, Material.CHEST);
            ItemCreator creator = new ItemCreator(material, name, 1, data, texture, lore);
            creator.updateLore(s -> replaceCommonPlaceholders(s, player).replace("%category%", name));

            GuiItem item = new GuiItem(creator.getItem());
            item.setAction(e -> openCategory((Player) e.getWhoClicked(), catId));
            int[] pos = configuredPos("categories." + catId, rows);
            if (pos != null) {
                gui.setItem(pos[0], pos[1], item);
            } else {
                gui.addItem(item);
            }
        }

        return gui;
    }

    private void openCategory(Player player, String categoryId) {
        PaginatedGui gui = buildCategoryGui(player, categoryId);
        gui.open(player);
    }

    private PaginatedGui buildCategoryGui(Player player, String categoryId) {
        // Resolve section
        ConfigurationSection catSec = config.getConfigurationSection("categories." + categoryId);
        ConfigurationSection legacyItems = null;
        boolean legacy = false;
        if (catSec == null) {
            // Legacy fallback to root items
            legacyItems = config.getConfigurationSection("items");
            legacy = true;
        }

        String catName = legacy ? "General" : catSec.getString("name", categoryId);
        String titleTpl = cfg("gui.category.title", "&8XP Shop: %category_name%");
        String title = titleTpl.replace("%category_name%", catName);

        int rows = cfgInt("gui.category.rows", 4);
        int pageSize = cfgInt("gui.category.page_size", Math.max(1, (rows - 1) * 9));

        PaginatedGui gui = Gui.paginated()
                .title(Component.text(ColorUtil.color(title)))
                .rows(rows)
                .pageSize(pageSize)
                .create();

        gui.setDefaultClickAction(e -> e.setCancelled(true));

        // Filler bottom row
        String fillerMat = cfg("gui.category.filler", "");
        if (!fillerMat.isEmpty()) {
            Material mat = matOrGlass(fillerMat);
            gui.getFiller().fillBottom(ItemBuilder.from(new ItemStack(mat, 1, (short) (isLegacyGlass(mat) ? 15 : 0))).asGuiItem());
        } else {
            gui.getFiller().fillBottom(ItemBuilder.from(CompatUtil.glassPaneBlack()).asGuiItem());
        }

        // Navigation
        placeNav(gui, "gui.category.nav.previous", "&bPrevious", (e) -> gui.previous());
        placeNav(gui, "gui.category.nav.next", "&bNext", (e) -> gui.next());
        // Back button
        placeNav(gui, "gui.category.nav.back", "&cBack", (e) -> buildCategoriesGui((Player) e.getWhoClicked()).open((Player) e.getWhoClicked()));

        // Info item (player XP etc.)
        addInfoItem(gui, player, "gui.category.info_item");

        // Items
        ConfigurationSection itemsSec = legacy ? legacyItems : catSec.getConfigurationSection("items");
        if (itemsSec == null) return gui;

        for (String itemId : itemsSec.getKeys(false)) {
            String name = itemsSec.getString(itemId + ".name", itemId);
            String matStr = itemsSec.getString(itemId + ".material", "STONE");
            int data = itemsSec.getInt(itemId + ".data", 0);
            String texture = itemsSec.getString(itemId + ".texture", "");
            int price = itemsSec.getInt(itemId + ".price", 0); // <-- Interpret as XP POINTS
            List<String> lore = itemsSec.getStringList(itemId + ".lore");
            List<String> commands = itemsSec.isList(itemId + ".commands")
                    ? itemsSec.getStringList(itemId + ".commands")
                    : Collections.singletonList(itemsSec.getString(itemId + ".command", ""));

            Material material = material(matStr, Material.STONE);
            ItemCreator creator = new ItemCreator(material, name, 1, data, texture, lore);
            creator.updateLore(s -> replaceCommonPlaceholders(s, player)
                    .replace("%price%", String.valueOf(price))
                    .replace("%category_name%", catName));

            GuiItem item = new GuiItem(creator.getItem());
            item.setAction(e -> clickAction(e, price, commands, name));
            int[] pos = configuredPos((legacy ? "items." : "categories." + categoryId + ".items.") + itemId, rows);
            if (pos != null) {
                gui.setItem(pos[0], pos[1], item);
            } else {
                gui.addItem(item);
            }
        }

        return gui;
    }

    private void clickAction(InventoryClickEvent event, int price, List<String> commands, String itemName) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();

        // === Work in XP POINTS ===
        int current = getCurrentExpPoints(player);
        if (current < price) {
            player.sendMessage(ColorUtil.color(cfg("messages.cannot_afford", "&5&lEXP &fYou cannot afford this!")));
            // sound
            String legacy = cfg("sounds.cannot_afford.legacy", "VILLAGER_NO");
            String modern = cfg("sounds.cannot_afford.modern", "ENTITY_VILLAGER_NO");
            float vol = (float) cfgDouble("sounds.cannot_afford.volume", 7.0);
            float pitch = (float) cfgDouble("sounds.cannot_afford.pitch", 6.0);
            CompatUtil.play(player, vol, pitch, legacy, modern);
            return;
        }

        // Subtract XP points safely
        setExpPoints(player, current - price);

        for (String cmd : new ArrayList<>(commands)) {
            if (cmd == null || cmd.trim().isEmpty()) continue;
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", player.getName()));
        }

        String purchased = cfg("messages.purchased", "&aPurchased &f%item_name% &afor &f%price% &aXP.")
                .replace("%item_name%", itemName)
                .replace("%price%", String.valueOf(price));
        player.sendMessage(ColorUtil.color(purchased));

        String sLegacy = cfg("sounds.purchased.legacy", "LEVEL_UP");
        String sModern = cfg("sounds.purchased.modern", "ENTITY_PLAYER_LEVELUP");
        float vol = (float) cfgDouble("sounds.purchased.volume", 1.0);
        float pitch = (float) cfgDouble("sounds.purchased.pitch", 1.0);
        CompatUtil.play(player, vol, pitch, sLegacy, sModern);
    }

    // Helpers
    private String replaceCommonPlaceholders(String s, Player p) {
        return ColorUtil.color(s
                .replace("%player%", p.getName())
                // Show total XP points instead of levels
                .replace("%player_xp%", String.valueOf(getCurrentExpPoints(p))));
        // If you also want to keep levels available: add .replace("%player_level%", String.valueOf(p.getLevel()))
    }

    private void addInfoItem(PaginatedGui gui, Player p, String path) {
        if (!config.getBoolean(path + ".enabled", true)) return;
        String name = cfg(path + ".name", "&aYour XP");
        String matStr = cfg(path + ".material", "EXP_BOTTLE");
        int row = cfgInt(path + ".row", gui.getRows());
        int col = cfgInt(path + ".col", 5);
        List<String> lore = config.getStringList(path + ".lore");

        // Optional head texture support for info item
        boolean head = config.getBoolean(path + ".head", false);
        String texture = config.getString(path + ".texture", "");
        int data = cfgInt(path + ".data", head ? 3 : 0);

        // Avoid referencing newer Material constants directly; resolve safely across versions
        Material infoDefault = head ? headMaterial() : CompatUtil.mat("EXP_BOTTLE", "EXPERIENCE_BOTTLE");
        // If head=true or texture provided, force head material
        Material chosen = (head || (texture != null && !texture.isEmpty())) ? headMaterial() : material(matStr, infoDefault);

        ItemCreator creator = new ItemCreator(chosen, name, 1, data, texture, lore);
        creator.updateLore(s -> replaceCommonPlaceholders(s, p));

        ItemStack infoItem = creator.getItem();
        // If using a head and no custom texture, make it the player's skull
        if ((head || CompatUtil.isSkull(chosen)) && (texture == null || texture.isEmpty())) {
            try {
                if (infoItem.getItemMeta() instanceof SkullMeta) {
                    SkullMeta sm = (SkullMeta) infoItem.getItemMeta();
                    try {
                        // 1.8 and many versions
                        sm.setOwner(p.getName());
                    } catch (Throwable ignore) {
                        // Newer API fallback via reflection (setOwningPlayer)
                        try {
                            java.lang.reflect.Method m = sm.getClass().getMethod("setOwningPlayer", org.bukkit.OfflinePlayer.class);
                            m.invoke(sm, p);
                        } catch (Exception ignored) {}
                    }
                    infoItem.setItemMeta(sm);
                }
            } catch (Throwable ignored) {}
        }

        gui.setItem(row, col, new GuiItem(infoItem));
    }

    private void placeNav(PaginatedGui gui, String basePath, String defName, dev.triumphteam.gui.components.GuiAction<InventoryClickEvent> action) {
        String matStr = cfg(basePath + ".material", "ARROW");
        String name = cfg(basePath + ".name", defName);
        int row = cfgInt(basePath + ".row", gui.getRows());
        int col = cfgInt(basePath + ".col", defName.toLowerCase().contains("back") ? 5 : (defName.toLowerCase().contains("previous") ? 4 : 6));

        ItemStack stack = new ItemCreator(material(matStr, Material.ARROW), name, 1, 0, "", new ArrayList<>()).getItem();
        gui.setItem(row, col, ItemBuilder.from(stack).asGuiItem(action));
    }

    private Material material(String matStr, Material def) {
        try {
            Material m = Material.matchMaterial(matStr);
            return (m != null) ? m : def;
        } catch (Throwable t) {
            return def;
        }
    }

    private Material matOrGlass(String matStr) {
        Material m = material(matStr, null);
        if (m == null) return CompatUtil.glassPaneBlack().getType();
        return m;
    }

    private boolean isLegacyGlass(Material m) {
        return m != null && (m.name().equals("STAINED_GLASS_PANE") || m.name().equals("STAINED_GLASS"));
    }

    private Material headMaterial() {
        return CompatUtil.mat("SKULL_ITEM", "PLAYER_HEAD");
    }

    private String cfg(String path, String def) {
        return config.getString(path, def);
    }

    private int cfgInt(String path, int def) {
        return config.getInt(path, def);
    }

    private double cfgDouble(String path, double def) {
        return config.getDouble(path, def);
    }

    // Returns 1-based row,col if row/col or slot is configured; otherwise null
    private int[] configuredPos(String basePath, int totalRows) {
        int row = config.getInt(basePath + ".row", -1);
        int col = config.getInt(basePath + ".col", -1);
        if (row > 0 && col > 0) return new int[]{row, col};

        int slot = config.getInt(basePath + ".slot", -1);
        if (slot > 0) {
            int r = ((slot - 1) / 9) + 1;
            int c = ((slot - 1) % 9) + 1;
            // Clamp within GUI bounds
            r = Math.max(1, Math.min(totalRows, r));
            c = Math.max(1, Math.min(9, c));
            return new int[]{r, c};
        }
        return null;
    }

    // ====================== XP POINTS HELPERS ======================

    /** Total XP points the player currently has (level base + progress). */
    private int getCurrentExpPoints(Player p) {
        int level = p.getLevel();
        int base = xpAtLevel(level);
        int progress = Math.round(p.getExp() * p.getExpToLevel()); // 0..getExpToLevel()
        return base + progress;
    }

    /** Set exact total XP points; recalculates levels/progress safely across versions. */
    private void setExpPoints(Player p, int points) {
        if (points < 0) points = 0;
        p.setExp(0f);
        p.setLevel(0);
        p.setTotalExperience(0);
        p.giveExp(points);
    }

    /** Cumulative XP points required to reach 'level' from level 0 (Minecraft 1.8 formulas). */
    private int xpAtLevel(int level) {
        if (level <= 16) {
            return level * level + 6 * level;
        } else if (level <= 31) {
            return (int) Math.floor(2.5 * level * level - 40.5 * level + 360);
        } else {
            return (int) Math.floor(4.5 * level * level - 162.5 * level + 2220);
        }
    }
}
