package net.okocraft.haze.config;

import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import net.okocraft.configurationapi.BaseConfig;
import net.okocraft.haze.Haze;

public class Messages extends BaseConfig {

    private static Haze plugin = Haze.getInstance();
    private static Messages instance = new Messages();

    public Messages() {
        super("messages.yml", plugin.getDataFolder(), plugin.getResource("messages.yml"));
        if (instance != null) {
            throw new ExceptionInInitializerError("Message is already initialized.");
        }
    }

    public static Messages getInstance() {
        return instance;
    }

    /**
     * Send message to player.
     * 
     * @param player
     * @param addPrefix
     * @param path
     * @param placeholders
     */
    public void sendMessage(CommandSender sender, boolean addPrefix, String path, Map<String, Object> placeholders) {
        String prefix = addPrefix ? getConfig().getString("command.general.info.plugin-prefix", "&8[&6Haze&8]&r") + " " : "";
        String message = prefix + getMessage(path);
        for (Map.Entry<String, Object> placeholder : placeholders.entrySet()) {
            message = message.replace(placeholder.getKey(), placeholder.getValue().toString());
        }
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        return;
    }

    /**
     * Send message to player.
     * 
     * @param player
     * @param path
     * @param placeholders
     */
    public void sendMessage(CommandSender sender, String path, Map<String, Object> placeholders) {
        sendMessage(sender, true, path, placeholders);
    }

    /**
     * Send message to player.
     * 
     * @param sender
     * @param path
     */
    public void sendMessage(CommandSender sender, String path) {
        sendMessage(sender, path, Map.of());
    }

    /**
     * Send message to player.
     * 
     * @param sender
     * @param addPrefix
     * @param path
     */
    public void sendMessage(CommandSender sender, boolean addPrefix, String path) {
        sendMessage(sender, addPrefix, path, Map.of());
    }

    /**
     * Gets message from key. Returned messages will not translated its color code.
     * 
     * @param path
     * @return
     */
    public String getMessage(String path) {
        return getConfig().getString(path, path);
    }
    
    public void sendInvalidArgument(CommandSender sender, String invalid) {
        sendMessage(sender, "command.general.error.invalid-argument", Map.of("%argument%", invalid));
    }

    public void sendNoPermission(CommandSender sender, String permission) {
        sendMessage(sender, "command.general.error.no-permission", Map.of("%permission%", permission));
    }

    public void sendConsoleSenderCannotUse(CommandSender sender) {
        sendMessage(sender, "command.general.error.cannot-use-from-console");
    }

    public void sendPlayerCannotUse(CommandSender sender) {
        sendMessage(sender, "command.general.error.player-cannot-use");
    }

    public void sendNotEnoughArguments(CommandSender sender) {
        sendMessage(sender, "command.general.error.not-enough-arguments");
    }

    public void sendInvalidNumber(CommandSender sender, String number) {
        sendMessage(sender, "command.general.error.invalid-number", Map.of("%number%", number));
    }

    public void sendUsage(CommandSender sender, String usage) {
        sendMessage(sender, "command.general.info.usage", Map.of("%usage%", usage));
    }

    public void sendNoPlayerFound(CommandSender sender, String player) {
        sendMessage(sender, "command.general.error.no-player-found", Map.of("%player%", player));
    }

    public void sendNoPointNameFound(CommandSender sender, String pointName) {
        sendMessage(sender, "command.general.error.no-point-name-found", Map.of("%point-name%", pointName));
    }

    void reload() {
        reloadConfig();
    }
}