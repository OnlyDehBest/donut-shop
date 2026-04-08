package it.onlynelchilling.donutshop.gui;

import it.onlynelchilling.donutshop.config.ShopItem;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;

public class GuiHolder implements InventoryHolder {

    public enum GuiType {
        MAIN,
        CATEGORY,
        CONFIRM
    }

    private final GuiType type;
    private final String categoryId;
    private final int page;
    private final Map<Integer, Integer> slotToItemIndex = new HashMap<>();
    private ShopItem confirmItem;
    private Inventory inventory;

    private int backSlot = -1;
    private int prevSlot = -1;
    private int nextSlot = -1;
    private int selectedAmount = 1;

    public GuiHolder(GuiType type, String categoryId, int page) {
        this.type = type;
        this.categoryId = categoryId;
        this.page = page;
    }

    public GuiType getType() {
        return type;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public int getPage() {
        return page;
    }

    public Map<Integer, Integer> getSlotToItemIndex() {
        return slotToItemIndex;
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

    public int getBackSlot() {
        return backSlot;
    }

    public void setBackSlot(int backSlot) {
        this.backSlot = backSlot;
    }

    public int getPrevSlot() {
        return prevSlot;
    }

    public void setPrevSlot(int prevSlot) {
        this.prevSlot = prevSlot;
    }

    public int getNextSlot() {
        return nextSlot;
    }

    public void setNextSlot(int nextSlot) {
        this.nextSlot = nextSlot;
    }

    public int getSelectedAmount() {
        return selectedAmount;
    }

    public void setSelectedAmount(int selectedAmount) {
        this.selectedAmount = selectedAmount;
    }
}
