package it.onlynelchilling.donutshop.shop;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

public class PlaceholderHook {

    public static String parse(Player player, String placeholder) {
        return PlaceholderAPI.setPlaceholders(player, placeholder);
    }
}

