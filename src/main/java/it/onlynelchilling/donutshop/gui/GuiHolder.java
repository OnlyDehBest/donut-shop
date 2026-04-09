package it.onlynelchilling.donutshop.gui;

import it.onlynelchilling.donutshop.config.ShopItem;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class GuiHolder implements InventoryHolder {

    private final String categoryId;
    private final int page;
    private ShopItem confirmItem;
    private Inventory inventory;
    private int selectedAmount = 1;
    private boolean locked;

    public boolean isLocked() {
        return locked;
    }

    public void lock() {
        this.locked = true;
    }

    public GuiHolder(String categoryId, int page) {
        this.categoryId = categoryId;
        this.page = page;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public int getPage() {
        return page;
    }

    public ShopItem getConfirmItem() {
        return confirmItem;
    }

    public void setConfirmItem(ShopItem confirmItem) {
        this.confirmItem = confirmItem;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public int getSelectedAmount() {
        return selectedAmount;
    }

    public void setSelectedAmount(int selectedAmount) {
        this.selectedAmount = selectedAmount;
    }
}
