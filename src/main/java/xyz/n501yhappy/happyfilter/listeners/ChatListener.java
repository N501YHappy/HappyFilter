package xyz.n501yhappy.happyfilter.listeners;

import java.util.ArrayList;
import java.util.HashMap;
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
import static xyz.n501yhappy.happyfilter.config.PluginConfig.LOG_INFO;
import static xyz.n501yhappy.happyfilter.config.PluginConfig.PREFIX;
import static xyz.n501yhappy.happyfilter.config.PluginConfig.SP_K;
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
import static xyz.n501yhappy.happyfilter.config.PluginConfig.special_replaces;
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
        Map<String, String> special_replace_cache = new HashMap<>();
        for (int i = 0; i < filtered_result.getAreas().size(); i++) {
            Area area = filtered_result.getAreas().get(i);
            int l_index = area.getL();
            int r_index = area.getR();
            int len = r_index - l_index;
            String bad_word = solvedMessage.substring(l_index, r_index);
            String replaces = getReplace(len, bad_word);
            if (log_to_console) {
                HappyFilter.plugin.getLogger().info(LOG_INFO
                        .replace("{w}", bad_word)
                        .replace("{player}", player.getName()));
            }
            // 特殊替换处理
            if (SP_K.contains(bad_word)) {
                if (debug_mode) {
                    player.sendMessage(ChatColor.LIGHT_PURPLE + "Debug - special: " + bad_word);
                }
                StringBuilder nya = new StringBuilder(len);
                for (int solvedPos = l_index; solvedPos < r_index; solvedPos++) {
                    if (solvedPos >= indexMapping.size())
                        break;
                    int originalPos = indexMapping.get(solvedPos) - startIndex;
                    char unique = (char) (33 + ((l_index + solvedPos) % 94));
                    if (originalPos >= 0 && originalPos < ret_message.length()) {
                        ret_message.setCharAt(originalPos, unique);
                    }
                    nya.append(unique);
                }
                special_replace_cache.put(nya.toString(), special_replaces.get(bad_word));
                if (debug_mode) {
                    player.sendMessage(ChatColor.LIGHT_PURPLE + "Debug - special_replace_cache: " + nya + ChatColor.BLUE
                            + " " + special_replaces.get(bad_word));
                }
                continue;
            }
            for (int solvedPos = l_index; solvedPos < r_index; solvedPos++) {
                int relativePos = solvedPos - l_index;
                if (relativePos >= replaces.length())
                    break;
                if (solvedPos < indexMapping.size()) {
                    int originalPos = Math.max(0, indexMapping.get(solvedPos) - startIndex);
                    if (originalPos < ret_message.length()) {
                        ret_message.setCharAt(originalPos, replaces.charAt(relativePos));
                    }
                }
            }
        }
        String result = ret_message.toString();
        if (debug_mode) {
            player.sendMessage(ChatColor.LIGHT_PURPLE + "Debug - RESULT: " + result);
        }
        for (Map.Entry<String, String> e : special_replace_cache.entrySet()) {
            result = result.replace(e.getKey(), e.getValue());
            if (debug_mode) {
                player.sendMessage(ChatColor.AQUA + "Debug - replacing: " + e.getKey() + " " + e.getValue());
            }
        }
        if (debug_mode) {
            player.sendMessage(ChatColor.LIGHT_PURPLE + "Debug - RESULT2: " + result);
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

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        messageHistory.remove(event.getPlayer());
    }
}