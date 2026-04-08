package it.onlynelchilling.donutshop.gui;

import it.onlynelchilling.donutshop.DonutShop;
import it.onlynelchilling.donutshop.config.Category;
import it.onlynelchilling.donutshop.config.ShopItem;
import it.onlynelchilling.donutshop.config.SoundConfig;
import it.onlynelchilling.donutshop.shop.PurchaseHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class GuiListener implements Listener {

    private final DonutShop plugin = DonutShop.getInstance();

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder(false) instanceof GuiHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        int rawSlot = event.getRawSlot();
        if (rawSlot < 0 || rawSlot >= event.getInventory().getSize()) return;

        switch (holder.getType()) {
            case MAIN -> {
                Category category = plugin.getShopConfig().getCategoryBySlot(rawSlot);
                if (category != null) {
                    plugin.getSoundConfig().play(player, "open-category");
                    CategoryGui.open(player, category, 0);
                }
            }
            case CATEGORY -> handleCategoryClick(player, holder, rawSlot);
            case CONFIRM -> handleConfirmClick(player, holder, rawSlot);
        }
    }

    private void handleCategoryClick(Player player, GuiHolder holder, int rawSlot) {
        SoundConfig sounds = plugin.getSoundConfig();

        if (rawSlot == holder.getBackSlot()) {
            ShopGui.open(player);
            return;
        }

        if (rawSlot == holder.getPrevSlot() && holder.getPage() > 0) {
            Category category = getCategory(holder);
            if (category != null) {
                sounds.play(player, "navigate-page");
                CategoryGui.open(player, category, holder.getPage() - 1);
            }
            return;
        }

        if (rawSlot == holder.getNextSlot()) {
            Category category = getCategory(holder);
            if (category != null) {
                int totalPages = (int) Math.ceil(category.items().size() / (double) category.itemsPerPage());
                if (holder.getPage() < totalPages - 1) {
                    sounds.play(player, "navigate-page");
                    CategoryGui.open(player, category, holder.getPage() + 1);
                }
            }
            return;
        }

        Integer itemIndex = holder.getSlotToItemIndex().get(rawSlot);
        if (itemIndex != null) {
            Category category = getCategory(holder);
            if (category != null && itemIndex < category.items().size()) {
                ShopItem item = category.items().get(itemIndex);
                sounds.play(player, "select-item");
                ConfirmGui.open(player, item, holder.getCategoryId(), holder.getPage());
            }
        }
    }

    private void handleConfirmClick(Player player, GuiHolder holder, int rawSlot) {
        SoundConfig sounds = plugin.getSoundConfig();
        ShopItem item = holder.getConfirmItem();
        if (item == null) return;

        if (ConfirmGui.isAmountSlot(rawSlot)) {
            int newAmount;
            if (ConfirmGui.isSetSlot(rawSlot)) {
                newAmount = (rawSlot == ConfirmGui.SET_MIN_SLOT) ? 1 : item.maxStack();
            } else {
                int delta = ConfirmGui.getDelta(rawSlot);
                newAmount = Math.max(1, Math.min(holder.getSelectedAmount() + delta, item.maxStack()));
            }
            if (newAmount != holder.getSelectedAmount()) {
                sounds.play(player, "select-item");
                ConfirmGui.open(player, item, holder.getCategoryId(), holder.getPage(), newAmount);
            }
            return;
        }

        if (rawSlot == ConfirmGui.CONFIRM_SLOT) {
            PurchaseHandler.handle(player, item, holder.getSelectedAmount());
            Category category = getCategory(holder);
            if (category != null) {
                CategoryGui.open(player, category, holder.getPage());
            } else {
                ShopGui.open(player);
            }
            return;
        }

        if (rawSlot == ConfirmGui.CANCEL_SLOT) {
            sounds.play(player, "cancel-purchase");
            Category category = getCategory(holder);
            if (category != null) {
                CategoryGui.open(player, category, holder.getPage());
            } else {
                ShopGui.open(player);
            }
        }
    }

    private Category getCategory(GuiHolder holder) {
        return plugin.getShopConfig().getCategories().get(holder.getCategoryId());
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder(false) instanceof GuiHolder) {
            event.setCancelled(true);
        }
    }
}
