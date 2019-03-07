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

package net.okocraft.haze.command;

import lombok.NonNull;
import lombok.val;

import com.google.common.base.Strings;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import net.okocraft.haze.Haze;

public class HazeCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("haze.admin"))
            return true;

        if (args.length == 0)
            return false;

        @NonNull
        val subCommand = Strings.isNullOrEmpty(args[0]) ? "" : args[0];

        if (subCommand.equalsIgnoreCase("version")) {
             sender.sendMessage(Haze.getInstance().getVersion());

             return true;
        }

        return true;
    }
}
