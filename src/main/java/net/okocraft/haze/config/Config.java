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



    public void reloadAllConfigs() {
        Messages.getInstance().reload();
        reloadConfig();
    }

}