package it.onlynelchilling.donutshop.shop;

import it.onlynelchilling.donutshop.DonutShop;
import it.onlynelchilling.donutshop.config.CustomEconomy;
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
        CustomEconomy custom = config.getCustomEconomy(item.economy());
        boolean isCommandItem = item.commands() != null && !item.commands().isEmpty();

        if (!isCommandItem && !hasSpace(player, item.material(), amount, config.isAntiDupe())) {
            msg.sendPrefixed(player, "inventory-full");
            sounds.play(player, "purchase-error");
            return;
        }

        if (custom != null) {
            if (custom.takeCommand() == null || custom.takeCommand().isEmpty()) {
                plugin.getLogger().severe("Cannot process '" + custom.id() + "' purchase for "
                        + player.getName() + ": take-command is not configured");
                msg.sendPrefixed(player, "transaction-error");
                sounds.play(player, "purchase-error");
                return;
            }

            double balance = getCustomBalance(player, custom, plugin);
            if (balance < 0) {
                msg.sendPrefixed(player, "transaction-error");
                sounds.play(player, "purchase-error");
                return;
            }
            if (config.isDebug()) {
                plugin.getLogger().info("[debug] " + player.getName()
                        + " purchase check: economy='" + custom.id()
                        + "' balance=" + balance
                        + " totalPrice=" + totalPrice
                        + " (unitPrice=" + (item.price() / item.amount())
                        + " * amount=" + amount + ")");
            }
            if (balance < totalPrice) {
                if (config.isDebug()) {
                    plugin.getLogger().info("[debug] insufficient-funds for "
                            + player.getName() + ": balance " + balance
                            + " < required " + totalPrice);
                }
                msg.sendPrefixed(player, "insufficient-funds");
                sounds.play(player, "purchase-error");
                return;
            }
            long roundedPrice = Math.round(totalPrice);
            String takeCmd = custom.takeCommand()
                    .replace("%player%", player.getName())
                    .replace("%amount%", String.valueOf(roundedPrice));
            boolean dispatched = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), takeCmd);
            if (!dispatched) {
                msg.sendPrefixed(player, "transaction-error");
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

        String priceStr = custom != null
                ? custom.formatPrice(totalPrice)
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

    private static double getCustomBalance(Player player, CustomEconomy economy, DonutShop plugin) {
        String placeholder = economy.balancePlaceholder();
        if (placeholder == null || placeholder.isEmpty()) {
            plugin.getLogger().severe("Cannot read '" + economy.id()
                    + "' balance: balance-placeholder is empty in config.yml");
            return -1;
        }
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            plugin.getLogger().severe("Cannot read '" + economy.id()
                    + "' balance: PlaceholderAPI is not installed");
            return -1;
        }

        String result = PlaceholderHook.parse(player, placeholder);
        if (plugin.getMainConfig().isDebug()) {
            plugin.getLogger().info("[debug] placeholder '" + placeholder
                    + "' for " + player.getName() + " resolved to: '" + result + "'");
        }
        if (result.isEmpty() || result.equals(placeholder) || result.contains("%")) {
            plugin.getLogger().warning("Placeholder '" + placeholder + "' for economy '"
                    + economy.id() + "' was not resolved (got '" + result + "'). "
                    + "Check that the expansion providing it is installed and the name is correct.");
            return -1;
        }

        return parseLocalizedNumber(result, plugin, placeholder, economy.id());
    }

    private static double parseLocalizedNumber(String raw, DonutShop plugin, String placeholder, String economyId) {
        String s = raw.trim();

        double multiplier = 1.0;
        if (!s.isEmpty()) {
            String upper = s.toUpperCase();
            if (upper.endsWith("KK")) {
                multiplier = 1_000_000d;
                s = s.substring(0, s.length() - 2);
            } else {
                char last = upper.charAt(upper.length() - 1);
                switch (last) {
                    case 'K' -> multiplier = 1_000d;
                    case 'M' -> multiplier = 1_000_000d;
                    case 'B' -> multiplier = 1_000_000_000d;
                    case 'T' -> multiplier = 1_000_000_000_000d;
                    default -> { }
                }
                if (multiplier != 1.0) {
                    s = s.substring(0, s.length() - 1);
                }
            }
        }

        int lastComma = s.lastIndexOf(',');
        int lastDot = s.lastIndexOf('.');
        String normalized;
        if (lastComma > lastDot) {
            normalized = s.replace(".", "").replace(",", ".");
        } else if (multiplier != 1.0) {
            normalized = s.replace(",", ".");
        } else {
            normalized = s.replace(",", "");
        }
        normalized = normalized.replaceAll("[^\\d.\\-]", "");
        if (normalized.isEmpty()) {
            plugin.getLogger().warning("Placeholder '" + placeholder + "' (economy '"
                    + economyId + "') returned a non-numeric value: '" + raw + "'");
            return -1;
        }
        try {
            return Double.parseDouble(normalized) * multiplier;
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Placeholder '" + placeholder + "' (economy '"
                    + economyId + "') returned a non-numeric value: '" + raw + "'");
            return -1;
        }
    }
}
