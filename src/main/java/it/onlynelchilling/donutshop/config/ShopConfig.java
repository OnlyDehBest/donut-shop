package it.onlynelchilling.donutshop.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ShopConfig {

    private static final String[] DEFAULT_CATEGORIES = {
            "shard.yml", "end.yml", "food.yml", "gear.yml", "nether.yml"
    };

    private final JavaPlugin plugin;
    private final MainConfig mainConfig;
    private final Map<String, Category> categories = new LinkedHashMap<>();

    public ShopConfig(JavaPlugin plugin, MainConfig mainConfig) {
        this.plugin = plugin;
        this.mainConfig = mainConfig;
        saveDefaults();
        load();
    }

    private void saveDefaults() {
        File categoriesDir = new File(plugin.getDataFolder(), "categories");
        if (!categoriesDir.exists()) {
            categoriesDir.mkdirs();
        }
        for (String name : DEFAULT_CATEGORIES) {
            File file = new File(categoriesDir, name);
            if (!file.exists()) {
                plugin.saveResource("categories/" + name, false);
            }
        }
    }

    public void load() {
        categories.clear();
        File categoriesDir = new File(plugin.getDataFolder(), "categories");
        if (!categoriesDir.exists() || !categoriesDir.isDirectory()) return;

        File[] files = categoriesDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            try {
                String categoryId = file.getName().replace(".yml", "");

                if (!mainConfig.isCategoryEnabled(categoryId)) {
                    continue;
                }

                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

                Material icon = mainConfig.getCategoryIconType(categoryId);
                Component displayName = MainConfig.colorize(mainConfig.getCategoryIconName(categoryId));
                int slot = mainConfig.getCategorySlot(categoryId);

                List<ShopItem> items = new ArrayList<>();
                List<Map<?, ?>> itemsList = config.getMapList("items");

                for (Map<?, ?> map : itemsList) {
                    try {
                        Material material = Material.valueOf(((String) map.get("material")).toUpperCase());
                        String rawName = (String) map.get("display-name");
                        var itemName = MainConfig.colorize(rawName != null ? rawName : material.name());
                        int amount = map.containsKey("amount") ? ((Number) map.get("amount")).intValue() : 1;
                        double price = map.containsKey("price") ? ((Number) map.get("price")).doubleValue() : 0.0;
                        int maxStack = map.containsKey("max-stack") ? ((Number) map.get("max-stack")).intValue() : 64;

                        List<String> commands = new ArrayList<>();
                        if (map.containsKey("commands") && map.get("commands") instanceof List<?> cmdList) {
                            for (Object cmd : cmdList) {
                                commands.add(String.valueOf(cmd));
                            }
                        }

                        String economy = map.containsKey("economy") ? String.valueOf(map.get("economy")) : "vault";

                        items.add(new ShopItem(material, itemName, amount, price, maxStack, List.copyOf(commands), economy));
                    } catch (Exception e) {
                        plugin.getLogger().warning("Invalid item in " + file.getName() + ": " + e.getMessage());
                    }
                }

                int rows = mainConfig.getCategoryRows(categoryId);
                List<String> layout = mainConfig.getCategoryLayout(categoryId);
                List<Component> iconLore = MainConfig.colorizeList(mainConfig.getCategoryIconLore(categoryId));
                Component inventoryName = MainConfig.colorize(mainConfig.getCategoryInventoryName(categoryId));

                List<Integer> itemSlots = new ArrayList<>();
                int backSlot = -1;
                int nextSlot = -1;
                for (int row = 0; row < layout.size(); row++) {
                    String line = layout.get(row);
                    for (int col = 0; col < Math.min(line.length(), 9); col++) {
                        int s = row * 9 + col;
                        char c = line.charAt(col);
                        switch (c) {
                            case 'O' -> itemSlots.add(s);
                            case '<' -> backSlot = s;
                            case '>' -> nextSlot = s;
                        }
                    }
                }
                int prevSlot = backSlot >= 0 ? backSlot + 1 : -1;

                ItemStack cachedIcon = new ItemStack(icon);
                ItemMeta iconMeta = cachedIcon.getItemMeta();
                iconMeta.displayName(displayName.decoration(TextDecoration.ITALIC, false));
                if (!iconLore.isEmpty()) {
                    iconMeta.lore(iconLore);
                }
                cachedIcon.setItemMeta(iconMeta);

                Category category = new Category(
                        categoryId, slot, List.copyOf(items),
                        rows, inventoryName,
                        List.copyOf(itemSlots), backSlot, nextSlot, prevSlot,
                        cachedIcon
                );

                categories.put(categoryId, category);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load category " + file.getName() + ": " + e.getMessage());
            }
        }
    }

    public Map<String, Category> getCategories() {
        return categories;
    }
}
