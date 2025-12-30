package xyz.n501yhappy.happyfilter.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import static xyz.n501yhappy.happyfilter.HappyFilter.plugin;
import static xyz.n501yhappy.happyfilter.config.PluginConfig.HELP_DISABLE;
import static xyz.n501yhappy.happyfilter.config.PluginConfig.HELP_ENABLE;
import static xyz.n501yhappy.happyfilter.config.PluginConfig.HELP_HEADER;
import static xyz.n501yhappy.happyfilter.config.PluginConfig.HELP_HELP;
import static xyz.n501yhappy.happyfilter.config.PluginConfig.HELP_RELOAD;
import static xyz.n501yhappy.happyfilter.config.PluginConfig.NO_PERMISSION;
import static xyz.n501yhappy.happyfilter.config.PluginConfig.PLUGIN_DISABLED;
import static xyz.n501yhappy.happyfilter.config.PluginConfig.PLUGIN_ENABLED;
import static xyz.n501yhappy.happyfilter.config.PluginConfig.PREFIX;
import static xyz.n501yhappy.happyfilter.config.PluginConfig.RELOAD_SUCCESS;
import static xyz.n501yhappy.happyfilter.config.PluginConfig.UNKNOWN_COMMAND;
import static xyz.n501yhappy.happyfilter.config.PluginConfig.config;
import static xyz.n501yhappy.happyfilter.config.PluginConfig.isEnable;
import static xyz.n501yhappy.happyfilter.config.PluginConfig.loadConfig;
import static xyz.n501yhappy.happyfilter.config.PluginConfig.permissions;

public class CommandHandler implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(permissions.get("admin"))) {
            sender.sendMessage(PREFIX + NO_PERMISSION);
            return true;
        }

        if (args.length < 1) {
            showHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                reloadConfig();
                sender.sendMessage(PREFIX + RELOAD_SUCCESS);
                break;
            case "help":
                showHelp(sender);
                break;
            case "enable":
                isEnable = true;
                config.set("enabled", true);
                sender.sendMessage(PREFIX + PLUGIN_ENABLED);
                break;
            case "disable":
                isEnable = false;
                config.set("enabled", false);
                sender.sendMessage(PREFIX + PLUGIN_DISABLED);
                break;
            default:
                sender.sendMessage(PREFIX + UNKNOWN_COMMAND);
        }
        return true;
    }
    private void reloadConfig() {
        plugin.reloadConfig();
        loadConfig();
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(PREFIX + HELP_HEADER);
        sender.sendMessage(HELP_RELOAD);
        sender.sendMessage(HELP_HELP);
        sender.sendMessage(HELP_ENABLE);
        sender.sendMessage(HELP_DISABLE);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission(permissions.get("admin"))) {
            List<String> completions = new ArrayList<>();
            completions.add("reload");
            completions.add("help");
            completions.add("enable");
            completions.add("disable");
            return completions;
        }
        return new ArrayList<>();
    }
}
