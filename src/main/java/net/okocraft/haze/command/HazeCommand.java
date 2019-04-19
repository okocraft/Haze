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
        if (!hasPermission(sender, "haze.admin"))
            return false;

        // only /hz
        if (args.length == 0)
            return errorOccured(sender, ":PARAM_INSUFFICIENT");

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

            if (!(sender instanceof Player))
                return errorOccured(sender, ":INVALID_SENDER");

            if (args.length < 2)
                return errorOccured(sender, ":PARAM_INSUFFICIENT");

            val table = args[1];
            val uuid = ((Player) sender).getUniqueId();
            val name = ((Player) sender).getName();

            if (database.addRecord(table, uuid, name)){
                sender.sendMessage(":ADDED_PLAYER");
                return true;
            }
            sender.sendMessage(":WRITE_FAILURE");
            return false;
        }

        // hz get <table> <column> <uuid|mcid>
        if (subCommand.equalsIgnoreCase("get")) {
            if (args.length < 4)
                return errorOccured(sender, ":PARAM_INSUFFICIENT");

            val table = args[1];
            val column = args[2];
            val entry = args[3];

            sender.sendMessage(database.get(table, column, entry));

            return true;
        }

        // hz addcolumn <table> <column> [type]
        if (subCommand.equalsIgnoreCase("addcolumn")) {
            if (args.length < 3)
                return errorOccured(sender, ":PARAM_INSUFFICIENT");

            val table = args[1];
            val column = args[2];

            String type;

            if (args.length >= 4) {
                type = args[3];
            } else {
                type = "int";
            }

            if (database.addColumn(table, column, type)){
                sender.sendMessage(":ADD_COLUMN");
                return true;
            }

            sender.sendMessage(":ADD_COLUMN_FAILURE");
            return false;
        }

        // hz dropcolumn <table> <column>
        if (subCommand.equalsIgnoreCase("dropcolumn")) {
            if (args.length < 3)
                return errorOccured(sender, ":PARAM_INSUFFICIENT");

            val table = args[1];
            val column = args[2];

            if (args[2].equalsIgnoreCase("uuid") || args[2].equalsIgnoreCase("player"))
                return errorOccured(sender, ":UNREMOVABLE_COLUMN");

            if (!database.getTableMap().keySet().contains(table))
                return errorOccured(sender, ":NO_TABLE_EXIST");

            if (!database.getColumnMap(table).keySet().contains(column))
                return errorOccured(sender, ":COLUMN_NOT_EXIST");

            database.dropColumn(table, column);

            return true;
        }

        // hz list table
        // hz list column <table>
        if (subCommand.equalsIgnoreCase("list")) {
            if (args.length < 2)
                return errorOccured(sender, ":PARAM_INSUFFICIENT");

            if (args[1].equalsIgnoreCase("column")) {

                if (args.length < 3)
                    return errorOccured(sender, ":PARAM_INSUFFICIENT");

                val table = args[2];

                if (!database.getTableMap().keySet().contains(table))
                    return errorOccured(sender, ":NO_TABLE_EXIST");

                sender.sendMessage("列の名前 - 型");
                database.getColumnMap(table).forEach((colName, colType) -> {
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
            if (args.length < 5)
                return errorOccured(sender, ":PARAM_INSUFFICIENT");

            val table = args[1];
            val column = args[2];
            val player = args[3];
            val value = args[4];

            if (database.set(table, column, player, value)){
                sender.sendMessage(":SET_VALUE");
                return true;
            }

            sender.sendMessage(":SET_FAILURE");
            return false;
        }

        // hz give <table> <column> <player> <value>
        // hz take <table> <column> <player> <value>
        if (subCommand.equalsIgnoreCase("give") || subCommand.equalsIgnoreCase("give")) {
            if (args.length < 5)
                return errorOccured(sender, ":PARAM_INSUFFICIENT");

            val table = args[1];
            val column = args[2];
            val player = args[3];

            if (!database.getColumnMap(table).get(column).equals("INTEGER")) {
                Haze.getInstance().getLog().warning(":INVALID_COLUMN_TYPE");
                sender.sendMessage(":SET_FAILURE");
                return false;
            }

            int inputValue;
            int currentValue;

            try {
                inputValue = Integer.parseInt(args[4]);
                currentValue = Integer.parseInt(database.get(table, column, player));
            } catch (NumberFormatException exception) {
                return errorOccured(sender, ":INVALID_PARAM");
            }

            if (inputValue < 0) 
                return errorOccured(sender, ":INVALID_PARAM_INTEGER");

            String calcedValue = String.valueOf(currentValue);

            if (subCommand.equalsIgnoreCase("give"))
                calcedValue = String.valueOf(currentValue + inputValue);

            if (subCommand.equalsIgnoreCase("take"))
                calcedValue = String.valueOf(currentValue - inputValue);

            if (database.set(table, column, player, calcedValue)){
                sender.sendMessage(":SET_VALUE");
                return true;
            }
            sender.sendMessage(":SET_FAILURE");
            return false;
        }

        sender.sendMessage(":PARAM_UNKNOWN");
        return false;
    }

    public static String checkEntryType(String entry) {
        return entry.matches("([a-z]|\\d){8}-([a-z]|\\d){4}-([a-z]|\\d){4}-([a-z]|\\d){4}-([a-z]|\\d){12}") ? "uuid"
                : "player";
    }

    /**
     * 権限がないときにメッセージを送りつつfalseを返す。
     * 
     * @param sender
     * @param permission
     * @return 権限がないときにfalse あればtrue
     */
    public static boolean hasPermission(CommandSender sender, String permission) {
        if (!sender.hasPermission(permission))
            return errorOccured(sender, ":PERM_INSUFFICIENT_" + permission);
        return true;
    }

    /**
     * エラーが発生したときにメッセージを送りつつfalseを返す。
     * 
     * @param sender
     * @param errorMessage
     * @return false
     */
    public static boolean errorOccured(CommandSender sender, String errorMessage) {
        sender.sendMessage(errorMessage);
        return false;
    }
}
