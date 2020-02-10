package net.okocraft.haze.bukkit;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.okocraft.haze.util.Points;

public class PointsBungee implements Points {

    private static Points instance;
    
    private PluginMessages pluginMessages = PluginMessages.getInstance();

    public PointsBungee() {
        if (instance != null) {
            throw new IllegalStateException("The Points is already instantiated. Use getInstance method.");
        }
        
        instance = this;
    }

    public static Points getInstance() {
        if (instance == null) {
            throw new IllegalStateException(
                    "The Points is not instantiated yet. Construct it before getting instance.");
        }

        return instance;
    }

    @Override
    public boolean add(String pointName) {
        List<Object> response = pluginMessages.getResponse("add", pointName);
        if (!checkSingleResponse(response, Boolean.class)) {
            return false;
        }

        return (boolean) response.get(0);
    }

    @Override
    public boolean remove(String pointName) {
        List<Object> response = pluginMessages.getResponse("remove", pointName);
        if (!checkSingleResponse(response, Boolean.class)) {
            return false;
        }

        return (boolean) response.get(0);
    }

    @Override
    public boolean set(String pointName, UUID uniqueId, long amount) {
        List<Object> response = pluginMessages.getResponse("set", pointName, uniqueId.toString(), amount);
        if (!checkSingleResponse(response, Boolean.class)) {
            return false;
        }

        return (boolean) response.get(0);
    }

    @Override
    public long get(String pointName, UUID uniqueId) {
        List<Object> response = pluginMessages.getResponse("get", pointName, uniqueId.toString());
        if (!checkSingleResponse(response, Boolean.class)) {
            return 0L;
        }

        return (long) response.get(0);
    }

    @Override
    public Map<String, Long> get(UUID uniqueId) {
        try {
            Map<String, Long> result = new HashMap<>();
            for (Object element : pluginMessages.getResponse("get", uniqueId.toString())) {
                if (element instanceof String) {
                    String[] response = ((String) element).split(" ", 2);
                    result.put(response[0], Long.parseLong(response[1]));
                }
            }
            return result;
        } catch (NumberFormatException e) {
            return Map.of();
        }

    }

    @Override
    public Set<String> getPoints() {
        Set<String> result = new HashSet<>();
        List<Object> response = pluginMessages.getResponse("getPoints");
        for (Object pointName : response) {
            if (pointName instanceof String) {
                result.add((String) pointName);
            }
        }

        return result;
    }

    @Override
    public Map<String, String> getPlayers() {
        Map<String, String> result = new HashMap<>();
        List<Object> response = pluginMessages.getResponse("getPlayers");
        if (!checkSingleResponse(response, String.class)) {
            return Map.of();
        }

        String[] data = ((String) response.get(0)).split(" ", -1);
        for (int i = 0; i + 1 < data.length; i += 2) {
            result.put(data[i], data[i + 1]);
        }

        return result;
    }

    private boolean checkSingleResponse(List<Object> response, Class<?> expectedReturnType) {
        return response.size() != 0 && response.get(0).getClass() == expectedReturnType;
    }
}