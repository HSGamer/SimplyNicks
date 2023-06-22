package net.simplyvanilla.simplynicks;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.simplyvanilla.simplynicks.commands.NickCommandExecutor;
import net.simplyvanilla.simplynicks.commands.RealnameCommandExecutor;
import net.simplyvanilla.simplynicks.database.Cache;
import net.simplyvanilla.simplynicks.database.MySQL;
import net.simplyvanilla.simplynicks.event.PlayerEvents;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class SimplyNicks extends JavaPlugin {
    private MySQL database;
    private Cache cache;
    private List<String> colors;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();

        this.database = new MySQL(this);
        this.cache = new Cache(this);

        try {
            this.database.connect();
        } catch (Exception e) {
            getLogger().warning(
                "Could not connect to database! Please check your config.yml and try again.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        try {
            this.cache.initCache();
        } catch (Exception e) {
            getLogger().warning(
                "Could not load cache! Please check your config.yml and try again.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        this.getServer().getPluginManager().registerEvents(new PlayerEvents(this), this);
        this.getCommand("nick").setExecutor(new NickCommandExecutor(this));
        this.getCommand("realname").setExecutor(new RealnameCommandExecutor(this));
        colors = this.getConfig().getStringList("colors");
    }

    @Override
    public void onDisable() {
        database.close();
    }

    public MySQL getDatabase() {
        return this.database;
    }

    public Cache getCache() {
        return this.cache;
    }

    public List<String> getColors() {
        return colors;
    }

    public String getMessage(String path) {
        return Optional.ofNullable(this.getConfig().getString(path)).orElse(path);
    }

    public void sendConfigMessage(CommandSender commandSender, String message) {
        sendConfigMessage(commandSender, message, new HashMap<>());
    }

    public void sendConfigMessage(CommandSender commandSender, String message,
                                  Map<String, String> replacements) {
        message = getMessage(message);
        commandSender.sendMessage(MiniMessage.miniMessage()
            .deserialize(message, replacements.entrySet().stream()
                .map(entry -> {
                    if (entry.getValue().contains("&")) {
                        return Placeholder.component(entry.getKey(),
                            LegacyComponentSerializer.legacyAmpersand()
                                .deserialize(entry.getValue()));
                    }

                    return Placeholder.unparsed(entry.getKey(),
                        entry.getValue());
                }).toList().toArray(TagResolver[]::new)));
    }
}
