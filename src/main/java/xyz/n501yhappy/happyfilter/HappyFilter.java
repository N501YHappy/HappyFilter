package xyz.n501yhappy.happyfilter;

import org.bstats.bukkit.Metrics;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.n501yhappy.happyfilter.commands.CommandHandler;
import xyz.n501yhappy.happyfilter.listeners.ChatListener;

import static xyz.n501yhappy.happyfilter.config.PluginConfig.*;

public final class HappyFilter extends JavaPlugin {
    public static HappyFilter plugin;

    @Override
    public void onEnable() {
        plugin = this; new Metrics(this, 27208); //注册bstats
        loadConfig();
        getServer().getPluginManager().registerEvents(new ChatListener(), this);

        PluginCommand command = getCommand("happyfilter"); //注册命令
        if (command != null) {
            CommandHandler handler = new CommandHandler();
            command.setExecutor(handler);
            command.setTabCompleter(handler);
        }
    }

    @Override
    public void onDisable() {
        // sunh666
    }
}
