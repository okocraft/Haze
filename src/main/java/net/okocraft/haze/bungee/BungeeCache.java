package net.okocraft.haze.bungee;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.event.EventHandler;
import net.okocraft.haze.util.PointsLocal;
import net.okocraft.haze.util.Utilities;

public class BungeeCache implements Listener {

    private static final Haze PLUGIN = Haze.getInstance();
    private static final PointsLocal POINTS = new PointsLocal(PLUGIN.getDataFolder().toPath().resolve("data.db"));

    private final Configuration data = new Configuration();

    BungeeCache() {
        setPoints(POINTS.getPoints());
        ProxyServer.getInstance().getPluginManager().registerListener(PLUGIN, this);
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

    public long get(UUID player, String point) {
        if (!getPoints().contains(point)) {
            new IllegalArgumentException("The point " + point + " is not registered.");
        }

        if (isCached(player)) {
            return data.getLong(player.toString() + "." + point, 0L);
        } else {
            return POINTS.get(point, player);
        }
    }

    public boolean increase(UUID player, String point, long amount) {
        return set(player, point, get(player, point) + amount);
    }

    public boolean decrease(UUID player, String point, long amount) {
        return set(player, point, get(player, point) - amount);
    }

    public boolean set(UUID player, String point, long amount) {
        if (!getPoints().contains(point)) {
            new IllegalArgumentException("The point " + point + " is not registered.");
        }

        if (POINTS.set(point, player, amount)) {
            data.set(player.toString() + "." + point, amount);
            return true;
        }

        return false;
    }

    PointsLocal getPointsInstance() {
        return POINTS;
    }

    private void cache(UUID player) {
        String uuid = player.toString();
        POINTS.get(player).forEach((name, amount) -> {
            if (amount != 0) {
                data.set(uuid + "." + name, amount);
            }
        });
    }

    private void uncache(UUID player) {
        data.set(player.toString(), null);
    }

    private boolean isCached(UUID player) {
        return data.contains(player.toString());
    }

    @EventHandler
    public void onJoin(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();
        ((PointsLocal) POINTS).updatePlayer(player.getUniqueId(), player.getName());
        cache(player.getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerDisconnectEvent event) {
        uncache(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPluginMesssage(PluginMessageEvent event) {
        if (!event.getTag().equals("haze:tobungee")) {
            return;
        }
        
        ProxyServer.getInstance().getLogger().info("haze:tobungee catched data length is: " + event.getData().length);
        
        List<Object> data = Utilities.readByteArray(event.getData());
        ProxyServer.getInstance().getLogger().info("haze:tobungee data list length is: " + data.size());
        
        if (data.size() == 0 || !(data.get(0) instanceof Long)) {
            System.out.println("return not long (timestump) catched type is: " + data.get(0).getClass().getSimpleName());
            if (data.get(0) instanceof String) {
                System.out.println("contents is : " + (String) data.get(0));
            }
            return;
        }
        
        long timeStamp = (long) data.get(0);
        System.out.println(timeStamp);
        
        if (data.size() == 1 || !(data.get(1) instanceof String)) {
            System.out.println("return not String (method) " + data.get(1).getClass().getSimpleName());
            return;
        }
        
        String method = (String) data.get(1);
        System.out.println(method);
        byte[] response = null;
        
        if (method.equals("add")) {
            if (data.size() == 2 || !(data.get(2) instanceof String)) {
                System.out.println("not enough arg (set");
                System.out.println("or not str");
                return;
            }
            
            response = Utilities.createByteArray(timeStamp, addPoint((String) data.get(1)));
            
        } else if (method.equals("remove")) {
            if (data.size() == 2 || !(data.get(2) instanceof String)) {
                System.out.println("not enough arg (remove");
                System.out.println("or not str");
                return;
            }
            
            response = Utilities.createByteArray(timeStamp, removePoint((String) data.get(2)));
            
        } else if (method.equals("set")) {
            if (data.size() < 5) {
                System.out.println("not enough arg (set");
                return;
            }
            
            if (!(data.get(2) instanceof String)) {
                return;
            }
            
            if (!(data.get(3) instanceof String)) {
                return;
            }
            
            if (!(data.get(4) instanceof Long)) {
                return;
            }

            String pointName = (String) data.get(2);
            
            UUID player;
            try {
                player = UUID.fromString((String) data.get(3));
            } catch (IllegalArgumentException e) {
                return;
            }

            Long amount = (Long) data.get(4);

            response = Utilities.createByteArray(timeStamp, set(player, pointName, amount));
        
        } else if (method.equals("get")) {
            if (data.size() == 3) {
                if (!(data.get(2) instanceof String)) {
                    return;
                }

                UUID player;
                try {
                    player = UUID.fromString((String) data.get(2));
                } catch (IllegalArgumentException e) {
                    return;
                }

                List<String> result = new ArrayList<>();
                
                for (String pointName : getPoints()) {
                    result.add(pointName + " " + get(player, pointName));
                }

                response = Utilities.createByteArray(Utilities.addFirst(timeStamp, result.toArray()));
            } else if (data.size() == 4) {
                if (!(data.get(2) instanceof String || data.get(3) instanceof String)) {
                    return;
                }

                String pointName = (String) data.get(2);
                UUID player;
                try {
                    player = UUID.fromString((String) data.get(3));
                } catch (IllegalArgumentException e) {
                    return;
                }

                response = Utilities.createByteArray(timeStamp, get(player, pointName));
            } else {
                return;
            }

        } else if (method.equals("getPoints")) {
            response = Utilities.createByteArray(Utilities.addFirst(timeStamp, getPoints().toArray()));

        } else if (method.equals("getPlayers")) {
            StringBuilder builder = new StringBuilder();
            POINTS.getPlayers().forEach((uuid, name) -> builder.append(uuid).append(" ").append(name));
            response = Utilities.createByteArray(timeStamp, builder.toString());
        }

        if (response == null) {
            return;
        }

        for (ServerInfo server : ProxyServer.getInstance().getServers().values()) {
            
            server.sendData("haze:tobukkit", response, false);
        }
    }
}

// points:
// - point-1
// - point-2
// - point-3
// - point-4
// - point-5
// 00000000-0000-0000-0000-000000000000:
// point-name-1: amount1
// point-name-2: amount2
// point-name-3: amount3
// 00000000-0000-0000-0000-000000000001:
// point-name-1: amount1
// point-name-2: amount2
// point-name-3: amount3