package it.onlynelchilling.donutshop.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("all")
public class ConfigFile {

    private final Plugin plugin;
    private final String fileName;
    private final File file;
    private YamlConfiguration config;

    public ConfigFile(Plugin plugin, String fileName) {
        this.plugin = plugin;
        this.fileName = fileName + ".yml";
        this.file = new File(plugin.getDataFolder(), this.fileName);

        if (!file.exists()) {
            plugin.saveResource(this.fileName, false);
        }

        config = YamlConfiguration.loadConfiguration(file);

        if (config.getKeys(false).isEmpty()) {
            plugin.saveResource(this.fileName, true);
            config = YamlConfiguration.loadConfiguration(file);
        }

        mergeDefaults();
    }

    public YamlConfiguration getConfig() {
        return config;
    }

    public void reload() {
        config = YamlConfiguration.loadConfiguration(file);

        if (config.getKeys(false).isEmpty()) {
            plugin.saveResource(this.fileName, true);
            config = YamlConfiguration.loadConfiguration(file);
        }

        mergeDefaults();
    }

    private void mergeDefaults() {
        InputStream defaultStream = plugin.getResource(fileName);
        if (defaultStream == null) return;

        List<String> defaultLines = readLines(defaultStream);
        if (defaultLines == null) return;

        YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                new StringReader(String.join("\n", defaultLines)));

        if (!hasMissingKeys(defaults)) return;

        byte[] backup;
        try {
            backup = Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            return;
        }

