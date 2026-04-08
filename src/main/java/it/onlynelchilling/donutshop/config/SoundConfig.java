package it.onlynelchilling.donutshop.config;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class SoundConfig {

    private final JavaPlugin plugin;
    private final ConfigFile configFile;
    private final Map<String, SoundEntry> sounds = new HashMap<>();

    public SoundConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new ConfigFile(plugin, "sounds");
        loadValues();
    }

    public void load() {
        configFile.reload();
        loadValues();
    }

    private void loadValues() {
        sounds.clear();
        YamlConfiguration config = configFile.getConfig();

        for (String key : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(key);
            if (section == null) continue;

            try {
                String soundKey = section.getString("sound");
                if (soundKey == null) continue;
                Sound sound = resolveSound(soundKey);
                if (sound == null) {
                    plugin.getLogger().warning("Invalid sound for key " + key + ": " + soundKey);
                    continue;
                }
                float volume = (float) section.getDouble("volume");
                float pitch = (float) section.getDouble("pitch");
                boolean enabled = section.getBoolean("enabled");
                sounds.put(key, new SoundEntry(sound, volume, pitch, enabled));
            } catch (Exception e) {
                plugin.getLogger().warning("Error loading sound " + key + ": " + e.getMessage());
            }
        }
    }

    public void play(Player player, String key) {
        SoundEntry entry = sounds.get(key);
        if (entry != null && entry.enabled) {
            player.playSound(player.getLocation(), entry.sound, entry.volume, entry.pitch);
        }
    }

    private Sound resolveSound(String input) {
        Sound sound = Registry.SOUNDS.get(NamespacedKey.minecraft(input.toLowerCase()));
        if (sound != null) return sound;
        return Registry.SOUNDS.get(NamespacedKey.minecraft(input.toLowerCase().replace("_", ".")));
    }

    private record SoundEntry(Sound sound, float volume, float pitch, boolean enabled) {
    }
}
