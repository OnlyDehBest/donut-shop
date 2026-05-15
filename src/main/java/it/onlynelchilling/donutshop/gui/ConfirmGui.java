package it.onlynelchilling.donutshop.gui;

import it.onlynelchilling.donutshop.DonutShop;
import it.onlynelchilling.donutshop.config.CustomEconomy;
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
import java.util.List;

public class ConfirmGui {

    public static void open(Player player, ShopItem item, String categoryId, int page) {
        open(player, item, categoryId, page, item.amount());
    }

    public static void open(Player player, ShopItem item, String categoryId, int page, int amount) {
        DonutShop plugin = DonutShop.getInstance();
        MainConfig config = plugin.getMainConfig();
        amount = Math.max(1, Math.min(amount, item.maxStack()));

        GuiHolder holder = new GuiHolder(categoryId, page);
        holder.setConfirmItem(item);
        holder.setSelectedAmount(amount);

        MessagesConfig msg = plugin.getMessagesConfig();
        Economy economy = plugin.getVaultHook().getEconomy();

        double unitPrice = item.price() / item.amount();
        double totalPrice = unitPrice * amount;

        String priceStr;
        CustomEconomy custom = config.getCustomEconomy(item.economy());
        if (custom != null) {
            priceStr = custom.formatPrice(totalPrice);
        } else {
            priceStr = economy.format(totalPrice);
        }

        Inventory inventory = Bukkit.createInventory(holder, 36,
                msg.get("confirm-title").decoration(TextDecoration.ITALIC, false));

        ItemStack filler = config.getFillerItem();
        for (int i = 0; i < 36; i++) {
            inventory.setItem(i, filler);
        }

        if (amount > 1) {
            inventory.setItem(9, tagAction(config.getSetToMinItem(), GuiKeys.SET_MIN));
            inventory.setItem(11, tagDelta(config.getRemoveStackItem(16), -16));
            inventory.setItem(12, tagDelta(config.getRemoveStackItem(1), -1));
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
        inventory.setItem(13, preview);

        if (amount < item.maxStack()) {
            inventory.setItem(14, tagDelta(config.getAddStackItem(1), 1));
            inventory.setItem(15, tagDelta(config.getAddStackItem(16), 16));
            inventory.setItem(17, tagAction(config.getSetToMaxItem(item.maxStack()), GuiKeys.SET_MAX));
        }

        inventory.setItem(30, tagAction(config.getAcceptItem(), GuiKeys.CONFIRM));
        inventory.setItem(32, tagAction(config.getDeclineItem(), GuiKeys.CANCEL));

        holder.setInventory(inventory);
        player.openInventory(inventory);
    }

    private static ItemStack tagAction(ItemStack stack, String action) {
        ItemMeta meta = stack.getItemMeta();
        meta.getPersistentDataContainer().set(GuiKeys.ACTION, PersistentDataType.STRING, action);
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack tagDelta(ItemStack stack, int delta) {
        ItemMeta meta = stack.getItemMeta();
        meta.getPersistentDataContainer().set(GuiKeys.ACTION, PersistentDataType.STRING, GuiKeys.CHANGE_AMOUNT);
        meta.getPersistentDataContainer().set(GuiKeys.DELTA, PersistentDataType.INTEGER, delta);
        stack.setItemMeta(meta);
        return stack;
    }
}
