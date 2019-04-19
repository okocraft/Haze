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
import java.util.logging.Logger;
import java.util.Map;
import java.util.HashMap;

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

    /**
     * ロガー
     */
    private static Logger log;

    public Database() {
        // Configure database properties
        DBProps = new Properties();
        DBProps.put("journal_mode", "WAL");
        DBProps.put("synchronous", "NORMAL");

        // Create new thread pool
        threadPool = Executors.newSingleThreadExecutor();

        log = Haze.getInstance().getLog();
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
        // Check if driver exists
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException exception) {
            //log.error("There's no JDBC driver.");
            log.severe("There's no JDBC driver.");
            exception.printStackTrace();

            return false;
        }

        
        // Set DB URL
        fileUrl = url;
        DBUrl = "jdbc:sqlite:" + url;

        // Check if the database file exists.
        // If not exist, attempt to create the file.
        try {
            val file = Paths.get(fileUrl);

            if (!Files.exists(file)) {
                Files.createFile(file);
            }
        } catch (IOException exception) {
            //log.error("Failed to create database file.");
            log.severe("Failed to create database file.");
            exception.printStackTrace();

            return false;
        }

        // Connect to database
        connection = getConnection(DBUrl, DBProps);

        if (!connection.isPresent()) {
            //log.error("Failed to connect the database.");
            log.severe("Failed to connect the database.");

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
                //Haze.getLog().error("Failed to initialize database.");
                Haze.getInstance().getLog().severe("Failed to initialize database.");
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
     * 失敗した場合はコンソールにログを出力する。
     *
     * @since 1.0.0-SNAPSHOT
     * @author akaregi
     *
     * @param table 操作するテーブル
     * @param uuid UUID
     * @param name 名前
     * 
     * @return 成功すればtrue 失敗すればfalse
     */
    public boolean addRecord(@NonNull String table, @NonNull UUID uuid, @NonNull String name) {

        if (!getTableMap().keySet().contains(table)){
            log.warning(":NO_TABLE_EXIST");
            return false;
        }

        if (hasRecord(table, name)){
            log.warning(":RECORD_EXIST");
            return false;
        }

        return prepare("INSERT OR IGNORE INTO " + table + " (uuid, player) VALUES (?, ?)").map(statement -> {
            try {
                statement.setString(1, uuid.toString());
                statement.setString(2, name);
                statement.addBatch();

                // Execute this batch
                threadPool.submit(new StatementRunner(statement));
                return true;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }).orElse(false);
    }

    /**
     * テーブルからレコードを削除する。
     * 失敗した場合はコンソールにログを出力する。
     *
     * @since 1.1.0-SNAPSHOT
     * @author LazyGon
     *
     * @param table 操作するテーブル
     * @param entry プレイヤー
     * 
     * @return 成功すればtrue 失敗すればfalse
     */
    public boolean removeRecord(@NonNull String table, @NonNull String entry) {

        if (!getTableMap().keySet().contains(table)){
            log.warning(":NO_TABLE_EXIST");
            return false;
        }

        if (!hasRecord(table, entry)){
            log.warning(":NO_RECORD_EXIST");
            return false;
        }

        String entryType = HazeCommand.checkEntryType(entry);

        return prepare("DELETE FROM " + table + " WHERE " + entryType + " = ?").map(statement -> {
            try {
                statement.setString(1, entry);
                statement.addBatch();

                // Execute this batch
                threadPool.submit(new StatementRunner(statement));
                return true;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }).orElse(false);
    }

    /**
     * テーブルのデータベースに名前が記録されているか調べる。
     *
     * @since 1.0.0-SNAPSHOT
     * @author akaregi
     *
     * @param table 操作するテーブル
     * @param entry uuidでもmcidでも可
     */
    public boolean hasRecord(@NonNull String table, @NonNull String entry) {

        if (!getTableMap().keySet().contains(table)){
            log.warning(":NO_TABLE_EXIST");
            return false;
        }

        String entryType = HazeCommand.checkEntryType(entry);

        val statement = prepare("SELECT " + entryType + " FROM " + table + " WHERE " + entryType + " = ?");

        Optional<String> result = statement.map(stmt -> {
            try {
                stmt.setString(1, entry);

                return stmt.executeQuery().getString(entryType);
            } catch (SQLException exception) {
                if (!exception.getMessage().equals("ResultSet closed"))
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
    public boolean set(@NonNull String table, @NonNull String column, @NonNull String entry, String value) {

        if (!getTableMap().keySet().contains(table)){
            log.warning(":NO_TABLE_EXIST");
            return false;
        }

        if (!getColumnMap(table).keySet().contains(column)){
            log.warning(":COLUMN_NOT_EXIST");
            return false;
        }

        if (!hasRecord(table, entry)){
            log.warning(":RECORD_NOT_EXIST");
            return false;
        }

        String entryType = HazeCommand.checkEntryType(entry);

        return prepare("UPDATE " + table + " SET " + column + " = ? WHERE " + entryType + " = ?")
                .map(statement -> {
                    try {
                        statement.setString(1, value);
                        statement.setString(2, entry);
                        statement.addBatch();

                        // Execute this batch
                        threadPool.submit(new StatementRunner(statement));
                        return true;
                    } catch (SQLException e) {
                        e.printStackTrace();
                        return false;
                    }
                }).orElse(false);
    }

    /**
     * {@code table} で指定したテーブルの列 {@code column} の値を取得する。
     * テーブル、カラム、レコードのいずれかが存在しない場合は対応するエラー文字列を返す。
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

        if (!getTableMap().keySet().contains(table))
            return ":NO_TABLE_EXIST";

        if (!getColumnMap(table).keySet().contains(column))
            return ":COLUMN_NOT_EXIST";

        if (!hasRecord(table, entry))
            return ":RECORD_NOT_EXIST";

        String entryType = HazeCommand.checkEntryType(entry);

        val statement = prepare("SELECT " + column + " FROM " + table + " WHERE " + entryType + " = ?");

        Optional<String> result = statement.map(stmt -> {
            try {
                stmt.setString(1, entry);
                ResultSet rs = stmt.executeQuery();
                return rs.getString(column);
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

        if (!getTableMap().keySet().contains(table)){
            log.warning(":NO_TABLE_EXIST");
            return false;
        }

        if (getColumnMap(table).keySet().contains(column)){
            log.warning(":COLUMN_EXIST");
            return false;
        }


        String whenTypeIsInteger = (type.equalsIgnoreCase("INTEGER")) ? " NOT NULL DEFAULT '0'" : "";
        val statement = prepare(
                "ALTER TABLE " + table + " ADD " + column + " " + type + whenTypeIsInteger);

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

        if (!getTableMap().keySet().contains(table)){
            log.warning(":NO_TABLE_EXIST");
            return false;
        }

        if (!getColumnMap(table).keySet().contains(column)){
            log.warning(":COLUMN_NOT_EXIST");
            return false;
        }

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
     * @return テーブルに含まれるcolumnの名前と型のマップ 失敗したら空のマップを返す。
     */
    public Map<String, String> getColumnMap(String table) {
        
        Map<String, String> columnMap = new HashMap<>();

        if (!getTableMap().keySet().contains(table)){
            log.warning(":NO_TABLE_EXIST");
            return columnMap;
        }

        val statement = prepare("SELECT * FROM " + table + " WHERE 0=1");

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
        
        if (!getTableMap().keySet().contains(table)){
            log.warning(":NO_TABLE_EXIST");
            return playersMap;
        }

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

        if (playersMap.isEmpty())
            log.warning(":MAP_IS_EMPTY");
        return playersMap;
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
    public boolean removeValue(@NonNull String table, String column, String entry) {

        if (!getTableMap().keySet().contains(table)){
            log.warning(":NO_TABLE_EXIST");
            return false;
        }

        if (!getColumnMap(table).keySet().contains(column)){
            log.warning(":COLUMN_NOT_EXIST");
            return false;
        }

        if (!hasRecord(table, entry)){
            log.warning(":RECORD_NOT_EXIST");
            return false;
        }

        String entryType = HazeCommand.checkEntryType(entry);

        val statement = prepare("UPDATE " + table + " SET " + column + " = NULL WHERE " + entryType + " = ?");

        return statement.map(stmt -> {
            try {
                stmt.setString(1, entry);
                stmt.executeUpdate();
                return true;
            } catch (SQLException exception) {
                exception.printStackTrace();
                return false;
            }
        }).orElse(false);
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
