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
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import lombok.Getter;
import lombok.NonNull;
import lombok.val;

import net.okocraft.haze.Haze;
import net.okocraft.haze.command.HazeCommand;

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
     */
    public boolean connect(String url) {
        val log = Haze.getLog();

        // Check if driver exists
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException exception) {
            log.error("There's no JDBC driver.");
            exception.printStackTrace();

            return false;
        }

        // Check if the database file exists.
        // If not exist, attempt to create the file.
        try {
            val file = Paths.get(fileUrl);

            if (!Files.exists(file)) {
                Files.createFile(file);
            }
        } catch (IOException exception) {
            log.error("Failed to create database file.");
            exception.printStackTrace();

            return false;
        }

        // Set DB URL
        fileUrl = url;
        DBUrl = "jdbc:sqlite:" + url;

        // Connect to database
        connection = getConnection(DBUrl, DBProps);

        if (!connection.isPresent()) {
            log.error("Failed to connect the database.");

            return false;
        }

        // create table for haze plugin
        return createTable(Haze.getInstance().getName());
    }

    /**
     * テーブルが有るかどうか調べ、なければ作る。
     * 
     * @author LazyGon
     * @since 1.1.0-SNAPSHOT
     * 
     * @param table 新たなテーブルの名前
     * @return SQL文の実行に成功すればtrue 失敗すればfalse
     */
    public boolean createTable(String table){
        return connection.map(connection -> {
            try {
                connection.createStatement().execute(
                        "CREATE TABLE IF NOT EXISTS " + table + " (uuid TEXT PRIMARY KEY NOT NULL, player TEXT NOT NULL)");

                return true;
            } catch (SQLException e) {
                Haze.getLog().error("Failed to initialize database.");
                e.printStackTrace();

                return false;
            }
        }).orElse(false);
    }

    /**
     * テーブルを消す。
     * 
     * @author LazyGon
     * @since 1.1.0-SNAPSHOT
     * 
     * @param table 削除するテーブルの名前
     */
    public void removeTable(String table){
        connection.ifPresent(connection -> {
            try {
                connection.createStatement().execute("DROP TABLE IF EXISTS " + table);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * データベースへの接続を切断する。
     *
     * @since 1.0.0-SNAPSHOT
     * @author akaregi
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
     * @param table 操作するテーブル
     * @param uuid UUID
     * @param name 名前
     */
    public void addRecord(@NonNull String table, @NonNull UUID uuid, @NonNull String name) {
        prepare("INSERT OR IGNORE INTO " + table + " (uuid, player) VALUES (?, ?)").ifPresent(statement -> {
            try {
                statement.setString(1, uuid.toString());
                statement.setString(2, name);
                statement.addBatch();

                // Execute this batch
                threadPool.submit(new StatementRunner(statement));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * テーブルからレコードを削除する。
     *
     * @since 1.1.0-SNAPSHOT
     * @author LazyGon
     *
     * @param table 操作するテーブル
     * @param entry プレイヤー
     */
    public void removeRecord(@NonNull String table, @NonNull String entry) {

        String entryType = HazeCommand.checkEntryType(entry);

        prepare("DELETE FROM " + table + "WHERE " + entryType + " = " + entry).ifPresent(statement -> {
            try {
                statement.setString(1, entry);
                statement.addBatch();

                // Execute this batch
                threadPool.submit(new StatementRunner(statement));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * テーブルのデータベースに名前が記録されているか調べる。
     *
     * @since 1.0.0-SNAPSHOT
     * @author akaregi
     *
     * @param table 操作するテーブル
     * @param uuid UUID
     * @param name 名前
     */
    public boolean hasRecord(@NonNull String table, @NonNull String entry) {
        String entryType = HazeCommand.checkEntryType(entry);

        val statement = prepare("SELECT " + entryType + " FROM " + table + " WHERE " + entryType + " = ?");

        Optional<String> result = statement.map(stmt -> {
            try {
                stmt.setString(1, entry);

                return stmt.executeQuery().getString(entry);
            } catch (SQLException exception) {
                exception.printStackTrace();

                return "";
            }
        });

        return !result.orElse("").equals("");
    }

    /**
     * {@code table}の{@code column}に値をセットする。
     *
     * @since 1.0.0-SNAPSHOT
     * @author LazyGon
     *
     * @param table  操作するテーブル
     * @param column 更新する列
     * @param entry  プレイヤー。uuidでもmcidでも可
     * @param value  新しい値
     */
    public void set(@NonNull String table, @NonNull String column, @NonNull String entry, String value) {

        String entryType = HazeCommand.checkEntryType(entry);

        prepare("UPDATE " + table + " SET " + column + " = ? WHERE " + entryType + " = ?")
                .ifPresent(statement -> {
                    try {
                        statement.setString(1, value);
                        statement.setString(2, entry);
                        statement.addBatch();

                        // Execute this batch
                        threadPool.submit(new StatementRunner(statement));
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                });
    }

    /**
     * {@code table} で指定したテーブルの列 {@code column} の値を取得する。
     * 
     * @author akaregi
     * @since 1.0.0-SNAPSHOT
     * 
     * @param table
     * @param column
     * @param entry
     * @return 値
     */
    public String get(@NonNull String table, String column, String entry) {

        String entryType = HazeCommand.checkEntryType(entry);

        val statement = prepare("SELECT " + column + " FROM " + table + " WHERE " + entryType + " = ?");

        Optional<String> result = statement.map(stmt -> {
            try {
                stmt.setString(1, entry);

                return stmt.executeQuery().getString(column);
            } catch (SQLException exception) {
                exception.printStackTrace();

                return "";
            }
        });

        return result.orElse(":NOTHING");
    }

    /**
     * テーブルに新しい列 {@code column} を追加する。
     *
     * @author akaregi
     * @since 1.0.0-SNAPSHOT
     *
     * @param table  列を追加するテーブル。
     * @param column 列の名前。
     * @param type   列の型。
     *
     * @return 成功したなら {@code true} 、さもなくば {@code false} 。
     */
    public boolean addColumn(String table, String column, String type) {
        val statement = prepare(
                "ALTER TABLE " + table + " ADD " + column + " " + type + " NOT NULL DEFAULT '0'");

        return statement.map(stmt -> {
            try {
                stmt.addBatch();

                // Execute this batch
                threadPool.submit(new StatementRunner(stmt));
                return true;
            } catch (SQLException exception) {
                exception.printStackTrace();
                return false;
            }
        }).orElse(false);
    }

    /**
     * テーブル {@code table} から列 {@code column} を削除する。
     *
     * @author akaregi
     * @since 1.0.0-SNAPSHOT
     *
     * @param table  削除する列を含むテーブル。
     * @param column 削除する列の名前。
     *
     * @return 成功したなら {@code true} 、さもなくば {@code false} 。
     */
    public boolean dropColumn(String table, String column) {

        // 新しいテーブルの列
        StringBuilder columnsBuilder = new StringBuilder();
        getColumnMap(table).forEach((colName, colType) -> {
            if (!column.equals(colName))
                columnsBuilder.append(colName + " " + colType + ", ");
        });
        String columns = columnsBuilder.toString().replaceAll(", $", "");

        // 新しいテーブルの列 (型なし)
        StringBuilder colmunsBuilderExcludeType = new StringBuilder();
        getColumnMap(table).forEach((colName, colType) -> {
            if (!column.equals(colName))
                colmunsBuilderExcludeType.append(colName + ", ");
        });
        String columnsExcludeType = colmunsBuilderExcludeType.toString().replaceAll(", $", "");

        Statement statement;

        try {
            statement = connection.get().createStatement();

            statement.addBatch("BEGIN TRANSACTION");
            statement.addBatch("ALTER TABLE " + table + " RENAME TO temp_" + table + "");
            statement.addBatch("CREATE TABLE " + table + " (" + columns + ")");
            statement.addBatch("INSERT INTO " + table + " (" + columnsExcludeType + ") SELECT "
                    + columnsExcludeType + " FROM temp_" + table + "");
            statement.addBatch("DROP TABLE temp_" + table + "");
            statement.addBatch("COMMIT");

            // Execute this batch
            threadPool.submit(new StatementRunner(statement));
            return true;
        } catch (SQLException exception) {
            exception.printStackTrace();
            return false;
        }
    }

    /**
     * テーブルに含まれる列 {@code column} のリストを取得する。
     *
     * @author LazyGon
     * @since 1.0.0-SNAPSHOT
     *
     * @param table 調べるテーブル。
     *
     * @return テーブルに含まれるcolumnの名前と型のマップ
     */
    public Map<String, String> getColumnMap(String table) {
        val statement = prepare("SELECT * FROM " + table + " WHERE 0=1");

        Map<String, String> columnMap = new HashMap<>();

        return statement.map(stmt -> {
            try {
                ResultSetMetaData rsmd = stmt.executeQuery().getMetaData();

                for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                    columnMap.put(rsmd.getColumnName(i), rsmd.getColumnTypeName(i));
                }

                return columnMap;
            } catch (SQLException exception) {
                exception.printStackTrace();
                return new HashMap<String, String>();
            }
        }).orElse(columnMap);
    }

    /**
     * すべてのテーブル名前と型のマップを取得する。
     *
     * @author LazyGon
     * @since 1.1.0-SNAPSHOT
     *
     * @return テーブル名と型のマップ
     */
    public Map<String, String> getTableMap() {

        Map<String, String> tableMap = new HashMap<>();

        return connection.map(con -> {
            try {
                ResultSet resultSet = con.getMetaData().getTables(null, null, null, new String[]{"TABLE"});

                while(resultSet.next())
                    tableMap.put(resultSet.getString("TABLE_NAME"), resultSet.getString("TABLE_TYPE"));

                return tableMap;
            } catch (SQLException exception) {
                exception.printStackTrace();
                return new HashMap<String, String>();
            }
        }).orElse(tableMap);
    }

    /**
     * 登録されているプレイヤーの名前とUUIDのマップを取得する。
     *
     * @author LazyGon
     * @since 1.1.0-SNAPSHOT
     *
     * @return プレイヤー名とそのUUIDのマップ
     */
    public Map<String, String> getPlayersMap(String table) {

        Map<String, String> playersMap = new HashMap<>();

        val statement = prepare("SELECT uuid, player FROM " + table);

        statement.ifPresent(stmt -> {
            try {
                ResultSet rs = stmt.executeQuery();
                while (rs.next())
                    playersMap.put(rs.getString("uuid"), rs.getString("player"));
            } catch (SQLException exception) {
                exception.printStackTrace();
            }
        });

        return playersMap;
    }

    /**
     * 指定したテーブルの、指定したプレイヤーの全カラムのデータをリストで取得する
     *
     * @author LazyGon
     * @since 1.1.0-SNAPSHOT
     *
     * @param table 調べるテーブル。
     * @param player 調べるプレイヤー。uuidでもmcidでも可。
     *
     * @return データのリスト
     */
    public List<Object> getAllData(String table, String player) {

        String entryType = HazeCommand.checkEntryType(player);

        val statement = prepare("SELECT * FROM " + table + " WHERE " + entryType + " = ?");

        List<Object> allDataList = new ArrayList<>();

        statement.ifPresent(stmt -> {
            try {
                stmt.setString(1, player);

                ResultSet rs = stmt.executeQuery();
                ResultSetMetaData rsmd = rs.getMetaData();

                for (int i = 1; i <= rsmd.getColumnCount(); i++){
                    allDataList.add(rs.getString(rsmd.getColumnName(i)));
                    if (rs.wasNull())
                        allDataList.remove(allDataList.size() - 1);
                }

            } catch (SQLException exception) {
                exception.printStackTrace();
            }
        });

        return allDataList;
    }

    /**
     * {@code table} の {@code column} の {@code entry} の行をNULLにする。(消す)
     * 
     * @author LazyGon
     * @since 1.1.0-SNAPSHOT
     * 
     * @param table
     * @param column
     * @param entry
     */
    public void removeValue(@NonNull String table, String column, String entry) {

        String entryType = HazeCommand.checkEntryType(entry);

        val statement = prepare("UPDATE " + table + " SET " + column + " = NULL WHERE " + entryType + " = ?");

        statement.ifPresent(stmt -> {
            try {
                stmt.setString(1, entry);
                stmt.executeUpdate();
            } catch (SQLException exception) {
                exception.printStackTrace();
            }
        });
    }

    /**
     * スレッド上で SQL を実行する。
     *
     * @author akaregi
     * @since 1.0.0-SNAPSHOT
     *
     * @param statement SQL 準備文。
     *
     * @return {@Code ResultSet}
     */
    public Optional<ResultSet> exec(PreparedStatement statement) {
        val thread = threadPool.submit(new StatementCaller(statement));

        try {
            return thread.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();

            return Optional.empty();
        }
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
