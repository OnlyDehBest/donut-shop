package it.onlynelchilling.donutshop.gui;

import it.onlynelchilling.donutshop.DonutShop;
import it.onlynelchilling.donutshop.config.Category;
import it.onlynelchilling.donutshop.config.MainConfig;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class ShopGui {

    private static final String CACHE_KEY = "main";
    private static final Cache<String, Inventory> cache = Caffeine.newBuilder().build();

    public static void invalidate() {
        cache.invalidateAll();
    }

    public static void open(Player player) {
        Inventory inventory = cache.get(CACHE_KEY, k -> build());
        DonutShop.getInstance().getSoundConfig().play(player, "open-shop");
        player.openInventory(inventory);
    }

    private static Inventory build() {
        DonutShop plugin = DonutShop.getInstance();
        MainConfig config = plugin.getMainConfig();
        List<String> layout = config.getMainLayout();
        int size = layout.size() * 9;

        GuiHolder holder = new GuiHolder(null, 0);
        Inventory inventory = Bukkit.createInventory(holder, size, config.getMainTitle());

        ItemStack filler = config.getFillerItem();
        for (int i = 0; i < size; i++) {
            inventory.setItem(i, filler);
        }

        for (Category category : plugin.getShopConfig().getCategories().values()) {
            ItemStack icon = category.cachedIcon().clone();
            ItemMeta meta = icon.getItemMeta();
            meta.getPersistentDataContainer().set(GuiKeys.ACTION, PersistentDataType.STRING, GuiKeys.OPEN_CATEGORY);
            meta.getPersistentDataContainer().set(GuiKeys.CATEGORY_ID, PersistentDataType.STRING, category.id());
            icon.setItemMeta(meta);
            inventory.setItem(category.slot(), icon);
        }

        holder.setInventory(inventory);
        return inventory;
    }
}
