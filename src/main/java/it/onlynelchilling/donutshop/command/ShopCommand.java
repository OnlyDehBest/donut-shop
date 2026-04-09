package it.onlynelchilling.donutshop.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Subcommand;
import it.onlynelchilling.donutshop.DonutShop;
import it.onlynelchilling.donutshop.gui.CategoryGui;
import it.onlynelchilling.donutshop.gui.ShopGui;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("shop")
public class ShopCommand extends BaseCommand {

    @Default
    public void onShop(Player player) {
        ShopGui.open(player);
    }

    @Subcommand("reload")
    @CommandPermission("donutshop.reload")
    public void onReload(CommandSender sender) {
        DonutShop plugin = DonutShop.getInstance();
        ShopGui.invalidate();
        CategoryGui.invalidateAll();
        plugin.getMainConfig().reload();
        plugin.getMessagesConfig().load();
        plugin.getSoundConfig().load();
        plugin.getShopConfig().load();
        plugin.getMessagesConfig().sendPrefixed(sender, "shop-reloaded");
    }
}
