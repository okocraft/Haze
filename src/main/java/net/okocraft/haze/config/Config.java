package net.okocraft.haze.config;

import net.okocraft.configurationapi.BaseConfig;
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

    public String getMySQLHost() {
        return getConfig().getString("database.mysql.host", "localhost");
    }

    public String getMySQLUser() {
        return getConfig().getString("database.mysql.user", "root");
    }

    public String getMySQLPassword() {
        return getConfig().getString("database.mysql.pass", "");
    }

    public int getMySQLPort() {
        return getConfig().getInt("database.mysql.port", 3306);
    }

    public String getDatabaseName() {
        return getConfig().getString("database.mysql.db-name", "haze");
    }

    public void reloadAllConfigs() {
        Messages.getInstance().reload();
        reloadConfig();
    }

}