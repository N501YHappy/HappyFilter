package xyz.n501yhappy.happyfilter.config;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringEscapeUtils;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import net.md_5.bungee.api.ChatColor;
import xyz.n501yhappy.happyfilter.HappyFilter;
import static xyz.n501yhappy.happyfilter.HappyFilter.plugin;

public class PluginConfig {
    public static Configuration messagesConfig;
    //
    public static Configuration config;
    public static List<String> filterWords, regexPatterns;
    public static List<String> replaceWords;
    public static List<Character> interferenceChars;
    public static Boolean anti_interference_enabled;
    public static Boolean enableWarning;
    public static Boolean to_lower;
    public static Boolean isEnable = true;
    public static Boolean log_to_console = true;
    public static Boolean regex_enabled = true;
    public static Boolean replace_enabled = true;
    public static Boolean debug_mode = false;
    public static Boolean special_replace_enabled = false;
    public static Map<String, String> permissions = new HashMap<>();
    public static Map<String, String> special_replaces = new HashMap<>();

    // special_replace 数据存储（外层：模式字符串，内层：字符映射）
    public static Map<String, Map<String, String>> special_replace = new HashMap<>();

    // message
    public static String PREFIX;
    public static String RELOAD_SUCCESS;
    public static String PLUGIN_ENABLED;
    public static String PLUGIN_DISABLED;
    public static String UNKNOWN_COMMAND;
    public static String HELP_HEADER;
    public static String HELP_RELOAD;
    public static String HELP_HELP;
    public static String HELP_ENABLE;
    public static String HELP_DISABLE;
    public static String WARNING_MESSAGE;
    public static String NO_PERMISSION;
    public static String LOG_INFO;
    public static String SPECIAL_REPLACE_INFO;

