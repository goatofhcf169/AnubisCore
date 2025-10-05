package com.candyrealms.candycore.modules.revive;

import com.candyrealms.candycore.AnubisCore;
import com.candyrealms.candycore.utils.ColorUtil;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ReviveModule implements Listener {

    private final AnubisCore plugin;
    private final Map<UUID, Deque<DeathRecord>> history = new ConcurrentHashMap<>();
    private final int maxRecords = 10;

    public ReviveModule(AnubisCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        Player p = event.getEntity();
        UUID uuid = p.getUniqueId();

        ItemStack[] contents = cloneItems(p.getInventory().getContents());
        ItemStack[] armor = cloneItems(p.getInventory().getArmorContents());

        String cause = (p.getLastDamageCause() != null) ? p.getLastDamageCause().getCause().name() : "UNKNOWN";
        String killer = (p.getKiller() != null) ? p.getKiller().getName() : "Unknown";

        DeathRecord record = new DeathRecord(System.currentTimeMillis(), cause, killer, p.getLocation().clone(), contents, armor, p.getLevel());

        Deque<DeathRecord> deque = history.computeIfAbsent(uuid, k -> new ArrayDeque<>());
        deque.addFirst(record);
        while (deque.size() > maxRecords) deque.removeLast();
    }

    private ItemStack[] cloneItems(ItemStack[] items) {
        if (items == null) return new ItemStack[0];
        ItemStack[] clone = new ItemStack[items.length];
        for (int i = 0; i < items.length; i++) {
            clone[i] = items[i] == null ? null : items[i].clone();
        }
        return clone;
    }

    public List<DeathRecord> getDeaths(UUID uuid) {
        Deque<DeathRecord> deque = history.getOrDefault(uuid, new ArrayDeque<>());
        return new ArrayList<>(deque);
    }

    public void openDeathsMenu(Player staff, OfflinePlayer target) {
        List<DeathRecord> deaths = getDeaths(target.getUniqueId());
        if (deaths.isEmpty()) {
            staff.sendMessage(ColorUtil.color("&d&lRevive &fNo death history found for &d" + target.getName() + "&f."));
            return;
        }

        PaginatedGui gui = PaginatedGui.paginated()
                .title(Component.text(ColorUtil.color("&8Revive: &d" + target.getName())))
                .rows(6)
                .pageSize(45)
                .create();

        gui.getFiller().fillBottom(ItemBuilder.from(new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15)).asGuiItem());
        gui.setDefaultClickAction(e -> e.setCancelled(true));

        gui.setItem(6, 4, ItemBuilder.from(Material.ARROW).setName(ColorUtil.color("&bPrevious"))
                .asGuiItem(e -> gui.previous()));
        gui.setItem(6, 6, ItemBuilder.from(Material.ARROW).setName(ColorUtil.color("&bNext"))
                .asGuiItem(e -> gui.next()));

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (int i = 0; i < deaths.size(); i++) {
            final int index = i;
            DeathRecord dr = deaths.get(i);
            String time = sdf.format(new Date(dr.getTimestamp()));
            List<String> lore = new ArrayList<>();
            lore.add(ColorUtil.color("&7Time: &f" + time));
            lore.add(ColorUtil.color("&7Cause: &f" + dr.getCause()));
            lore.add(ColorUtil.color("&7Killer: &f" + dr.getKiller()));
            lore.add(ColorUtil.color(String.format("&7Location: &f%s (%.1f, %.1f, %.1f)",
                    dr.getLocation().getWorld().getName(),
                    dr.getLocation().getX(), dr.getLocation().getY(), dr.getLocation().getZ())));
            lore.add(ColorUtil.color(""));
            lore.add(ColorUtil.color("&eClick to view details"));

            GuiItem item = ItemBuilder.from(Material.PAPER)
                    .setName(ColorUtil.color("&dDeath #" + (index + 1)))
                    .setLore(lore)
                    .asGuiItem(e -> openDeathDetails(staff, target, dr, index));

            gui.addItem(item);
        }

        gui.open(staff);
    }

    private void openDeathDetails(Player staff, OfflinePlayer target, DeathRecord record, int index) {
        Gui gui = Gui.gui()
                .title(Component.text(ColorUtil.color("&8Death #" + (index + 1) + " - &d" + target.getName())))
                .rows(6)
                .create();

        gui.setDefaultClickAction(e -> e.setCancelled(true));

        // Armor preview (arranged vertically left)
        ItemStack[] armor = record.getArmor();
        if (armor.length >= 4) {
            gui.setItem(2, 2, ItemBuilder.from(armor[3] == null ? new ItemStack(Material.AIR) : armor[3].clone()).asGuiItem()); // Helmet
            gui.setItem(3, 2, ItemBuilder.from(armor[2] == null ? new ItemStack(Material.AIR) : armor[2].clone()).asGuiItem()); // Chest
            gui.setItem(4, 2, ItemBuilder.from(armor[1] == null ? new ItemStack(Material.AIR) : armor[1].clone()).asGuiItem()); // Legs
            gui.setItem(5, 2, ItemBuilder.from(armor[0] == null ? new ItemStack(Material.AIR) : armor[0].clone()).asGuiItem()); // Boots
        }

        // Inventory preview (first 27 items)
        ItemStack[] contents = record.getContents();
        int slot = 10; // start somewhere central
        for (int i = 0; i < Math.min(contents.length, 27); i++) {
            ItemStack is = contents[i];
            if (is == null || is.getType() == Material.AIR) continue;
            int row = 2 + (i / 9);
            int col = 4 + (i % 9);
            int guiRow = Math.min(6, row);
            int guiCol = Math.min(9, col);
            try {
                gui.setItem(guiRow, guiCol, ItemBuilder.from(is.clone()).asGuiItem());
            } catch (Exception ignored) {
            }
        }

        // Info book
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        List<String> infoLore = Arrays.asList(
                ColorUtil.color("&7Time: &f" + sdf.format(new Date(record.getTimestamp()))),
                ColorUtil.color("&7Cause: &f" + record.getCause()),
                ColorUtil.color("&7Killer: &f" + record.getKiller()),
                ColorUtil.color(String.format("&7Loc: &f%s (%.1f, %.1f, %.1f)",
                        record.getLocation().getWorld().getName(),
                        record.getLocation().getX(), record.getLocation().getY(), record.getLocation().getZ()))
        );
        gui.setItem(2, 8, ItemBuilder.from(Material.BOOK_AND_QUILL).setName(ColorUtil.color("&dDeath Info")).setLore(infoLore).asGuiItem());

        // Buttons
        GuiItem revive = ItemBuilder.from(new ItemStack(Material.WOOL, 1, (short) 5))
                .setName(ColorUtil.color("&a&lRevive Player"))
                .setLore(ColorUtil.color("&7Restore target's items from this death."))
                .asGuiItem(e -> {
                    Player targetPlayer = target.getPlayer();
                    if (targetPlayer == null || !targetPlayer.isOnline()) {
                        staff.sendMessage(ColorUtil.color("&d&lRevive &fTarget &d" + target.getName() + " &fmust be &conline&f."));
                        return;
                    }

                    targetPlayer.getInventory().setContents(cloneItems(record.getContents()));
                    targetPlayer.getInventory().setArmorContents(cloneItems(record.getArmor()));
                    targetPlayer.updateInventory();

                    staff.sendMessage(ColorUtil.color("&d&lRevive &fRestored inventory for &d" + targetPlayer.getName() + " &ffrom death #" + (index + 1) + "."));
                    targetPlayer.sendMessage(ColorUtil.color("&d&lRevive &fYour inventory has been &arestored&f by staff."));

                    e.getWhoClicked().closeInventory();
                });

        GuiItem cancel = ItemBuilder.from(new ItemStack(Material.WOOL, 1, (short) 14))
                .setName(ColorUtil.color("&c&lCancel"))
                .asGuiItem(e -> e.getWhoClicked().closeInventory());

        gui.setItem(6, 4, revive);
        gui.setItem(6, 6, cancel);

        gui.open(staff);
    }
}

