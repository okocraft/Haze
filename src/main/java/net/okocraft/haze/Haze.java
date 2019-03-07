/*
 * This file is a part of Haze.
 *
 * Haze, Player's Point Manager.
 * Copyright (C) 2019 OKOCRAFT
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

import java.util.Optional;

import lombok.Getter;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import net.okocraft.haze.command.HazeCommand;

/**
 * @author OKOCRAFT
 */
public class Haze extends JavaPlugin {

    /**
     * Haze のバージョン。
     */
    @Getter
    private final String version;

    /**
     * プラグイン Haze のインスタンス。
     */
    private static Haze instance;

    public Haze() {
        this.version = Optional.ofNullable(getClass().getPackage().getImplementationVersion()).orElse("unknown");
    }

    @Override
    public void onEnable() {
        getCommand("haze").setExecutor(new HazeCommand());
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    public static Haze getInstance() {
        if (instance == null) {
            instance = (Haze) Bukkit.getPluginManager().getPlugin("Haze");
        }

        return instance;
    }
}
