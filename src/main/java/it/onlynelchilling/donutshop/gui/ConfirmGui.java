package it.onlynelchilling.donutshop.gui;

import it.onlynelchilling.donutshop.DonutShop;
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

public class ConfirmGui {

    public static final int SET_MIN_SLOT   = 9;
    public static final int REMOVE_16_SLOT = 11;
    public static final int REMOVE_1_SLOT  = 12;
    public static final int ITEM_SLOT      = 13;
    public static final int ADD_1_SLOT     = 14;
    public static final int ADD_16_SLOT    = 15;
    public static final int SET_MAX_SLOT   = 17;

    public static final int CONFIRM_SLOT = 30;
    public static final int CANCEL_SLOT  = 32;

    public static int getDelta(int slot) {
        return switch (slot) {
            case REMOVE_16_SLOT -> -16;
            case REMOVE_1_SLOT  -> -1;
            case ADD_1_SLOT     -> 1;
            case ADD_16_SLOT    -> 16;
            default -> 0;
        };
    }

    public static boolean isSetSlot(int slot) {
        return slot == SET_MIN_SLOT || slot == SET_MAX_SLOT;
    }

    public static boolean isAmountSlot(int slot) {
        return getDelta(slot) != 0 || isSetSlot(slot);
    }

    public static void open(Player player, ShopItem item, String categoryId, int page) {
        open(player, item, categoryId, page, item.amount());
    }

    public static void open(Player player, ShopItem item, String categoryId, int page, int amount) {
        DonutShop plugin = DonutShop.getInstance();
        MainConfig config = plugin.getMainConfig();
        amount = Math.max(1, Math.min(amount, item.maxStack()));

        GuiHolder holder = new GuiHolder(GuiHolder.GuiType.CONFIRM, categoryId, page);
        holder.setConfirmItem(item);
        holder.setSelectedAmount(amount);

        MessagesConfig msg = plugin.getMessagesConfig();
        Economy economy = plugin.getVaultHook().getEconomy();

        double unitPrice = item.price() / item.amount();
        double totalPrice = unitPrice * amount;

        String priceStr = "shards".equalsIgnoreCase(item.economy())
                ? config.formatShardsPrice(totalPrice)
                : economy.format(totalPrice);

        Inventory inventory = Bukkit.createInventory(holder, 36,
                msg.get("confirm-title").decoration(TextDecoration.ITALIC, false));

        ItemStack filler = config.getFillerItem();
        for (int i = 0; i < 36; i++) {
            inventory.setItem(i, filler);
        }

        if (amount > 1) {
            inventory.setItem(SET_MIN_SLOT, config.getSetToMinItem());
            inventory.setItem(REMOVE_16_SLOT, config.getRemoveStackItem(16));
            inventory.setItem(REMOVE_1_SLOT, config.getRemoveStackItem(1));
        }

        ItemStack preview = new ItemStack(item.material(), Math.min(amount, 64));
        ItemMeta previewMeta = preview.getItemMeta();
        previewMeta.displayName(item.displayName().decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        if (!msg.isEmpty("item-price")) {
            lore.add(msg.get("item-price",
                    Placeholder.unparsed("price", priceStr)
            ).decoration(TextDecoration.ITALIC, false));
        }
        if (!msg.isEmpty("item-amount")) {
            lore.add(msg.get("item-amount",
                    Placeholder.unparsed("amount", String.valueOf(amount))
            ).decoration(TextDecoration.ITALIC, false));
        }
        if (!lore.isEmpty()) {
            previewMeta.lore(lore);
        }

        preview.setItemMeta(previewMeta);
        inventory.setItem(ITEM_SLOT, preview);

        if (amount < item.maxStack()) {
            inventory.setItem(ADD_1_SLOT, config.getAddStackItem(1));
            inventory.setItem(ADD_16_SLOT, config.getAddStackItem(16));
            inventory.setItem(SET_MAX_SLOT, config.getSetToMaxItem(item.maxStack()));
        }

        inventory.setItem(CONFIRM_SLOT, config.getAcceptItem());
        inventory.setItem(CANCEL_SLOT, config.getDeclineItem());

        holder.setInventory(inventory);
        player.openInventory(inventory);
    }

}
