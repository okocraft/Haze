package net.okocraft.haze.bungee;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;

public class Haze extends Plugin {

    private static Haze instance;
    private BungeeCache cache;

    @Override
    public void onLoad() {
        instance = this;    
    }

    @Override
    public void onEnable() {
        getProxy().registerChannel("haze:tobukkit");
        getProxy().registerChannel("haze:tobungee");

        this.cache = new BungeeCache();
    }

    @Override
    public void onDisable() {
        cache.getPointsInstance().getSQL().dispose();

        getProxy().unregisterChannel("haze:tobukkit");
        getProxy().unregisterChannel("haze:tobungee");
    }

    public static Haze getInstance() {
        if (instance == null) {
            instance = (Haze) ProxyServer.getInstance().getPluginManager().getPlugin("Haze");
        }
        return instance;
    }

    public BungeeCache getCache() {
        return cache;
    }
}