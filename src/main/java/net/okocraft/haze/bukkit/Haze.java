package net.okocraft.haze.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import net.okocraft.haze.bukkit.command.HazeCommand;
import net.okocraft.haze.bukkit.config.Config;
import net.okocraft.haze.util.Points;
import net.okocraft.haze.util.PointsLocal;

/**
 * @author OKOCRAFT
 */
public class Haze extends JavaPlugin implements PluginMessageListener {

    private static Haze instance;
    private BukkitCache cache;
    
    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        Config.getInstance().reloadAllConfigs();
        this.cache = new BukkitCache();

        HazeCommand.getInstance().init();

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PlaceholderAPIBridge().register();
        }

        getLogger().info(String.format("Haze v%s has been enabled!", getVersion()));
    }

    @Override
    public void onDisable() {
        Points pointsInstance = cache.getPointsInstance();
        try {
            if (pointsInstance instanceof PointsLocal) {
                ((PointsLocal) pointsInstance).getSQL().dispose();
            }
        } catch (NoClassDefFoundError ignore) {
            // If PointsLocal is not initialized, there is no class loaded.
        }

        HandlerList.unregisterAll(this);
        Bukkit.getScheduler().cancelTasks(this);

        Bukkit.getServer().getMessenger().unregisterIncomingPluginChannel(this, "haze:tobukkit");
        Bukkit.getServer().getMessenger().unregisterOutgoingPluginChannel(this, "haze:tobungee");

        getLogger().info(String.format("Haze v%s has been disabled!", getVersion()));
    }

    /**
     * このクラスのインスタンスを返す。
     *
     * @return インスタンス
     */
    public static Haze getInstance() {
        if (instance == null) {
            instance = (Haze) Bukkit.getPluginManager().getPlugin("Haze");
        }

        return instance;
    }

    public BukkitCache getCache() {
        return cache;
    }

    public String getVersion() {
        return getInstance().getDescription().getVersion();
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        System.out.println(channel);
    }
}
