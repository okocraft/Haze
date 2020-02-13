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

package net.okocraft.haze;

import lombok.Getter;

import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import net.okocraft.haze.command.HazeCommand;
import net.okocraft.haze.config.Config;
import net.okocraft.haze.listener.PlayerListener;

/**
 * @author OKOCRAFT
 */
public class Haze extends JavaPlugin {

    private static Haze instance;

    @Getter
    private PointManager pointManager;

    @Getter
    private Logger log = getLogger();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        // Implementation info
        log.info("We are using " + (Config.getInstance().isUsingMySQL() ? "MySQL" : "SQLite"));
        pointManager = new PointManager();
        
        PlayerListener.start();
        HazeCommand.init();

        // GO GO GO
        log.info("Haze has been enabled!");
    }

    @Override
    public void onDisable() {
        pointManager.getSQL().dispose();
        log.info("Haze has been disabled!");
    }

    public static Haze getInstance() {
        if (instance == null) {
            instance = (Haze) Bukkit.getPluginManager().getPlugin("Haze");
            if (instance == null) {
                throw new IllegalStateException("Haze is not enabled.");
            }
        }

        return instance;
    }
}
