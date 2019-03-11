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

package net.okocraft.haze.database;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import lombok.Getter;
import lombok.NonNull;
import lombok.val;

public class Database {
    /**
     * データベルファイルへの URL 。{@code plugins/Haze/data.db}
     */
    @Getter
    private String fileUrl;

    /**
     * データベースへの URL 。{@code jdbc:sqlite:database}
     */
    @Getter
    private String DBUrl;

    /**
     * データベース接続のプロパティ
     */
    private final Properties DBProps;

    /**
     * データベースの参照用スレッドプール
     */
    private final ExecutorService threadPool;

    /**
     * データベースへの接続。
     */
    private static Optional<Connection> connection;

    public Database() {
        // Configure database properties
        DBProps = new Properties();
        DBProps.put("journal_mode", "WAL");
        DBProps.put("synchronous", "NORMAL");

        // Create new thread pool
        threadPool = Executors.newSingleThreadExecutor();
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
     */
    public void connect(String url) {
        // Check if driver exists
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException exception) {
            exception.printStackTrace();

            return;
        }

        // Set DB URL
        fileUrl = url;
        DBUrl = "jdbc:sqlite:" + url;

        // Connect to database
        connection = getConnection(DBUrl, DBProps);

        // Check if the database file exists.
        // If not exist, attempt to create the file.
        try {
            val file = Paths.get(fileUrl);

            if (!Files.exists(file)) {
                Files.createFile(file);
            }
        } catch (IOException exception) {
            exception.printStackTrace();

            return;
        }

        // Check if the table exists.
        // If not exist, attempt to create the table.
        connection.ifPresent(connection -> {
            try {
                connection.createStatement().execute(
                        "CREATE TABLE IF NOT EXISTS haze (uuid TEXT PRIMARY KEY NOT NULL, player TEXT NOT NULL)");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * データベースの接続を切断する。
     *
     * @since 1.0.0-SNAPSHOT
     * @author akaregi
     *
     */
    public void dispose() {
        connection.ifPresent(connection -> {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });

        connection = Optional.empty();
    }

    /**
     * WIP: データベースにレコードを追加する。
     *
     * @since 1.0.0-SNAPSHOT
     * @author akaregi
     *
     * @param uuid UUID
     * @param name 名前
     *
     */
    public void addRecord(@NonNull UUID uuid, @NonNull String name) {
        prepare("INSERT OR IGNORE INTO haze VALUES (?, ?)").ifPresent(statement -> {
            try {
                statement.setString(1, uuid.toString());
                statement.setString(2, name);
                statement.addBatch();

                // Execute this batch
                exec(statement);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public String readRecord(String uuid) {
        val statement = prepare("SELECT player FROM haze WHERE uuid = ?");

        Optional<String> result = statement.map(stmt -> {
            try {
                stmt.setString(1, uuid);
                stmt.addBatch();

                return stmt.executeQuery().getString("player");
            } catch (SQLException exception) {
                exception.printStackTrace();

                return "";
            }
        });

        return result.orElse(":NOTHING");
    }

    /**
     * スレッドプールにて SQL 準備文を実行する。
     *
     * @since 1.0.0-SNAPSHOT
     * @author akaregi
     *
     * @param statement SQL 準備文
     *
     */
    private Future<?> exec(@NonNull PreparedStatement statement) {
        return threadPool.submit(new StatementRunner(statement));
    }

    /**
     * SQL 準備文を構築する。
     *
     * @author akaregi
     * @since 1.0.0-SNAPSHOT
     *
     * @param sql SQL 文。
     *
     * @return SQL 準備文
     *
     */
    public Optional<PreparedStatement> prepare(@NonNull String sql) {
        if (connection.isPresent()) {
            try {
                return Optional.of(connection.get().prepareStatement(sql));
            } catch (SQLException e) {
                e.printStackTrace();

                return Optional.empty();
            }
        }

        return Optional.empty();
    }

    /**
     * Connection(String, Properties)} のラッパーメソッド。
     *
     * @since 1.0.0-SNAPSHOT
     * @author akaregi
     *
     * @see DriverManager#getConnection(String, Properties)
     *
     * @param url   {@code jdbc:subprotocol:subname} という形式のデータベース URL
     * @param props データベースの取り扱いについてのプロパティ
     *
     * @return 指定されたデータベースへの接続 {@code Connect} 。
     *
     */
    private static Optional<Connection> getConnection(@NonNull String url, Properties props) {
        try {
            return Optional.of(DriverManager.getConnection(url, props));
        } catch (SQLException exception) {
            exception.printStackTrace();

            return Optional.empty();
        }
    }
}
