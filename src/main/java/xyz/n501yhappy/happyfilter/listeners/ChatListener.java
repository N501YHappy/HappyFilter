package xyz.n501yhappy.happyfilter.listeners;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import net.md_5.bungee.api.ChatColor;
import xyz.n501yhappy.happyfilter.HappyFilter;
import xyz.n501yhappy.happyfilter.config.PluginConfig;
import static xyz.n501yhappy.happyfilter.config.PluginConfig.LOG_INFO;
import static xyz.n501yhappy.happyfilter.config.PluginConfig.PREFIX;
import static xyz.n501yhappy.happyfilter.config.PluginConfig.WARNING_MESSAGE;
import static xyz.n501yhappy.happyfilter.config.PluginConfig.anti_interference_enabled;
import static xyz.n501yhappy.happyfilter.config.PluginConfig.debug_mode;
import static xyz.n501yhappy.happyfilter.config.PluginConfig.enableWarning;
import static xyz.n501yhappy.happyfilter.config.PluginConfig.filterWords;
import static xyz.n501yhappy.happyfilter.config.PluginConfig.interferenceChars;
import static xyz.n501yhappy.happyfilter.config.PluginConfig.isEnable;
import static xyz.n501yhappy.happyfilter.config.PluginConfig.log_to_console;
import static xyz.n501yhappy.happyfilter.config.PluginConfig.permissions;
import static xyz.n501yhappy.happyfilter.config.PluginConfig.regexPatterns;
import static xyz.n501yhappy.happyfilter.config.PluginConfig.regex_enabled;
import static xyz.n501yhappy.happyfilter.config.PluginConfig.replaceWords;
import static xyz.n501yhappy.happyfilter.config.PluginConfig.replace_enabled;
import static xyz.n501yhappy.happyfilter.config.PluginConfig.special_replace;
import static xyz.n501yhappy.happyfilter.config.PluginConfig.to_lower;
import xyz.n501yhappy.happyfilter.utils.Filter;
import xyz.n501yhappy.happyfilter.utils.structs.Area;
import xyz.n501yhappy.happyfilter.utils.structs.Filtered;

public class ChatListener implements Listener {
    private final Filter filter = new Filter();
    private final Random random = new Random();
    private final Map<Player, List<PlayerMessage>> messageHistory = new ConcurrentHashMap<>();

    private static class PlayerMessage {
        final String message;
        final long time;

        PlayerMessage(String message, long time) {
            this.message = message;
            this.time = time;
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        // 检测前置条件部分 没有权限就return,没有启用也return
        if (!isEnable)
            return;
        Player player = event.getPlayer();
        if (player.hasPermission(permissions.get("bypass")))
            return;

        // 消息合并
        String message = event.getMessage(); // 先获取这次消息
        String mergedMessage = mergeHistory(player, message); // 把历史信息和这次消息连接到一起

        // 干扰字符处理
        String solvedMessage = mergedMessage; // slovedMessage是干净的消息,当前是有干扰字符的消息
        List<Integer> indexMapping = new ArrayList<>();
        if (anti_interference_enabled) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mergedMessage.length(); i++) {
                char c = mergedMessage.charAt(i);
                if (!interferenceChars.contains(c)) {
                    indexMapping.add(i);
                    sb.append(c);
                }
            }
            solvedMessage = sb.toString();
        }
        if (to_lower) {
            solvedMessage = solvedMessage.toLowerCase();
        }

        Filtered result = filter.filterText(solvedMessage, filterWords);
        if (regex_enabled) {
            result = result.merge(filter.filterRegex(solvedMessage, regexPatterns));
        }

