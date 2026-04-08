package it.onlynelchilling.donutshop.config;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;

import java.util.List;

public record ShopItem(Material material, Component displayName, int amount, double price, int maxStack,
                       List<String> commands, String economy) {
}
