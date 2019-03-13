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
import org.bukkit.entity.Player;

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

            if(!(sender instanceof Player)) return true;
            // NOTE: For testing
            val uuid = ((Player) sender).getUniqueId();
            val name = ((Player) sender).getName();

            database.addRecord(uuid, name);
            sender.sendMessage(":ADDED_PLAYER");

            return true;
        }

        // hz read <uuid> <column>
        if (subCommand.equalsIgnoreCase("read")) {
            if (args.length < 3) {
                sender.sendMessage(":PARAM_INSUFFICIENT");
                return false;
            }

            val uuid = args[1];
            val column = args[2];

            sender.sendMessage(database.readRecord(uuid, column));

            return true;
        }

        // hz read <uuid> <column>
        if (subCommand.equalsIgnoreCase("read")) {
            if (args.length < 3) {
                sender.sendMessage(":PARAM_INSUFFICIENT");
                return false;
            }

            val uuid = args[1];
            val column = args[2];

            sender.sendMessage(database.readRecord(uuid, column));

            return true;
        }

        // hz add <column>
        if (subCommand.equalsIgnoreCase("add")) {
            if (args.length < 2) {
                sender.sendMessage(":PARAM_INSUFFICIENT");
                return false;
            }

            val column = args[1];

            database.addColumn("haze", column, "int");

            return true;
        }

        // hz drop <column>
        if (subCommand.equalsIgnoreCase("drop")) {
            if (args.length < 2) {
                sender.sendMessage(":PARAM_INSUFFICIENT");
                return false;
            }

            val column = args[1];

            database.dropColumn("haze", column);

            return true;
        }

        // hz list
        if (subCommand.equalsIgnoreCase("list")) {
            if (args.length < 1) {
                sender.sendMessage(":PARAM_INSUFFICIENT");
                return false;
            }

            sender.sendMessage("列の名前 - 型");
            database.getColumnMap("haze").forEach((colName, colType) -> {
                sender.sendMessage(colName +" - "+ colType);
            });

            return true;
        }

        sender.sendMessage(":PARAM_UNKNOWN");
        return true;
    }
}
