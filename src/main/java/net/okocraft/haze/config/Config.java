package net.okocraft.haze.config;

import com.zaxxer.hikari.HikariConfig;

import net.okocraft.configurationapi.BaseConfig;
import net.okocraft.configurationapi.Configuration;
import net.okocraft.haze.Haze;

public class Config extends BaseConfig {

    private static Haze plugin = Haze.getInstance();
    private static Config instance = new Config();

    public Config() {
        super("config.yml", plugin.getDataFolder(), plugin.getResource("config.yml"));
        if (instance != null) {
            throw new ExceptionInInitializerError("Config is already initialized.");
        }
    }

    public static Config getInstance() {
        return instance;
    }

    public boolean isUsingMySQL() {
        return getConfig().getBoolean("database.use-mysql", false);
    }

    public HikariConfig getMySQLConfig() {
        if (!isUsingMySQL()) {
            throw new IllegalStateException("We do not use mysql.");
        }

        Configuration config = getConfig();
        HikariConfig hikariConfig = new HikariConfig();
        
        // login data
        hikariConfig.setDriverClassName("com.mysql.jdbc.Driver");
        hikariConfig.setJdbcUrl("jdbc:mysql://" + config.getString("mysql.host", "localhost") + ":" + getConfig().getInt("mysql.port") + "/haze?autoReconnect=true&useSSL=false");
        hikariConfig.addDataSourceProperty("user", config.getString("mysql.user", "root"));
        hikariConfig.addDataSourceProperty("password", config.getString("mysql.pass", ""));
        
        // general mysql settings
        // see https://github.com/brettwooldridge/HikariCP/wiki/MySQL-Configuration
        hikariConfig.addDataSourceProperty("cachePrepStmts", true);
        hikariConfig.addDataSourceProperty("prepStmtsCacheSize", 250);
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", 2048);
        hikariConfig.addDataSourceProperty("useServerPrepStmts", true);
        hikariConfig.addDataSourceProperty("useLocalSessionState", true);
        hikariConfig.addDataSourceProperty("rewriteBatchedStatements", true);
        hikariConfig.addDataSourceProperty("cacheResultSetMetadata", true);
        hikariConfig.addDataSourceProperty("cacheServerConfiguration", true);
        hikariConfig.addDataSourceProperty("elideSetAutoCommits", true);
        hikariConfig.addDataSourceProperty("maintainTimeStats", false);
        return hikariConfig;
    }

    public void reloadAllConfigs() {
        Messages.getInstance().reload();
        reloadConfig();
    }

}