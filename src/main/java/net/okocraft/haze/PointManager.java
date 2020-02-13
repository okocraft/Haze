package net.okocraft.haze;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;

import net.okocraft.haze.config.Config;
import net.okocraft.sqlibs.ColumnType;
import net.okocraft.sqlibs.SQLibs;

public class PointManager {

    private static final String TABLE = "haze_points";
    private SQLibs sqlibs;

    public PointManager() {
        try {
            if (Config.getInstance().isUsingMySQL()) {
                sqlibs = new SQLibs(Config.getInstance().getMySQLConfig());
            } else {
                Path dbPath = Haze.getInstance().getDataFolder().toPath().resolve("database.db");
                sqlibs = new SQLibs(dbPath);
            }
        } catch (SQLException e) {
            Haze.getInstance().getLogger().log(Level.SEVERE, "Cannot connect to database.", e);
            Bukkit.getPluginManager().disablePlugin(Haze.getInstance());     
        }

        if (!sqlibs.getTables().contains(TABLE)) {
            sqlibs.createTable(TABLE, "uuid", ColumnType.TEXT);
            sqlibs.addColumn(TABLE, "player", ColumnType.TEXT, true);
        }
    }
    
    SQLibs getSQL() {
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

        if (!getPlayers().containsKey(uniqueId.toString())) {
            updatePlayer(uniqueId, Optional.ofNullable(Bukkit.getOfflinePlayer(uniqueId).getName()).orElse("null"));
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