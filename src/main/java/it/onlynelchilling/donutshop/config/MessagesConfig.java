package it.onlynelchilling.donutshop.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class MessagesConfig {

    private final ConfigFile configFile;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
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
        prefix = configFile.getConfig().getString("prefix");
        if (prefix == null) prefix = "";
        else prefix = MainConfig.convertLegacy(prefix);
    }

    public boolean isEmpty(String key) {
        String raw = configFile.getConfig().getString(key);
        return raw == null || raw.isEmpty();
    }

    public Component get(String key, TagResolver... resolvers) {
        String raw = configFile.getConfig().getString(key);
        if (raw == null || raw.isEmpty()) return Component.empty();
        return miniMessage.deserialize(MainConfig.convertLegacy(raw), resolvers);
    }

    public Component getPrefixed(String key, TagResolver... resolvers) {
        String msg = configFile.getConfig().getString(key);
        if (msg == null || msg.isEmpty()) return Component.empty();
        msg = MainConfig.convertLegacy(msg);
        String raw = prefix.isEmpty() ? msg : prefix + " " + msg;
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

    public String getRaw(String key) {
        return configFile.getConfig().getString(key);
    }
}
