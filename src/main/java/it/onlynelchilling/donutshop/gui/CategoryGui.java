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

import java.util.ArrayList;
import java.util.List;

public class CategoryGui {

    public static void open(Player player, Category category, int page) {
        DonutShop plugin = DonutShop.getInstance();
        MainConfig config = plugin.getMainConfig();
        int size = category.rows() * 9;

        List<Integer> itemSlots = category.itemSlots();
        int backSlot = category.backSlot();
        int nextSlot = category.nextSlot();
        int prevSlot = category.prevSlot();

        int itemsPerPage = category.itemsPerPage();
        int totalPages = Math.max(1, (int) Math.ceil(category.items().size() / (double) itemsPerPage));
        page = Math.max(0, Math.min(page, totalPages - 1));

        GuiHolder holder = new GuiHolder(GuiHolder.GuiType.CATEGORY, category.id(), page);
        holder.setBackSlot(backSlot);
        holder.setNextSlot(nextSlot);
        holder.setPrevSlot(prevSlot);

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

            stack.setItemMeta(meta);
            inventory.setItem(slot, stack);
            holder.getSlotToItemIndex().put(slot, i);
        }

        if (backSlot >= 0) {
            inventory.setItem(backSlot, config.getBackPageItem());
        }

        if (page > 0 && prevSlot >= 0) {
            inventory.setItem(prevSlot, config.getRollBackPageItem());
        }

        if (nextSlot >= 0 && page < totalPages - 1) {
            inventory.setItem(nextSlot, config.getNextPageItem());
        }

        holder.setInventory(inventory);
        player.openInventory(inventory);
    }
}
