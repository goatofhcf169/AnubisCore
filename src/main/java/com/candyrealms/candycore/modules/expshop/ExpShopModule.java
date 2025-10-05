package com.candyrealms.candycore.modules.expshop;

import com.candyrealms.candycore.CandyCore;
import com.candyrealms.candycore.configuration.ExpCFG;
import com.candyrealms.candycore.utils.ColorUtil;
import com.candyrealms.candycore.utils.ItemCreator;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ExpShopModule {

    private final CandyCore plugin;

    private FileConfiguration config;

    private PaginatedGui gui;

    public ExpShopModule(CandyCore plugin) {
        this.plugin = plugin;

        config = plugin.getExpCFG().getConfig();

        createMenu();
    }

    public void openMenu(Player player) {
        gui.open(player);
    }

    private void createMenu() {
        gui = Gui.paginated()
                .title(Component.text(ColorUtil.color(config.getString("title"))))
                .rows(3)
                .pageSize(2)
                .create();

        gui.setItem(3, 4, ItemBuilder.from(Material.ARROW).setName(ColorUtil.color("&bPrevious")).asGuiItem(event -> gui.previous()));
        gui.setItem(3, 6, ItemBuilder.from(Material.ARROW).setName(ColorUtil.color("&bNext")).asGuiItem(event -> gui.next()));

        gui.getFiller().fillBottom(ItemBuilder.from(new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15)).asGuiItem());
        gui.setDefaultClickAction(event -> event.setCancelled(true));

        ConfigurationSection section = config.getConfigurationSection("items");

        for(String itemID : section.getKeys(false)) {
            String name = section.getString(itemID + ".name");
            Material material = Material.valueOf(section.getString(itemID + ".material"));
            String command = section.getString(itemID + ".command");
            List<String> loreList = section.getStringList(itemID + ".lore");
            int price = section.getInt(itemID + ".price");
            int data = section.getInt(itemID + ".data");

            ItemStack itemStack = new ItemCreator(material, name, 1, data, "", loreList).getItem();

            GuiItem guiItem = new GuiItem(itemStack);
            guiItem.setAction(event -> clickAction(event, price, command));

            gui.addItem(guiItem);
        }
    }

    private void clickAction(InventoryClickEvent event, int price, String command) {
        if(!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        int playerLevel = player.getLevel();

        if(playerLevel < price) {
            player.sendMessage(ColorUtil.color("&5&lEXP &fYou cannot afford this!"));
            player.playSound(player.getLocation(), Sound.VILLAGER_NO, 7, 6);
            return;
        }

        player.setLevel(playerLevel-price);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()));
    }

    public void reload() {
        config = plugin.getExpCFG().getConfig();

        createMenu();
    }
}