        List<String> result = buildMergedLines(defaultLines, defaults);
        writeMerged(result, backup);
    }

    private List<String> readLines(InputStream stream) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            return lines;
        } catch (IOException e) {
            return null;
        }
    }

    private boolean hasMissingKeys(YamlConfiguration defaults) {
        for (String key : defaults.getKeys(true)) {
            if (!defaults.isConfigurationSection(key) && !config.contains(key)) {
                return true;
            }
            if (defaults.isConfigurationSection(key)
                    && !config.isConfigurationSection(key)
                    && !config.contains(key)) {
                return true;
            }
        }
        return false;
    }

    private List<String> buildMergedLines(List<String> defaultLines, YamlConfiguration defaults) {
        Set<String> userKeys = config.getKeys(true);
        List<String> result = new ArrayList<>();

        for (int i = 0; i < defaultLines.size(); i++) {
            String dLine = defaultLines.get(i);

            if (dLine.isBlank() || dLine.stripLeading().startsWith("#")) {
                result.add(dLine);
                continue;
            }

            String yamlPath = resolvePathAtLine(defaultLines, i);
            if (yamlPath == null) {
                result.add(dLine);
                continue;
            }

            if (userKeys.contains(yamlPath)) {
                if (defaults.isConfigurationSection(yamlPath)
                        || config.isConfigurationSection(yamlPath)) {
                    result.add(dLine);
                } else {
                    Object userValue = config.get(yamlPath);
                    int indent = countIndent(dLine);
                    String keyName = extractKeyName(dLine);
                    result.addAll(serializeValue(keyName, userValue, indent));
                    i = skipValueBlock(defaultLines, i);
                }
            } else {
                result.add(dLine);
                if (defaults.isConfigurationSection(yamlPath) && !config.contains(yamlPath)) {
                    int blockEnd = skipValueBlock(defaultLines, i);
                    for (int j = i + 1; j <= blockEnd; j++) {
                        result.add(defaultLines.get(j));
                    }
                    i = blockEnd;
                }
            }
        }

        return result;
    }

    private void writeMerged(List<String> result, byte[] backup) {
        try {
            Files.write(file.toPath(), result, StandardCharsets.UTF_8);
            YamlConfiguration test = YamlConfiguration.loadConfiguration(file);

            if (test.getKeys(false).size() < config.getKeys(false).size()) {
                Files.write(file.toPath(), backup);
                config = YamlConfiguration.loadConfiguration(file);
                plugin.getLogger().warning("Merge produced invalid YAML for " + fileName + ", restored backup.");
                return;
            }

            config = test;
        } catch (Exception e) {
            try {
                Files.write(file.toPath(), backup);
                config = YamlConfiguration.loadConfiguration(file);
                plugin.getLogger().warning("Failed to write merged " + fileName + ", restored backup.");
            } catch (IOException ignored) {
            }
        }
    }

    private String resolvePathAtLine(List<String> lines, int lineIdx) {
        String line = lines.get(lineIdx);
        if (line.isBlank() || line.stripLeading().startsWith("#")) return null;

        String trimmed = line.stripLeading();
        int colonIdx = trimmed.indexOf(':');
        if (colonIdx <= 0) return null;

        if (colonIdx + 1 < trimmed.length()) {
            char afterColon = trimmed.charAt(colonIdx + 1);
            if (afterColon != ' ' && afterColon != '\r') return null;
        }

        String key = stripQuotes(trimmed.substring(0, colonIdx).strip());
        if (key.startsWith("-")) return null;

        int myIndent = countIndent(line);
        if (myIndent == 0) return key;

        Deque<String> pathParts = new ArrayDeque<>();
        pathParts.addFirst(key);

        int expectedParentIndent = myIndent - 2;
        for (int i = lineIdx - 1; i >= 0 && expectedParentIndent >= 0; i--) {
            String pLine = lines.get(i);
            if (pLine.isBlank() || pLine.stripLeading().startsWith("#")) continue;

            if (countIndent(pLine) == expectedParentIndent) {
                String pTrimmed = pLine.stripLeading();
                int pColon = pTrimmed.indexOf(':');
                if (pColon <= 0) return null;

                String pKey = stripQuotes(pTrimmed.substring(0, pColon).strip());
                if (pKey.startsWith("-")) return null;

                pathParts.addFirst(pKey);
                expectedParentIndent -= 2;
            }
        }

        return String.join(".", pathParts);
    }

    private int skipValueBlock(List<String> lines, int startIdx) {
        int indent = countIndent(lines.get(startIdx));
        int i = startIdx + 1;

        while (i < lines.size()) {
            String l = lines.get(i);

            if (l.isBlank() || l.stripLeading().startsWith("#")) {
                int peek = i;
                while (peek < lines.size()
                        && (lines.get(peek).isBlank() || lines.get(peek).stripLeading().startsWith("#"))) {
                    peek++;
                }
                if (peek >= lines.size() || countIndent(lines.get(peek)) <= indent) {
                    return i - 1;
                }
                i++;
                continue;
            }

            if (countIndent(l) <= indent) {
                return i - 1;
            }
            i++;
        }

        return i - 1;
    }

    private List<String> serializeValue(String key, Object value, int indent) {
        String pad = " ".repeat(indent);
        List<String> lines = new ArrayList<>();

        if (value instanceof List<?> list) {
            if (list.isEmpty()) {
                lines.add(pad + key + ": []");
            } else {
                lines.add(pad + key + ":");
                for (Object item : list) {
                    if (item instanceof Map) {
                        lines.addAll(serializeMapInList((Map<?, ?>) item, indent + 2));
                    } else {
                        String str = item == null ? "null" : item.toString();
                        lines.add(pad + "  - " + (needsQuoting(str) ? "'" + str.replace("'", "''") + "'" : str));
                    }
                }
            }
        } else if (value instanceof String str) {
            if (str.contains("\n")) {
                lines.add(pad + key + ": |");
                for (String part : str.split("\n", -1)) {
                    lines.add(pad + "  " + part);
                }
            } else if (needsQuoting(str)) {
                lines.add(pad + key + ": '" + str.replace("'", "''") + "'");
            } else {
                lines.add(pad + key + ": " + str);
            }
        } else if (value == null) {
            lines.add(pad + key + ": null");
        } else {
            lines.add(pad + key + ": " + value);
        }

        return lines;
    }

    private List<String> serializeMapInList(Map<?, ?> map, int indent) {
        String pad = " ".repeat(indent);
        List<String> lines = new ArrayList<>();
        boolean first = true;

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String k = entry.getKey().toString();
            Object v = entry.getValue();

            if (first) {
                lines.add(pad + "- " + k + ": " + (v == null ? "null" : v));
                first = false;
            } else {
                lines.add(pad + "  " + k + ": " + (v == null ? "null" : v));
            }
        }

        return lines;
    }

    private String extractKeyName(String line) {
        String trimmed = line.stripLeading();
        int colon = trimmed.indexOf(':');
        return stripQuotes(trimmed.substring(0, colon).strip());
    }

    private String stripQuotes(String str) {
        if ((str.startsWith("'") && str.endsWith("'"))
                || (str.startsWith("\"") && str.endsWith("\""))) {
            return str.substring(1, str.length() - 1);
        }
        return str;
    }

    private boolean needsQuoting(String str) {
        if (str.isEmpty()) return true;

        if (str.contains("#") || str.contains(":") || str.contains("{") || str.contains("}")
                || str.contains("[") || str.contains("]") || str.contains(",")
                || str.contains("&") || str.contains("*") || str.contains("?")
                || str.contains("|") || str.contains(">") || str.contains("<")
                || str.contains("!") || str.contains("%") || str.contains("@")
                || str.contains("`") || str.contains("'") || str.contains("\"")) {
            return true;
        }

        if (str.equals("---") || str.equals("...")) return true;

        if (str.startsWith(" ") || str.endsWith(" ")) return true;

        if (str.equalsIgnoreCase("true") || str.equalsIgnoreCase("false")
                || str.equalsIgnoreCase("null") || str.equalsIgnoreCase("yes")
                || str.equalsIgnoreCase("no")) {
            return true;
        }

        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException ignored) {
        }

        return false;
    }

    private int countIndent(String line) {
        int count = 0;
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) == ' ') count++;
            else break;
        }
        return count;
    }
}
