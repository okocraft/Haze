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

        // hz write <table>
        // tableにプレイヤーのレコードを追加する
        if (subCommand.equalsIgnoreCase("write")) {

            if (!(sender instanceof Player)) {
                sender.sendMessage(":INVALID_SENDER");
                return true;
            }

            if (args.length < 2) {
                sender.sendMessage(":PARAM_INSUFFICIENT");
                return false;
            }

            val table = args[1];
            val uuid = ((Player) sender).getUniqueId();
            val name = ((Player) sender).getName();

            database.addRecord(table, uuid, name);
            sender.sendMessage(":ADDED_PLAYER");

            return true;
        }

        // hz get <table> <uuid> <column>
        if (subCommand.equalsIgnoreCase("read")) {
            if (args.length < 4) {
                sender.sendMessage(":PARAM_INSUFFICIENT");
                return false;
            }

            val table = args[1];
            val column = args[2];
            val entry = args[3];

            sender.sendMessage(database.get(table, column, entry));

            return true;
        }

        // hz addcolumn <table> <column> [type]
        if (subCommand.equalsIgnoreCase("addcolumn")) {
            if (args.length < 3) {
                sender.sendMessage(":PARAM_INSUFFICIENT");
                return false;
            }

            val table = args[1];
            val column = args[2];

            String type;

            if (args.length >= 4) {
                type = args[3];
            } else {
                type = "int";
            }

            database.addColumn(table, column, type);

            return true;
        }

        // hz dropcolumn <table> <column>
        if (subCommand.equalsIgnoreCase("dropcolumn")) {
            if (args.length < 3) {
                sender.sendMessage(":PARAM_INSUFFICIENT");
                return false;
            }
            if (args[2].equalsIgnoreCase("uuid") || args[2].equalsIgnoreCase("player")) {
                sender.sendMessage(":UNREMOVABLE_COLUMN");
                return false;
            }

            val table = args[1];
            val column = args[2];

            if (database.getColumnMap(table).keySet().contains(column)) {
                sender.sendMessage(":NO_COLUMN_FOUND");
                return false;
            }

            database.dropColumn(table, column);

            return true;
        }

        // hz list table
        // hz list column <table>
        if (subCommand.equalsIgnoreCase("list")) {
            if (args.length < 2) {
                sender.sendMessage(":PARAM_INSUFFICIENT");
                return false;
            }

            if (args[1].equalsIgnoreCase("column")) {

                if (args.length < 3) {
                    sender.sendMessage(":PARAM_INSUFFICIENT");
                    return false;
                }

                sender.sendMessage("列の名前 - 型");
                database.getColumnMap(args[2]).forEach((colName, colType) -> {
                    sender.sendMessage(colName + " - " + colType);
                });
            } else if (args[1].equalsIgnoreCase("table")) {
                sender.sendMessage("テーブルの名前 - 型");

                database.getTableMap().forEach((tableName, tableType) -> {
                    sender.sendMessage(tableName + " - " + tableType);
                });
            }

            return true;
        }

        // hz set <table> <column> <player> <value>
        if (subCommand.equalsIgnoreCase("set")) {
            if (args.length < 5) {
                sender.sendMessage(":PARAM_INSUFFICIENT");
                return false;
            }

            val table = args[1];
            val column = args[2];
            val player = args[3];
            val value = args[4];

            database.set(table, column, player, value);

            return true;
        }

        // hz give <table> <column> <player> <value>
        // hz take <table> <column> <player> <value>
        if (subCommand.equalsIgnoreCase("give") || subCommand.equalsIgnoreCase("give")) {
            if (args.length < 5) {
                sender.sendMessage(":PARAM_INSUFFICIENT");
                return false;
            }

            val table = args[1];
            val column = args[2];
            val player = args[3];

            int inputValue;
            int currentValue;

            try {
                inputValue = Integer.parseInt(args[4]);
                currentValue = Integer.parseInt(database.get(table, column, player));
            } catch (NumberFormatException exception) {
                exception.printStackTrace();
                return false;
            }

            if (inputValue < 0) {
                sender.sendMessage(":TOO_SMALL_NUMBER");
                return false;
            }

            String calcedValue = String.valueOf(currentValue);

            if (subCommand.equalsIgnoreCase("give"))
                calcedValue = String.valueOf(currentValue + inputValue);

            if (subCommand.equalsIgnoreCase("take"))
                calcedValue = String.valueOf(currentValue - inputValue);

            database.set(table, column, player, calcedValue);

            return true;
        }

        sender.sendMessage(":PARAM_UNKNOWN");
        return true;
    }

    public static String checkEntryType(String entry) {
        return entry.matches("([a-z]|\\d){8}-([a-z]|\\d){4}-([a-z]|\\d){4}-([a-z]|\\d){4}-([a-z]|\\d){12}") ? "uuid"
                : "player";
    }
}
