/*
 * This file is a part of Haze.
 *
 * Haze, Player's Point Manager. Copyright (C) 2019 OKOCRAFT
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If
 * not, see <https://www.gnu.org/licenses/>.
 */

package net.okocraft.haze.db;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import lombok.Getter;
import lombok.val;
import org.bukkit.entity.Player;

public class HazeDB {
    final Properties DBProp;

    @Getter
    final String fileUrl;

    /**
     * データベースへの URL 。{@code jdbc:sqlite:database}
     */
    @Getter
    final String DBUrl;

    /**
     * データベースへの接続。
     */
    private Connection connection;

    public HazeDB(String url) {
        // Configure SQLite properties
        DBProp = new Properties();
        DBProp.put("journal_mode", "WAL");
        DBProp.put("synchronous", "NORMAL");

        // Set DB URL
        fileUrl = url;
        DBUrl = "jdbc:sqlite:" + url;

        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException exception) {
            exception.printStackTrace();
        }
    }

    /**
     * データベースの初期化を行う。
     *
     * <p>
     * データベースのファイル自体が存在しない場合はファイルを作成する。 ファイル内になんらデータベースが存在しない場合、データベースを新たに生成する。
     *
     * @since 1.0.0-SNAPSHOT
     * @author akaregi
     *
     * @throws IOException  {@link HazeDB#HazeDB(String)}
     * @throws SQLException {@link HazeDB#HazeDB(String)}
     *
     */
    public void initialize() throws IOException, SQLException {
        connection = getConnection(DBUrl, DBProp);
        connection.setAutoCommit(true);

        val file = Paths.get(fileUrl);

        if (!Files.exists(file)) {
            Files.createFile(file);
        }

        connection.createStatement().executeUpdate(
                "CREATE TABLE IF NOT EXISTS haze (uuid TEXT PRIMARY KEY NOT NULL, player TEXT NOT NULL)");
    }

    /**
     * データベースの接続を切断する。
     *
     * @since 1.0.0-SNAPSHOT
     * @author akaregi
     *
     * @throws SQLException 切断に失敗した場合
     */
    public void destruct() throws SQLException {
        connection.close();
    }

    /**
     * WIP: データベースにレコードを追加する。
     *
     * @since 1.0.0-SNAPSHOT
     * @author akaregi
     *
     * @param player プレイヤー
     * @param value  値
     *
     * @throws SQLException レコードの追加に失敗した場合
     *
     */
    public synchronized void addRecord(Player player, String value) throws SQLException {
        connection.setAutoCommit(false);

        val uuid = player.getUniqueId().toString();
        val name = player.getName();

        val statement = connection.prepareStatement("INSERT OR IGNORE INTO haze VALUES (?, ?)");

        statement.setString(1, uuid);
        statement.setString(2, name);
        statement.addBatch();

        statement.executeBatch();

        connection.setAutoCommit(true);
    }

    /**
     * {@link DriverManager#getConnection(String, Properties)} のラッパーメソッド。
     *
     * @since 1.0.0-SNAPSHOT
     * @author akaregi
     *
     * @see DriverManager#getConnection(String, Properties)
     *
     * @param url   {@code jdbc:subprotocol:subname} という形式のデータベース URL
     * @param props データベースの取り扱いについてのプロパティ
     *
     * @return 指定されたデータベースへの接続。
     *
     */
    private static Connection getConnection(String url, Properties props) throws SQLException {
        return DriverManager.getConnection(url, props);
    }
}
