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

package net.okocraft.haze.command;

import java.util.UUID;

import lombok.NonNull;
import lombok.val;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import net.okocraft.haze.Haze;
import net.okocraft.haze.database.Database;

public class HazeCommand implements CommandExecutor {
    private Database database;

    public HazeCommand(Database database) {
        this.database = database;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // insufficient permission
        if (!sender.hasPermission("haze.admin")) {
            sender.sendMessage(":PERM_INSUFFICIENT");
            return true;
        }

        // only /hz
        if (args.length == 0) {
            sender.sendMessage(":PARAM_INSUFFICIENT");
            return false;
        }

        @NonNull
        val subCommand = args[0];

        // hz version
        if (subCommand.equalsIgnoreCase("version")) {
             sender.sendMessage(Haze.getInstance().getVersion());

             return true;
        }

        // hz write
        if (subCommand.equalsIgnoreCase("write")) {
            // NOTE: For testing
            val uuid = UUID.randomUUID();
            val name = uuid.toString().substring(0, 6);

            database.addRecord(uuid, name);

            return true;
        }

        if (subCommand.equalsIgnoreCase("read")) {
            if (args.length <= 1) {
                sender.sendMessage(":PARAM_INSUFFICIENT");
                return false;
            }

            val uuid = args[1];

            sender.sendMessage(database.readRecord(uuid));

            return true;
        }

        sender.sendMessage(":PARAM_UNKNOWN");
        return true;
    }
}
