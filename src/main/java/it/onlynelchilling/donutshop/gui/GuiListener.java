package it.onlynelchilling.donutshop.gui;

import it.onlynelchilling.donutshop.DonutShop;
import it.onlynelchilling.donutshop.config.Category;
import it.onlynelchilling.donutshop.config.ShopItem;
import it.onlynelchilling.donutshop.shop.PurchaseHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;

public class GuiListener implements Listener {

    private final DonutShop plugin = DonutShop.getInstance();

    private record ClickContext(String categoryId, Integer itemIndex, Integer delta) {}

    @FunctionalInterface
    private interface ActionHandler {
        void handle(Player player, GuiHolder holder, ClickContext ctx);
    }

    private final Map<String, ActionHandler> actionHandlers = Map.ofEntries(
            Map.entry(GuiKeys.OPEN_CATEGORY, this::handleOpenCategory),
            Map.entry(GuiKeys.BACK,           this::handleBack),
            Map.entry(GuiKeys.PREV_PAGE,      this::handlePrevPage),
            Map.entry(GuiKeys.NEXT_PAGE,      this::handleNextPage),
            Map.entry(GuiKeys.SELECT_ITEM,    this::handleSelectItem),
            Map.entry(GuiKeys.CONFIRM,        this::handleConfirm),
            Map.entry(GuiKeys.CANCEL,         this::handleCancel),
            Map.entry(GuiKeys.SET_MIN,        this::handleSetMin),
            Map.entry(GuiKeys.SET_MAX,        this::handleSetMax),
            Map.entry(GuiKeys.CHANGE_AMOUNT,  this::handleChangeAmount)
    );

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder(false) instanceof GuiHolder holder)) return;
        event.setCancelled(true);
        if (holder.isLocked()) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        int rawSlot = event.getRawSlot();
        if (rawSlot < 0 || rawSlot >= event.getInventory().getSize()) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        PersistentDataContainer pdc = clicked.getItemMeta().getPersistentDataContainer();
        String action = pdc.get(GuiKeys.ACTION, PersistentDataType.STRING);
        if (action == null) return;

        ActionHandler handler = actionHandlers.get(action);
        if (handler == null) return;

        ClickContext ctx = new ClickContext(
                pdc.get(GuiKeys.CATEGORY_ID, PersistentDataType.STRING),
                pdc.get(GuiKeys.ITEM_INDEX,  PersistentDataType.INTEGER),
                pdc.get(GuiKeys.DELTA,       PersistentDataType.INTEGER)
        );

        handler.handle(player, holder, ctx);
    }

    private void handleOpenCategory(Player player, GuiHolder holder, ClickContext ctx) {
        if (ctx.categoryId() == null) return;
        Category category = plugin.getShopConfig().getCategories().get(ctx.categoryId());
        if (category != null) {
            plugin.getSoundConfig().play(player, "open-category");
            CategoryGui.open(player, category, 0);
        }
    }

    private void handleBack(Player player, GuiHolder holder, ClickContext ctx) {
        ShopGui.open(player);
    }

    private void handlePrevPage(Player player, GuiHolder holder, ClickContext ctx) {
        Category category = getCategory(holder);
        if (category != null && holder.getPage() > 0) {
            plugin.getSoundConfig().play(player, "navigate-page");
            CategoryGui.open(player, category, holder.getPage() - 1);
        }
    }

    private void handleNextPage(Player player, GuiHolder holder, ClickContext ctx) {
        Category category = getCategory(holder);
        if (category != null) {
            int totalPages = (int) Math.ceil(category.items().size() / (double) category.itemsPerPage());
            if (holder.getPage() < totalPages - 1) {
                plugin.getSoundConfig().play(player, "navigate-page");
                CategoryGui.open(player, category, holder.getPage() + 1);
            }
        }
    }

    private void handleSelectItem(Player player, GuiHolder holder, ClickContext ctx) {
        if (ctx.itemIndex() == null) return;
        Category category = getCategory(holder);
        if (category != null && ctx.itemIndex() < category.items().size()) {
            ShopItem item = category.items().get(ctx.itemIndex());
            plugin.getSoundConfig().play(player, "select-item");
            ConfirmGui.open(player, item, holder.getCategoryId(), holder.getPage());
        }
    }

    private void handleConfirm(Player player, GuiHolder holder, ClickContext ctx) {
        ShopItem item = holder.getConfirmItem();
        if (item == null) return;
        holder.lock();
        int amount = holder.getSelectedAmount();
        String categoryId = holder.getCategoryId();
        int page = holder.getPage();
        PurchaseHandler.handle(player, item, amount);
        if (plugin.getMainConfig().isKeepOpenAfterPurchase()) {
            ConfirmGui.open(player, item, categoryId, page, amount);
        } else {
            Category category = getCategory(holder);
            if (category != null) {
                CategoryGui.open(player, category, page);
            } else {
                ShopGui.open(player);
            }
        }
    }

    private void handleCancel(Player player, GuiHolder holder, ClickContext ctx) {
        plugin.getSoundConfig().play(player, "cancel-purchase");
        Category category = getCategory(holder);
        if (category != null) {
            CategoryGui.open(player, category, holder.getPage());
        } else {
            ShopGui.open(player);
        }
    }

    private void handleSetMin(Player player, GuiHolder holder, ClickContext ctx) {
        ShopItem item = holder.getConfirmItem();
        if (item == null) return;
        updateAmount(player, holder, item, 1);
    }

    private void handleSetMax(Player player, GuiHolder holder, ClickContext ctx) {
        ShopItem item = holder.getConfirmItem();
        if (item == null) return;
        updateAmount(player, holder, item, item.maxStack());
    }

    private void handleChangeAmount(Player player, GuiHolder holder, ClickContext ctx) {
        ShopItem item = holder.getConfirmItem();
        if (item == null || ctx.delta() == null) return;
        int newAmount = Math.max(1, Math.min(holder.getSelectedAmount() + ctx.delta(), item.maxStack()));
        updateAmount(player, holder, item, newAmount);
    }

    private void updateAmount(Player player, GuiHolder holder, ShopItem item, int newAmount) {
        if (newAmount != holder.getSelectedAmount()) {
            plugin.getSoundConfig().play(player, "select-item");
            ConfirmGui.open(player, item, holder.getCategoryId(), holder.getPage(), newAmount);
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