        // 过滤处理
        if (result.isFiltered()) {
            event.setMessage(AsolveMessages(message, mergedMessage, result, player, indexMapping, solvedMessage));
            if (enableWarning)
                player.sendMessage(PREFIX + WARNING_MESSAGE);
            messageHistory.remove(player);
        } else {
            updateMessageHistory(player, message);
        }
    }

    private String mergeHistory(Player player, String currentMessage) {
        List<PlayerMessage> history = messageHistory.computeIfAbsent(player, k -> new ArrayList<>());
        long now = System.currentTimeMillis();
        history.removeIf(msg -> now - msg.time > 2000);

        StringBuilder merged = new StringBuilder();
        for (PlayerMessage msg : history) {
            merged.append(msg.message);
        }
        merged.append(currentMessage);
        return merged.toString();
    }

    private void updateMessageHistory(Player player, String message) {
        List<PlayerMessage> history = messageHistory.computeIfAbsent(player, k -> new ArrayList<>());
        history.add(new PlayerMessage(message, System.currentTimeMillis()));
        while (history.size() > 20)
            history.remove(0);
    }

    private String AsolveMessages(String message, String mergedMessage, Filtered filtered_result, Player player,
            List<Integer> indexMapping, String solvedMessage) {
        StringBuilder ret_message = new StringBuilder(message);
        int startIndex = mergedMessage.length() - message.length();

        if (debug_mode) {
            player.sendMessage(ChatColor.LIGHT_PURPLE + "Debug - Original: " + message);
            player.sendMessage(ChatColor.LIGHT_PURPLE + "Debug - Merged: " + mergedMessage);
            player.sendMessage(ChatColor.LIGHT_PURPLE + "Debug - Solved: " + solvedMessage);
            player.sendMessage(ChatColor.LIGHT_PURPLE + "Debug - IndexMapping: " + indexMapping);
            player.sendMessage(ChatColor.LIGHT_PURPLE + "Debug - result: " + filtered_result.toString());
        }

        int offset = 0;
        for (int i = 0; i < filtered_result.getAreas().size(); i++) {
            Area area = filtered_result.getAreas().get(i);
            int l_index = area.getL();
            int r_index = area.getR();
            String bad_word = solvedMessage.substring(l_index, r_index);

            if (log_to_console) {
                HappyFilter.plugin.getLogger().info(LOG_INFO
                        .replace("{w}", bad_word)
                        .replace("{player}", player.getName()));
            }

            if (isSpecial(bad_word)) {
                Map<String, String> charMapping = special_replace.get(bad_word);
                for (int j = 0; j < bad_word.length(); j++) {
                    char currentChar = bad_word.charAt(j);
                    String mappedValue = charMapping.get(String.valueOf(currentChar));
                    int solvePos = l_index + j;

                    if (solvePos < indexMapping.size()) {
                        int mergedPos = indexMapping.get(solvePos);
                        int messagePos = mergedPos - startIndex + offset;

                        if (messagePos >= 0 && messagePos <= ret_message.length()) {
                            if (messagePos < ret_message.length()) ret_message.deleteCharAt(messagePos);
                            ret_message.insert(messagePos, mappedValue);
                            offset += mappedValue.length() - 1;
                        }
                    }
                }

            } else {
                int len = r_index - l_index;
                String replaces = getReplace(len, bad_word);
                for (int j = 0; j < bad_word.length(); j++) {
                    int cleanPos = l_index + j;
                    if (cleanPos < indexMapping.size()) {
                        int mergedPos = indexMapping.get(cleanPos);
                        int messagePos = mergedPos - startIndex + offset;

                        if (messagePos >= 0 && messagePos < ret_message.length()) {
                            char replaceChar = replaces.charAt(j);
                            ret_message.setCharAt(messagePos, replaceChar);
                        }
                    }
                }
            }
        }

        String result = ret_message.toString();

        if (debug_mode) {
            player.sendMessage(ChatColor.LIGHT_PURPLE + "Debug - Final Result: " + result);
        }

        return result;
    }

    private String getReplace(int length, String bad_word) {
        if (!replace_enabled || length <= 0)
            return "";
        StringBuilder sb = new StringBuilder();
        while (sb.length() < length) {
            String word = replaceWords.get(random.nextInt(replaceWords.size()));
            sb.append(word.substring(0, Math.min(length - sb.length(), word.length())));
        }
        return sb.toString();
    }

    private Boolean isSpecial(String word) {
        return PluginConfig.special_replace.containsKey(word);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        messageHistory.remove(event.getPlayer());
    }
}