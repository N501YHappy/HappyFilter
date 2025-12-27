package xyz.n501yhappy.happyfilter.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import net.md_5.bungee.api.ChatColor;
import xyz.n501yhappy.happyfilter.HappyFilter;
import xyz.n501yhappy.happyfilter.utils.Filter;
import xyz.n501yhappy.happyfilter.utils.Filtered;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static xyz.n501yhappy.happyfilter.config.PluginConfig.*;

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
        // 检测前置条件部分
        if (!isEnable) return;
        Player player = event.getPlayer();
        if (player.hasPermission(permissions.get("bypass"))) return;
        
        // 消息合并
        String message = event.getMessage();
        String mergedMessage = mergeHistory(player, message);
        
        // 干扰字符处理
        String solvedMessage = mergedMessage;
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
            event.setMessage(solveMessages(message, mergedMessage, result, player, indexMapping, solvedMessage));
            if (enableWarning) {
                player.sendMessage(PREFIX + WARNING_MESSAGE);
            }
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
        while (history.size() > 20) history.remove(0);
    }

    private String solveMessages(String message, String mergedMessage, Filtered result, Player player, List<Integer> indexMapping, String solvedMessage) {
        int startIndex = mergedMessage.length() - message.length();
        StringBuilder retMessage = new StringBuilder(message);
        
        // 分别处理特殊屏蔽词和普通屏蔽词
        for (int i = 0; i < result.getRIndexes().size(); i++) {
            int l_index = result.getLIndexes().get(i);
            int r_index = result.getRIndexes().get(i);
            String bad_word = solvedMessage.substring(l_index, r_index);
            
            // 检查是否为特殊屏蔽词
            if (SP_K.contains(bad_word)) {
                // 特殊屏蔽词：整体替换
                String replacement = special_replaces.get(bad_word);
                if (replacement != null && !replacement.isEmpty()) {
                    // 计算在原始消息中的起始和结束位置
                    int originalStart = -1;
                    int originalEnd = -1;
                    
                    if (l_index < indexMapping.size()) {
                        originalStart = indexMapping.get(l_index) - startIndex;
                    }
                    
                    if (r_index > 0 && r_index - 1 < indexMapping.size()) {
                        originalEnd = indexMapping.get(r_index - 1) - startIndex + 1;
                    }
                    
                    // 如果找到有效位置，进行整体替换
                    if (originalStart >= 0 && originalEnd > originalStart && originalEnd <= retMessage.length()) {
                        retMessage.replace(originalStart, originalEnd, replacement);
                        
                        // 记录日志
                        if (log_to_console) {
                            HappyFilter.plugin.getLogger().info(LOG_INFO
                                .replace("{w}", bad_word)
                                .replace("{player}", player.getName()));
                        }
                    }
                }
            } else {
                // 普通屏蔽词：使用原来的逐字符替换逻辑
                String replacement = getReplace(r_index - l_index, bad_word);
                
                // 记录日志
                if (log_to_console) {
                    HappyFilter.plugin.getLogger().info(LOG_INFO
                        .replace("{w}", bad_word)
                        .replace("{player}", player.getName()));
                }
                
                // 逐字符替换
                for (int solvedPos = l_index; solvedPos < r_index; solvedPos++) {
                    int relativePos = solvedPos - l_index;
                    if (relativePos >= replacement.length()) break;
                    
                    if (solvedPos < indexMapping.size()) {
                        int originalPos = indexMapping.get(solvedPos) - startIndex;
                        if (originalPos >= 0 && originalPos < retMessage.length()) {
                            retMessage.setCharAt(originalPos, replacement.charAt(relativePos));
                        }
                    }
                }
            }
        }
        
        return retMessage.toString();
    }

    private String getReplace(int length, String bad_word) {
        // 特殊屏蔽词已经在solveMessages中处理，这里只处理普通屏蔽词
        
        if (!replace_enabled || length <= 0) return "";
        
        // 普通替换逻辑
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
