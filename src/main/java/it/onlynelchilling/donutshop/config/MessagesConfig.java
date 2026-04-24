package it.onlynelchilling.donutshop.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class MessagesConfig {

    private final ConfigFile configFile;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Map<String, String> messageCache = new HashMap<>();
    private String prefix = "";

    public MessagesConfig(JavaPlugin plugin) {
        this.configFile = new ConfigFile(plugin, "messages");
        loadValues();
    }

    public void load() {
        configFile.reload();
        loadValues();
    }

    private void loadValues() {
        messageCache.clear();
        prefix = configFile.getConfig().getString("prefix");
        if (prefix == null) prefix = "";
        else prefix = MainConfig.convertLegacy(prefix);

        for (String key : configFile.getConfig().getKeys(true)) {
            if (configFile.getConfig().isString(key)) {
                String raw = configFile.getConfig().getString(key);
                if (raw != null && !raw.isEmpty()) {
                    messageCache.put(key, MainConfig.convertLegacy(raw));
                }
            }
        }
    }

    public boolean isEmpty(String key) {
        return !messageCache.containsKey(key);
    }

    public Component get(String key, TagResolver... resolvers) {
        String converted = messageCache.get(key);
        if (converted == null) return Component.empty();
        return miniMessage.deserialize(converted, resolvers);
    }

    public Component getPrefixed(String key, TagResolver... resolvers) {
        String converted = messageCache.get(key);
        if (converted == null) return Component.empty();
        String raw = prefix.isEmpty() ? converted : prefix + " " + converted;
        return miniMessage.deserialize(raw, resolvers);
    }

    public void send(CommandSender sender, String key, TagResolver... resolvers) {
        if (isEmpty(key)) return;
        sender.sendMessage(get(key, resolvers));
    }

    public void sendPrefixed(CommandSender sender, String key, TagResolver... resolvers) {
        if (isEmpty(key)) return;
        sender.sendMessage(getPrefixed(key, resolvers));
    }

    private String getRaw(String key) {
        return messageCache.get(key);
    }
}
