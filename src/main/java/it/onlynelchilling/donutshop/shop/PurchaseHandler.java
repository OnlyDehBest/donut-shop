package it.onlynelchilling.donutshop.shop;

import it.onlynelchilling.donutshop.DonutShop;
import it.onlynelchilling.donutshop.config.MainConfig;
import it.onlynelchilling.donutshop.config.MessagesConfig;
import it.onlynelchilling.donutshop.config.ShopItem;
import it.onlynelchilling.donutshop.config.SoundConfig;
import it.onlynelchilling.donutshop.gui.GuiKeys;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class PurchaseHandler {

    public static void handle(Player player, ShopItem item, int amount) {
        DonutShop plugin = DonutShop.getInstance();
        MainConfig config = plugin.getMainConfig();
        MessagesConfig msg = plugin.getMessagesConfig();
        SoundConfig sounds = plugin.getSoundConfig();

        double unitPrice = item.price() / item.amount();
        double totalPrice = unitPrice * amount;
        boolean isShards = "shards".equalsIgnoreCase(item.economy());
        boolean isCommandItem = item.commands() != null && !item.commands().isEmpty();

        if (!isCommandItem && !hasSpace(player, item.material(), amount, config.isAntiDupe())) {
            msg.sendPrefixed(player, "inventory-full");
            sounds.play(player, "purchase-error");
            return;
        }

        if (isShards) {
            double balance = getShardsBalance(player, config);
            if (balance < totalPrice) {
                msg.sendPrefixed(player, "insufficient-funds");
                sounds.play(player, "purchase-error");
                return;
            }
            long roundedPrice = Math.round(totalPrice);
            String takeCmd = config.getShardsTakeCommand()
                    .replace("%player%", player.getName())
                    .replace("%amount%", String.valueOf(roundedPrice));
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), takeCmd);
        } else {
            Economy economy = plugin.getVaultHook().getEconomy();
            if (!economy.has(player, totalPrice)) {
                msg.sendPrefixed(player, "insufficient-funds");
                sounds.play(player, "purchase-error");
                return;
            }
            EconomyResponse response = economy.withdrawPlayer(player, totalPrice);
            if (!response.transactionSuccess()) {
                msg.sendPrefixed(player, "transaction-error");
                sounds.play(player, "purchase-error");
                return;
            }
        }

        if (isCommandItem) {
            int times = amount / Math.max(1, item.amount());
            for (int i = 0; i < times; i++) {
                for (String cmd : item.commands()) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", player.getName()));
                }
            }
        } else {
            ItemStack stack = new ItemStack(item.material(), amount);
            if (config.isAntiDupe()) {
                ItemMeta meta = stack.getItemMeta();
                meta.getPersistentDataContainer().set(
                        GuiKeys.SHOP_STAMP, PersistentDataType.BOOLEAN, true
                );
                stack.setItemMeta(meta);
            }
            player.getInventory().addItem(stack);
        }

        String priceStr = isShards
                ? config.formatShardsPrice(totalPrice)
                : plugin.getVaultHook().getEconomy().format(totalPrice);

        msg.sendPrefixed(player, "purchase-success",
                Placeholder.unparsed("amount", String.valueOf(amount)),
                Placeholder.component("item", item.displayName()),
                Placeholder.unparsed("price", priceStr)
        );
        sounds.play(player, "confirm-purchase");

        if (config.isPrintConsole()) {
            plugin.getLogger().info(
                    player.getName() + " purchased " + amount + "x " + item.material().name()
                            + " for " + priceStr
            );
        }
    }

    private static boolean hasSpace(Player player, Material material, int amount, boolean antiDupe) {
        int remaining = amount;
        int maxStack = material.getMaxStackSize();
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (stack == null || stack.getType() == Material.AIR) {
                remaining -= maxStack;
            } else if (stack.getType() == material && stack.getAmount() < maxStack) {
                if (antiDupe) {
                    if (stack.hasItemMeta()
                            && stack.getItemMeta().getPersistentDataContainer().has(GuiKeys.SHOP_STAMP)) {
                        remaining -= (maxStack - stack.getAmount());
                    }
                } else {
                    remaining -= (maxStack - stack.getAmount());
                }
            }
            if (remaining <= 0) return true;
        }
        return false;
    }

    private static double getShardsBalance(Player player, MainConfig config) {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            String placeholder = config.getShardsBalancePlaceholder();
            if (!placeholder.isEmpty()) {
                String result = PlaceholderHook.parse(player, placeholder);
                try {
                    return Double.parseDouble(result.replaceAll("[^\\d.]", ""));
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }
        return Double.MAX_VALUE;
    }
}
