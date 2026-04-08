package it.onlynelchilling.donutshop.config;

import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public record Category(String id, int slot, List<ShopItem> items, int rows, Component inventoryName,
                       List<Integer> itemSlots, int backSlot, int nextSlot, int prevSlot,
                       ItemStack cachedIcon) {

    public int itemsPerPage() {
        return itemSlots.size();
    }
}
