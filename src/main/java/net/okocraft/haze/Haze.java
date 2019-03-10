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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import net.okocraft.haze.command.HazeCommand;
import net.okocraft.haze.db.HazeDB;

/**
 * @author OKOCRAFT
 */
public class Haze extends JavaPlugin {

    private static final Logger log = LoggerFactory.getLogger("Haze");

    /**
     * プラグイン Haze のインスタンス。
     */
    private static Haze instance;

    /**
     * Haze のバージョン。
     */
    @Getter
    private final String version;


    /**
     * データベース。
     */
    private final HazeDB database;

    public Haze() {
        version = getClass().getPackage().getImplementationVersion();
        database = new HazeDB();
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Implementation info
        log.info("Installed in : " + getDataFolder().getPath());
        log.info("Database file: " + database.getDBUrl());

        // Connect to database
        database.connect(getDataFolder().getPath() + "/data.db");

        // Register command /haze
        getCommand("haze").setExecutor(new HazeCommand(database));

        // GO GO GO
        log.info("Haze enabled!");
    }

    @Override
    public void onDisable() {
        database.dispose();

        log.info("Haze disabled!");
    }

    public static Haze getInstance() {
        if (instance == null) {
            instance = (Haze) Bukkit.getPluginManager().getPlugin("Haze");
        }

        return instance;
    }
}