    public static void loadMessages() {
        File msgFile = new File(HappyFilter.plugin.getDataFolder(), "messages.yml");
        if (!msgFile.exists()) {
            HappyFilter.plugin.saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(msgFile);
        loadMessagesFromConfig();
    }

    private static void loadMessagesFromConfig() {
        PREFIX = messagesConfig.getString("prefix", "§a[HappyFilter] ");
        RELOAD_SUCCESS = messagesConfig.getString("commands.reload_success", "§a配置已重载");
        PLUGIN_ENABLED = messagesConfig.getString("commands.plugin_enabled", "§a插件已启用");
        PLUGIN_DISABLED = messagesConfig.getString("commands.plugin_disabled", "§a插件已禁用");
        UNKNOWN_COMMAND = messagesConfig.getString("commands.unknown_command", "§c未知命令!");
        HELP_HEADER = messagesConfig.getString("commands.help.header", "§aHappyFilter 帮助");
        HELP_RELOAD = messagesConfig.getString("commands.help.reload", "§a/happyfilter reload - 重载配置");
        HELP_HELP = messagesConfig.getString("commands.help.help", "§a/happyfilter help - 显示帮助");
        HELP_ENABLE = messagesConfig.getString("commands.help.enable", "§a/happyfilter enable - 启用违禁词拦截");
        HELP_DISABLE = messagesConfig.getString("commands.help.disable", "§a/happyfilter disable - 禁用违禁词拦截");
        WARNING_MESSAGE = messagesConfig.getString("warning.message", "§c不要发布敏感信息!");
        NO_PERMISSION = messagesConfig.getString("commands.no_permission", "§c你没有权限执行此命令!");
        LOG_INFO = messagesConfig.getString("log", "Word: {w} Player: {player}");
        SPECIAL_REPLACE_INFO = messagesConfig.getString("special_replace_info", "已加载特殊替换规则: {count}");
    }

    public static void loadConfig() {
        plugin.saveDefaultConfig();
        config = plugin.getConfig();
        loadMessages();

        filterWords = config.getStringList("filter_words").stream()
                .map(StringEscapeUtils::unescapeJava)
                .collect(Collectors.toList());

        to_lower = config.getBoolean("filter_rules.to_lower", true);
        regexPatterns = config.getStringList("filter_rules.regex.regexes");

        regex_enabled = config.getBoolean("filter_rules.regex.enable", true);
        anti_interference_enabled = config.getBoolean("filter_rules.anti_interference.enabled", false);
        interferenceChars = config.getCharacterList("filter_rules.anti_interference.interference_characters");

        replace_enabled = config.getBoolean("filter_rules.replace.enable", true);
        replaceWords = config.getStringList("filter_rules.replace.replace_words").stream()
                .map(StringEscapeUtils::unescapeJava)
                .collect(Collectors.toList());

        enableWarning = config.getBoolean("warning.enabled", true);
        permissions.put("bypass", "happyfilter.bypass");
        permissions.put("admin", "happyfilter.admin");
        isEnable = config.getBoolean("enabled", true);
        log_to_console = config.getBoolean("log_to_console", true);
        debug_mode = config.getBoolean("debug", false);

        // 加载特殊替换规则
        special_replace_enabled = config.getBoolean("filter_rules.special_replace.enable", false);
        if (special_replace_enabled) {
            loadSpecialReplaceConfig();
        }

        if (log_to_console) {
            plugin.getLogger().info(ChatColor.GREEN + "配置加载完成！");
            plugin.getLogger().info(ChatColor.LIGHT_PURPLE + "特殊替换词数量: " + special_replaces.size());
            plugin.getLogger().info(ChatColor.LIGHT_PURPLE + "字符映射规则数量: " + special_replace.size());
            plugin.getLogger().info(ChatColor.BLUE + "过滤词数量: " + filterWords.size());
            plugin.getLogger().info(ChatColor.YELLOW + "正则模式数量: " + regexPatterns.size());
            if (debug_mode) {
                plugin.getLogger().info(ChatColor.LIGHT_PURPLE + "Debug enabled");
            }
        }
    }

    private static void loadSpecialReplaceConfig() {
        special_replaces.clear();
        special_replace.clear();

        ConfigurationSection matches = config.getConfigurationSection("filter_rules.special_replace.matches");
        for (String key : matches.getKeys(false)) {
            String value = matches.getString(key);
            if (value != null && !key.trim().isEmpty() && !value.trim().isEmpty()) {

                String unescapedKey = StringEscapeUtils.unescapeJava(key.trim());
                String unescapedValue = StringEscapeUtils.unescapeJava(value.trim());
                special_replaces.put(unescapedKey, unescapedValue);

                Map<String, String> charMapping = createCharMapping(unescapedKey, unescapedValue);
                special_replace.put(unescapedKey, charMapping);
                filterWords.add(unescapedKey);
            }
        }
    }

    private static Map<String, String> createCharMapping(String key, String val) {
        Map<String, String> mapping = new HashMap<>();
        int keyl = key.length();
        int vall = val.length();

        if (keyl == vall) {
            for (int i = 0; i < keyl; i++) {
                mapping.put(String.valueOf(key.charAt(i)),
                        String.valueOf(val.charAt(i)));
            }
        } else if (keyl > vall) {
            int step = (int) Math.floor((double) vall / keyl);

            for (int i = 0; i < keyl; i++) {
                String keyChar = String.valueOf(key.charAt(i));
                String valChar = "";

                int index = i / step;
                if (i % step == 0 && index < vall) {
                    valChar = String.valueOf(val.charAt(index));
                }

                mapping.put(keyChar, valChar);
            }
        } else {
            int step = (int) Math.floor((double) vall / keyl);
            for (int i = 0; i < keyl; i++) {
                String keyChar = String.valueOf(key.charAt(i));

                int start = i * step;
                int end = Math.min(start + step, vall);
                if (val.length() - start - step< step) {
                    end += val.length() - start - step;
                }
                String valChar = val.substring(start, end);

                mapping.put(keyChar, valChar);
            }
        }
        return mapping;
    }

    public static void reload() {
        loadConfig();
    }
}