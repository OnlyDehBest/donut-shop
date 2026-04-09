package it.onlynelchilling.donutshop.gui;

import it.onlynelchilling.donutshop.DonutShop;
import it.onlynelchilling.donutshop.config.Category;
import it.onlynelchilling.donutshop.config.MainConfig;
import it.onlynelchilling.donutshop.config.MessagesConfig;
import it.onlynelchilling.donutshop.config.ShopItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CategoryGui {

    private static final Map<String, Inventory> cache = new HashMap<>();

    public static void invalidateAll() {
        cache.clear();
    }

    public static void open(Player player, Category category, int page) {
        int itemsPerPage = category.itemsPerPage();
        int totalPages = Math.max(1, (int) Math.ceil(category.items().size() / (double) itemsPerPage));
        page = Math.max(0, Math.min(page, totalPages - 1));

        String key = category.id() + ":" + page;
        Inventory inventory = cache.get(key);
        if (inventory == null) {
            inventory = build(category, page, totalPages);
            cache.put(key, inventory);
        }
        player.openInventory(inventory);
    }

    private static Inventory build(Category category, int page, int totalPages) {
        DonutShop plugin = DonutShop.getInstance();
        MainConfig config = plugin.getMainConfig();
        int size = category.rows() * 9;

        List<Integer> itemSlots = category.itemSlots();
        int backSlot = category.backSlot();
        int nextSlot = category.nextSlot();
        int prevSlot = category.prevSlot();
        int itemsPerPage = category.itemsPerPage();

        GuiHolder holder = new GuiHolder(category.id(), page);

        MessagesConfig msg = plugin.getMessagesConfig();
        Economy economy = plugin.getVaultHook().getEconomy();

        Inventory inventory = Bukkit.createInventory(holder, size,
                category.inventoryName().decoration(TextDecoration.ITALIC, false));

        ItemStack filler = config.getFillerItem();
        for (int i = 0; i < size; i++) {
            inventory.setItem(i, filler);
        }

        int start = page * itemsPerPage;
        int end = Math.min(start + itemsPerPage, category.items().size());

        for (int i = start; i < end; i++) {
            int slotIndex = i - start;
            int slot = itemSlots.get(slotIndex);
            ShopItem item = category.items().get(i);
            ItemStack stack = new ItemStack(item.material(), item.amount());
            ItemMeta meta = stack.getItemMeta();
            meta.displayName(item.displayName().decoration(TextDecoration.ITALIC, false));

            String priceStr = "shards".equalsIgnoreCase(item.economy())
                    ? config.formatShardsPrice(item.price())
                    : economy.format(item.price());

            List<Component> lore = new ArrayList<>();
            if (!msg.isEmpty("item-price")) {
                lore.add(msg.get("item-price",
                        Placeholder.unparsed("price", priceStr)
                ).decoration(TextDecoration.ITALIC, false));
            }
            if (!msg.isEmpty("item-amount")) {
                lore.add(msg.get("item-amount",
                        Placeholder.unparsed("amount", String.valueOf(item.amount()))
                ).decoration(TextDecoration.ITALIC, false));
            }
            if (!msg.isEmpty("item-click")) {
                lore.add(Component.empty());
                lore.add(msg.get("item-click").decoration(TextDecoration.ITALIC, false));
            }
            if (!lore.isEmpty()) {
                meta.lore(lore);
            }

            meta.getPersistentDataContainer().set(GuiKeys.ACTION, PersistentDataType.STRING, GuiKeys.SELECT_ITEM);
            meta.getPersistentDataContainer().set(GuiKeys.ITEM_INDEX, PersistentDataType.INTEGER, i);

            stack.setItemMeta(meta);
            inventory.setItem(slot, stack);
        }

        if (backSlot >= 0) {
            ItemStack backItem = config.getBackPageItem();
            ItemMeta backMeta = backItem.getItemMeta();
            backMeta.getPersistentDataContainer().set(GuiKeys.ACTION, PersistentDataType.STRING, GuiKeys.BACK);
            backItem.setItemMeta(backMeta);
            inventory.setItem(backSlot, backItem);
        }

        if (page > 0 && prevSlot >= 0) {
            ItemStack prevItem = config.getRollBackPageItem();
            ItemMeta prevMeta = prevItem.getItemMeta();
            prevMeta.getPersistentDataContainer().set(GuiKeys.ACTION, PersistentDataType.STRING, GuiKeys.PREV_PAGE);
            prevItem.setItemMeta(prevMeta);
            inventory.setItem(prevSlot, prevItem);
        }

        if (nextSlot >= 0 && page < totalPages - 1) {
            ItemStack nextItem = config.getNextPageItem();
            ItemMeta nextMeta = nextItem.getItemMeta();
            nextMeta.getPersistentDataContainer().set(GuiKeys.ACTION, PersistentDataType.STRING, GuiKeys.NEXT_PAGE);
            nextItem.setItemMeta(nextMeta);
            inventory.setItem(nextSlot, nextItem);
        }

        holder.setInventory(inventory);
        return inventory;
    }
}
