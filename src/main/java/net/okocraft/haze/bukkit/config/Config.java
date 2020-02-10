
package net.okocraft.haze.bukkit.config;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class Config extends CustomConfig {

    private static final Config INSTANCE = new Config();

    private Config() {
        super("config.yml");
    }

    public static Config getInstance() {
        return INSTANCE;
    }

    public boolean isUsingBungee() {
        return get().getBoolean("bungee.enabled");
    }

    public long getDataReceivingTimeout() {
        return get().getLong("bungee.data-receiving-timeout");
    }

    /**
     * Reload config. If this method used before {@code JailConfig.save()}, the data
     * on memory will be lost.
     */
    @Override
    public void reload() {
        Bukkit.getOnlinePlayers().forEach(Player::closeInventory);
        super.reload();
    }

    public void reloadAllConfigs() {
        reload();
        Messages.getInstance().reload();
    }
}