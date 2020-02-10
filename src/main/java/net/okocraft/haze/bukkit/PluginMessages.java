package net.okocraft.haze.bukkit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import net.okocraft.haze.util.Utilities;

/**
 * Plugin message utilities for Bukkit side.
 */
public class PluginMessages implements PluginMessageListener {

    private static final Haze PLUGIN = Haze.getInstance(); 
    
    /** In second. */
    private static final int TIME_OUT = 3;
    
    private static final PluginMessages INSTANCE = new PluginMessages();

    private final Map<Long, List<Object>> responses = new HashMap<>();

    private PluginMessages() {
        Bukkit.getServer().getMessenger().registerIncomingPluginChannel(PLUGIN, "haze:tobukkit", this);
        Bukkit.getServer().getMessenger().registerOutgoingPluginChannel(PLUGIN, "haze:tobungee");
    }

    static PluginMessages getInstance() {
        return INSTANCE;
    }

    /**
     * Send plugin message and wait for response synchronously. If response is not returned in {@code TIME_OUT} seconds, method returns empty list.
     * 
     * @param messages plugin message.
     * @return response
     */
    List<Object> getResponse(Object ... messages) {
        if (responses.isEmpty()) {
            System.out.println("there is not received plugin message.");
        } else {
            System.out.println("received plugin messages are:");
            responses.keySet().forEach(timeS -> System.out.println(timeS));
        }

        Bukkit.getServer().getMessenger().dispatchIncomingMessage(null, null, null);

        long now = System.currentTimeMillis();
        sendPluginMessage(Utilities.addFirst(now, messages));
        while (!responses.containsKey(now) && System.currentTimeMillis() < now + TIME_OUT * 1000);
        List<Object> result = responses.remove(now);
        if (result == null) {
            PLUGIN.getLogger().severe("Bungeecord do not return response. Please make sure Bungeecord is running and haze is installed on it.");
        }
        return List.of();
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("haze:tobukkit")) {
            return;
        }

        // EVERYTHING IS ON MAIN THREAD. THERE IS NO WAY TO WAIT RESPONSE.
        System.out.println("is primary thread" + Bukkit.isPrimaryThread());

        System.out.println("catched pm from bungee");

        List<Object> result = Utilities.readByteArray(message);
        if (result.get(0) instanceof Long) {
            responses.put((long) result.remove(0), result);
        }
    }

    void sendPluginMessage(Object... messages) {
        Bukkit.getServer().sendPluginMessage(PLUGIN, "haze:tobungee", Utilities.createByteArray(messages));
    }
}