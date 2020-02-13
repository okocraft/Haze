package net.okocraft.haze.listener;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import net.okocraft.haze.Haze;

public class PlayerListener implements Listener {

    private static PlayerListener instance;

    private PlayerListener() {
    }

    public static void start() {
        if (instance == null) {
            instance = new PlayerListener();
            Bukkit.getPluginManager().registerEvents(instance, Haze.getInstance());
        }
    }

    public static void stop() {
        if (instance != null) {
            HandlerList.unregisterAll(instance);
            instance = null;
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Haze.getInstance().getPointManager().updatePlayer(event.getPlayer().getUniqueId(), event.getPlayer().getName());
    }
}