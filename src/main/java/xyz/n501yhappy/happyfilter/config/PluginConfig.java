package xyz.n501yhappy.happyfilter.config;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringEscapeUtils;
import org.bukkit.configuration.Configuration;
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
    public static Boolean special_replace_enabled = true;
    public static Boolean debug_mode = false;
    public static Map<String, String> permissions = new HashMap<>();
    public static Map<String, String> special_replaces = new HashMap<>();
    // others
    public static List<String> SP_K = new ArrayList<>();
    public static List<String> SP_V = new ArrayList<>();
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

        special_replace_enabled = config.getBoolean("filter_rules.special_replace.enable", true);
        loadSpecialReplaces();
        enableWarning = config.getBoolean("warning.enabled", true);
        permissions.put("bypass", "happyfilter.bypass");
        permissions.put("admin", "happyfilter.admin");
        isEnable = config.getBoolean("enabled", true);
        log_to_console = config.getBoolean("log_to_console", true);
        debug_mode = config.getBoolean("debug", false);
        if (log_to_console) {
            plugin.getLogger().info(ChatColor.GREEN + "配置加载完成！");
            plugin.getLogger().info(ChatColor.LIGHT_PURPLE + "特殊替换词数量: " + special_replaces.size());
            plugin.getLogger().info(ChatColor.BLUE + "过滤词数量: " + filterWords.size());
            plugin.getLogger().info(ChatColor.YELLOW + "正则模式数量: " + regexPatterns.size());
        }
    }

    private static void loadSpecialReplaces() {
        special_replaces.clear();
        SP_K.clear();
        SP_V.clear();
        if (config.contains("filter_rules.special_replace") &&
                config.isConfigurationSection("filter_rules.special_replace")) {

            for (String key : config.getConfigurationSection("filter_rules.special_replace.matches").getKeys(false)) {
                String value = config.getString("filter_rules.special_replace.matches." + key);
                if (value != null) {
                    String unescapedValue = StringEscapeUtils.unescapeJava(value);
                    special_replaces.put(key, unescapedValue);
                    SP_K.add(key);
                    SP_V.add(unescapedValue);
                    if (!filterWords.contains(key))
                        filterWords.add(key);
                }
            }
        }
    }

    public static void reload() {
        loadConfig();
    }
}
