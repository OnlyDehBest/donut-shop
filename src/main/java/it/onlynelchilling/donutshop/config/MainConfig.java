package it.onlynelchilling.donutshop.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainConfig {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final int CURRENT_VERSION = 1;

    private final JavaPlugin plugin;
    private final ConfigFile configFile;

    private Component cachedMainTitle;
    private List<String> cachedMainLayout;
    private ItemStack cachedFiller;
    private ItemStack cachedBackPage;
    private ItemStack cachedNextPage;
    private ItemStack cachedRollBackPage;
    private ItemStack cachedDecline;
    private ItemStack cachedAccept;
    private boolean cachedPrintConsole;
    private boolean cachedAntiDupe;
    private String cachedShardsName;
    private String cachedShardsBalancePlaceholder;
    private String cachedShardsTakeCommand;
    private String cachedShardsGiveCommand;
    private ItemStack cachedAdd1;
    private ItemStack cachedAdd16;
    private ItemStack cachedRemove1;
    private ItemStack cachedRemove16;
    private ItemStack cachedSetMin;
    private Material cachedSetMaxMaterial;
    private String cachedSetMaxDisplayName;

    public MainConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        migrateIfNeeded(plugin);
        this.configFile = new ConfigFile(plugin, "config");
        buildCache();
    }

    public void reload() {
        migrateIfNeeded(plugin);
        configFile.reload();
        buildCache();
    }

    private void buildCache() {
        cachedMainTitle = colorize(getConfig().getString("main-inventory.title"));
        cachedMainLayout = List.copyOf(getConfig().getStringList("main-inventory.layout"));
        cachedPrintConsole = getConfig().getBoolean("print-console.enabled");
        cachedAntiDupe = getConfig().getBoolean("anti-dupe", true);

        cachedShardsName = getConfig().getString("shards-economy-name");
        if (cachedShardsName == null) cachedShardsName = "Shards";
        cachedShardsBalancePlaceholder = getConfig().getString("shards-balance-placeholder");
        if (cachedShardsBalancePlaceholder == null) cachedShardsBalancePlaceholder = "";
        cachedShardsTakeCommand = getConfig().getString("shards-take-command");
        if (cachedShardsTakeCommand == null) cachedShardsTakeCommand = "";
        cachedShardsGiveCommand = getConfig().getString("shards-give-command");
        if (cachedShardsGiveCommand == null) cachedShardsGiveCommand = "";

        cachedFiller = buildItem(
                parseMaterial(getConfig().getString("items.filler.type")),
                getConfig().getString("items.filler.display-name"),
                getConfig().getStringList("items.filler.lore"));
        cachedBackPage = buildItem(
                parseMaterial(getConfig().getString("items.back-page.material")),
                getConfig().getString("items.back-page.name"),
                getConfig().getStringList("items.back-page.lore"));
        cachedNextPage = buildItem(
                parseMaterial(getConfig().getString("items.next-page.material")),
                getConfig().getString("items.next-page.name"),
                getConfig().getStringList("items.next-page.lore"));
        cachedRollBackPage = buildItem(
                parseMaterial(getConfig().getString("items.roll-back-page.material")),
                getConfig().getString("items.roll-back-page.name"),
                getConfig().getStringList("items.roll-back-page.lore"));
        cachedDecline = buildItem(
                parseMaterial(getConfig().getString("items.decline.material")),
                getConfig().getString("items.decline.name"),
                getConfig().getStringList("items.decline.lore"));
        cachedAccept = buildItem(
                parseMaterial(getConfig().getString("items.accept.material")),
                getConfig().getString("items.accept.name"),
                getConfig().getStringList("items.accept.lore"));

        cachedAdd1 = buildItem(
                parseMaterial(getConfig().getString("add-stack-item.type")),
                getConfig().getString("add-stack-item.display-name"),
                List.of(), Placeholder.unparsed("amount", "1"));
        cachedAdd16 = buildItem(
                parseMaterial(getConfig().getString("add-stack-item.type")),
                getConfig().getString("add-stack-item.display-name"),
                List.of(), Placeholder.unparsed("amount", "16"));
        cachedRemove1 = buildItem(
                parseMaterial(getConfig().getString("remove-stack-item.type")),
                getConfig().getString("remove-stack-item.display-name"),
                List.of(), Placeholder.unparsed("amount", "1"));
        cachedRemove16 = buildItem(
                parseMaterial(getConfig().getString("remove-stack-item.type")),
                getConfig().getString("remove-stack-item.display-name"),
                List.of(), Placeholder.unparsed("amount", "16"));
        cachedSetMin = buildItem(
                parseMaterial(getConfig().getString("set-to-min-item.type")),
                getConfig().getString("set-to-min-item.display-name"),
                List.of(), Placeholder.unparsed("amount", "1"));
        cachedSetMaxMaterial = parseMaterial(getConfig().getString("set-to-max-item.type"));
        cachedSetMaxDisplayName = getConfig().getString("set-to-max-item.display-name");
    }

    private static void migrateIfNeeded(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "config.yml");
        if (!file.exists()) return;

        YamlConfiguration disk = YamlConfiguration.loadConfiguration(file);
        int diskVersion = disk.getInt("config-version", 0);

        if (diskVersion < CURRENT_VERSION) {
            File backup = new File(plugin.getDataFolder(), "config.yml.old");
            if (backup.exists()) backup.delete();
            file.renameTo(backup);
            plugin.getLogger().info("Config outdated (v" + diskVersion + " → v" + CURRENT_VERSION + "). Regenerated. Old config saved as config.yml.old");
        }
    }

    public YamlConfiguration getConfig() {
        return configFile.getConfig();
    }

    public static Component colorize(String text, TagResolver... resolvers) {
        if (text == null || text.isEmpty()) return Component.empty();
        return MINI.deserialize(convertLegacy(text), resolvers).decoration(TextDecoration.ITALIC, false);
    }

    public static String convertLegacy(String text) {
        if (text == null || text.isEmpty()) return text;

        int len = text.length();
        StringBuilder sb = new StringBuilder(len + 16);

        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);
            if (c != '&' || i + 1 >= len) {
                sb.append(c);
                continue;
            }

            char next = text.charAt(i + 1);

            if (next == '#' && i + 7 < len) {
                String hex = text.substring(i + 2, i + 8);
                if (isHex(hex)) {
                    sb.append("<color:#").append(hex).append('>');
                    i += 7;
                    continue;
                }
            }

            String tag = legacyTag(next);
            if (tag != null) {
                sb.append(tag);
                i++;
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    private static boolean isHex(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'))) return false;
        }
        return true;
    }

    private static String legacyTag(char code) {
        return switch (code) {
            case '0' -> "<black>";
            case '1' -> "<dark_blue>";
            case '2' -> "<dark_green>";
            case '3' -> "<dark_aqua>";
            case '4' -> "<dark_red>";
            case '5' -> "<dark_purple>";
            case '6' -> "<gold>";
            case '7' -> "<gray>";
            case '8' -> "<dark_gray>";
            case '9' -> "<blue>";
            case 'a', 'A' -> "<green>";
            case 'b', 'B' -> "<aqua>";
            case 'c', 'C' -> "<red>";
            case 'd', 'D' -> "<light_purple>";
            case 'e', 'E' -> "<yellow>";
            case 'f', 'F' -> "<white>";
            case 'l', 'L' -> "<bold>";
            case 'o', 'O' -> "<italic>";
            case 'n', 'N' -> "<underlined>";
            case 'm', 'M' -> "<strikethrough>";
            case 'k', 'K' -> "<obfuscated>";
            case 'r', 'R' -> "<reset>";
            default -> null;
        };
    }

    public static List<Component> colorizeList(List<String> list, TagResolver... resolvers) {
        List<Component> result = new ArrayList<>();
        for (String s : list) {
            if (s != null && !s.isEmpty()) {
                result.add(colorize(s, resolvers));
            }
        }
        return result;
    }

    public static ItemStack buildItem(Material material, String name, List<String> loreStrings, TagResolver... resolvers) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(colorize(name, resolvers));
        if (loreStrings != null && !loreStrings.isEmpty()) {
            meta.lore(colorizeList(loreStrings, resolvers));
        }
        item.setItemMeta(meta);
        return item;
    }

    public Component getMainTitle() {
        return cachedMainTitle;
    }

    public List<String> getMainLayout() {
        return cachedMainLayout;
    }

    public boolean isPrintConsole() {
        return cachedPrintConsole;
    }

    public boolean isAntiDupe() {
        return cachedAntiDupe;
    }

    public ItemStack getFillerItem() { return cachedFiller.clone(); }
    public ItemStack getBackPageItem() { return cachedBackPage.clone(); }
    public ItemStack getNextPageItem() { return cachedNextPage.clone(); }
    public ItemStack getRollBackPageItem() { return cachedRollBackPage.clone(); }
    public ItemStack getDeclineItem() { return cachedDecline.clone(); }
    public ItemStack getAcceptItem() { return cachedAccept.clone(); }

    public ItemStack getAddStackItem(int amount) {
        return (amount == 1 ? cachedAdd1 : cachedAdd16).clone();
    }

    public ItemStack getRemoveStackItem(int amount) {
        return (amount == 1 ? cachedRemove1 : cachedRemove16).clone();
    }

    public ItemStack getSetToMinItem() {
        return cachedSetMin.clone();
    }

    public ItemStack getSetToMaxItem(int amount) {
        return buildItem(cachedSetMaxMaterial, cachedSetMaxDisplayName, List.of(),
                Placeholder.unparsed("amount", String.valueOf(amount)));
    }

    public ConfigurationSection getCategorySection(String id) {
        return getConfig().getConfigurationSection("categories." + id);
    }

    public int getCategorySlot(String id) {
        ConfigurationSection s = getCategorySection(id);
        return s != null ? s.getInt("slot") : 0;
    }

    public int getCategoryRows(String id) {
        ConfigurationSection s = getCategorySection(id);
        return s != null ? s.getInt("rows") : 3;
    }

    public List<String> getCategoryLayout(String id) {
        ConfigurationSection s = getCategorySection(id);
        if (s == null) return List.of();
        return s.getStringList("layout");
    }

    public String getCategoryInventoryName(String id) {
        ConfigurationSection s = getCategorySection(id);
        return s != null ? s.getString("inventory-name") : id;
    }

    public String getCategoryIconName(String id) {
        ConfigurationSection s = getCategorySection(id);
        return s != null ? s.getString("icon.display-name") : "";
    }

    public Material getCategoryIconType(String id) {
        ConfigurationSection s = getCategorySection(id);
        if (s == null) return Material.STONE;
        return parseMaterial(s.getString("icon.type"));
    }

    public List<String> getCategoryIconLore(String id) {
        ConfigurationSection s = getCategorySection(id);
        return s != null ? s.getStringList("icon.lore") : List.of();
    }

    private Material parseMaterial(String name) {
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Material.STONE;
        }
    }

    public String getShardsName() { return cachedShardsName; }
    public String getShardsBalancePlaceholder() { return cachedShardsBalancePlaceholder; }
    public String getShardsTakeCommand() { return cachedShardsTakeCommand; }
    public String getShardsGiveCommand() { return cachedShardsGiveCommand; }

    public String formatShardsPrice(double price) {
        return Math.round(price) + " " + cachedShardsName;
    }
}
