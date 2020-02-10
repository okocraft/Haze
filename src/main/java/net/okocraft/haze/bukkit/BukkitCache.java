package net.okocraft.haze.bukkit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import net.okocraft.haze.bukkit.config.Config;
import net.okocraft.haze.util.Points;
import net.okocraft.haze.util.PointsLocal;

public class BukkitCache implements Listener {

    private static final Haze PLUGIN = Haze.getInstance();
    private static final Points POINTS = Config.getInstance().isUsingBungee()
            ? new PointsBungee()
            : new PointsLocal(PLUGIN.getDataFolder().toPath().resolve("data.db"));

    private final Configuration data = new MemoryConfiguration();

    BukkitCache() {
        PLUGIN.getLogger().info((POINTS instanceof PointsBungee) ? "BungeeCord Mode" : "Bukkit Mode");
        Bukkit.getPluginManager().registerEvents(this, PLUGIN);
    }

    public Set<String> getPoints() {
        return new HashSet<>(data.getStringList("points"));
    }

    private void setPoints(Set<String> points) {
        data.set("points", new ArrayList<>(points));
    }

    public boolean addPoint(String point) {
        if (POINTS.add(point)) {
            Set<String> points = getPoints();
            points.add(point);
            setPoints(points);
            return true;
        }

        return false;
    }

    public boolean removePoint(String point) {
        if (POINTS.remove(point)) {
            Set<String> points = getPoints();
            points.remove(point);
            setPoints(points);
            return true;
        }

        return false;
    }

    public long get(String point, OfflinePlayer player) {
        if (!getPoints().contains(point)) {
            new IllegalArgumentException("The point " + point + " is not registered.");
        }

        if (isCached(player)) {
            return data.getLong(player.getUniqueId().toString() + "." + point, 0L);
        } else {
            return POINTS.get(point, player.getUniqueId());
        }
    }

    public boolean increase(String point, OfflinePlayer player, long amount) {
        return set(point, player, get(point, player) + amount);
    }

    public boolean decrease(String point, OfflinePlayer player, long amount) {
        return set(point, player, get(point, player) - amount);
    }

    public boolean set(String point, OfflinePlayer player, long amount) {
        if (!getPoints().contains(point)) {
            new IllegalArgumentException("The point " + point + " is not registered.");
        }

        if (POINTS.set(point, player.getUniqueId(), amount)) {
            data.set(player.getUniqueId().toString() + "." + point, amount);
            return true;
        }

        return false;
    }

    public Points getPointsInstance() {
        return POINTS;
    }

    private void cache(OfflinePlayer player) {
        String uuid = player.getUniqueId().toString();
        POINTS.get(player.getUniqueId()).forEach((name, amount) -> {
            if (amount != 0) {
                data.set(uuid + "." + name, amount);
            }
        });

        data.getKeys(true).forEach(System.out::println);
        System.out.println(player.getUniqueId().toString() + " is logged in!");
    }

    private void uncache(OfflinePlayer player) {
        data.set(player.getUniqueId().toString(), null);
    }

    private boolean isCached(OfflinePlayer player) {
        return data.contains(player.getUniqueId().toString());
    }

    @EventHandler
    private void onJoin(PlayerJoinEvent event) {
        
        if (Bukkit.getOnlinePlayers().size() == 1) {
            new BukkitRunnable(){
            
                @Override
                public void run() {
                    setPoints(POINTS.getPoints());
                }
            }.runTaskLater(PLUGIN, 10L);
        }
        
        if (POINTS instanceof PointsLocal) {
            Player player = event.getPlayer();
            ((PointsLocal) POINTS).updatePlayer(player.getUniqueId(), player.getName());
            cache(player);
        }
    }

    @EventHandler
    private void onQuit(PlayerQuitEvent event) {
        uncache(event.getPlayer());
    }
}


// points:
//   - point-1
//   - point-2
//   - point-3
//   - point-4
//   - point-5
// 00000000-0000-0000-0000-000000000000:
//   point-name-1: amount1
//   point-name-2: amount2
//   point-name-3: amount3
// 00000000-0000-0000-0000-000000000001:
//   point-name-1: amount1
//   point-name-2: amount2
//   point-name-3: amount3
