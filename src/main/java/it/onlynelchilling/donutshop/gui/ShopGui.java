package it.onlynelchilling.donutshop.gui;

import it.onlynelchilling.donutshop.DonutShop;
import it.onlynelchilling.donutshop.config.Category;
import it.onlynelchilling.donutshop.config.MainConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ShopGui {

    public static void open(Player player) {
        DonutShop plugin = DonutShop.getInstance();
        MainConfig config = plugin.getMainConfig();
        List<String> layout = config.getMainLayout();
        int size = layout.size() * 9;

        GuiHolder holder = new GuiHolder(GuiHolder.GuiType.MAIN, null, 0);

        Inventory inventory = Bukkit.createInventory(holder, size, config.getMainTitle());

        ItemStack filler = config.getFillerItem();
        for (int i = 0; i < size; i++) {
            inventory.setItem(i, filler);
        }

        for (Category category : plugin.getShopConfig().getCategories().values()) {
            inventory.setItem(category.slot(), category.cachedIcon().clone());
        }

        holder.setInventory(inventory);
        plugin.getSoundConfig().play(player, "open-shop");
        player.openInventory(inventory);
    }
}
