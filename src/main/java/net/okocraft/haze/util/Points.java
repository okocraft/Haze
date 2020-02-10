package net.okocraft.haze.util;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface Points {
    
    public boolean add(String pointName);
    public boolean remove(String pointName);
    public boolean set(String pointName, UUID uniqueId, long amount);
    public long get(String pointName, UUID uniqueId);
    public Map<String, Long> get(UUID uniqueId);
    public Set<String> getPoints();
    public Map<String, String> getPlayers();
}