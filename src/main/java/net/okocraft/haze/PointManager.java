package net.okocraft.haze;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import net.okocraft.sqlibs.ColumnType;
import net.okocraft.sqlibs.SQLibs;

public class PointManager {

    private static final String TABLE = "haze_points";

    static PointManager instance;
    private SQLibs sqlibs;

    public PointManager(Path databaseFile) {
        if (instance != null) {
            throw new IllegalStateException("The Points is already instantiated. Use getInstance method.");
        }

        instance = this;
        sqlibs = new SQLibs(databaseFile);
        
        sqlibs.createTable(TABLE, "uuid", ColumnType.TEXT);
        sqlibs.addColumn(TABLE, "player", ColumnType.TEXT, true);
    }
    
    public SQLibs getSQL() {
        return sqlibs;
    }

    static String getTableName() {
        return TABLE;
    }

    public void updatePlayer(UUID uuid, String name) {
        // 昔同じ名前のプレイヤーがログインしてた場合、古い方を消す。
        sqlibs.executeSQL("UPDATE OR IGNORE " + TABLE + " SET player = '' WHERE uuid = '" + uuid + "'");
        sqlibs.executeSQL("UPDATE OR IGNORE " + TABLE + " SET player = '' WHERE player = '" + name + "'");

        // 新しいプレイヤーだったら登録し、すでにuuidが登録されている場合はプレイヤーの名前だけ上書きする。
        sqlibs.executeSQL("INSERT INTO " + TABLE + " (uuid, player) VALUES ('" + uuid + "', '" + name
                + "') ON CONFLICT(uuid) DO UPDATE SET player = '" + name + "' WHERE uuid = '" + uuid + "'");
    }

    public boolean add(String pointName) {
        if (getPoints().contains(pointName)) {
            return false;
        }

        return sqlibs.addColumn(TABLE, pointName, ColumnType.INTEGER, true);
    }

    public boolean remove(String pointName) {
        if (!getPoints().contains(pointName)) {
            return false;
        }

        return sqlibs.dropColumn(TABLE, pointName);
    }

    public boolean set(String pointName, UUID uniqueId, long amount) {
        if (!getPoints().contains(pointName)) {
            return false;
        }

        return sqlibs.set(TABLE, uniqueId.toString(), pointName, String.valueOf(amount));
    }

    public long get(String pointName, UUID uniqueId) {
        if (!getPoints().contains(pointName)) {
            return 0L;
        }

        try {
            return Long.parseLong(sqlibs.get(TABLE, uniqueId.toString(), pointName));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    public Map<String, Long> get(UUID uniqueId) {
        Map<String, Long> result = new HashMap<>();
        try {
            sqlibs.get(TABLE, uniqueId.toString(), getPoints()).forEach((k, v) -> result.put(k, Long.parseLong(v)));
            return result;
        } catch (NumberFormatException e) {
            return Map.of();
        }
    }

    public Set<String> getPoints() {
        return sqlibs.getColumnMap(TABLE).entrySet().stream()
                .filter(entry -> entry.getValue() == ColumnType.INTEGER)
                .map(Map.Entry::getKey).collect(Collectors.toSet());
    }

    public Map<String, String> getPlayers() {
        return sqlibs.getValues(TABLE, "player");
    }

    public boolean increase(String point, UUID player, long amount) {
        return set(point, player, get(point, player) + amount);
    }

    public boolean decrease(String point, UUID player, long amount) {
        return set(point, player, get(point, player) - amount);
    }
}