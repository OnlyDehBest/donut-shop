package it.onlynelchilling.donutshop.shop;

import it.onlynelchilling.donutshop.DonutShop;
import it.onlynelchilling.donutshop.config.MainConfig;
import it.onlynelchilling.donutshop.config.MessagesConfig;
import it.onlynelchilling.donutshop.config.ShopItem;
import it.onlynelchilling.donutshop.config.SoundConfig;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class PurchaseHandler {

    public static void handle(Player player, ShopItem item, int amount) {
        DonutShop plugin = DonutShop.getInstance();
        MainConfig config = plugin.getMainConfig();
        MessagesConfig msg = plugin.getMessagesConfig();
        SoundConfig sounds = plugin.getSoundConfig();

        double unitPrice = item.price() / item.amount();
        double totalPrice = unitPrice * amount;
        boolean isShards = "shards".equalsIgnoreCase(item.economy());

        if (isShards) {
            double balance = getShardsBalance(player, config);
            if (balance < totalPrice) {
                msg.sendPrefixed(player, "insufficient-funds");
                sounds.play(player, "purchase-error");
                return;
            }
        } else {
            Economy economy = plugin.getVaultHook().getEconomy();
            if (!economy.has(player, totalPrice)) {
                msg.sendPrefixed(player, "insufficient-funds");
                sounds.play(player, "purchase-error");
                return;
            }
        }

        boolean isCommandItem = item.commands() != null && !item.commands().isEmpty();

        if (!isCommandItem) {
            ItemStack stack = new ItemStack(item.material(), amount);
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(stack);

            if (!leftover.isEmpty()) {
                player.getInventory().removeItem(stack);
                msg.sendPrefixed(player, "inventory-full");
                sounds.play(player, "purchase-error");
                return;
            }
        }

        if (isShards) {
            long roundedPrice = Math.round(totalPrice);
            String takeCmd = config.getShardsTakeCommand()
                    .replace("%player%", player.getName())
                    .replace("%amount%", String.valueOf(roundedPrice));
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), takeCmd);
        } else {
            Economy economy = plugin.getVaultHook().getEconomy();
            EconomyResponse response = economy.withdrawPlayer(player, totalPrice);
            if (!response.transactionSuccess()) {
                if (!isCommandItem) {
                    player.getInventory().removeItem(new ItemStack(item.material(), amount));
                }
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
