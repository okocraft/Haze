package net.okocraft.haze.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

import net.okocraft.haze.database.Database;

public class HazeTabCompleter implements TabCompleter {

    private Database database;

    HazeTabCompleter (Database database){
        this.database = database;
    }


    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {

        
        List<String> resultList = new ArrayList<>();
        if (!sender.hasPermission("haze.admin")) return resultList;

        if (args.length == 1) {
            List<String> commands = Arrays.asList("version", "write", "get", "set", "addcolumn", "dropcolumn",
                    "list", "give", "take");
            return StringUtil.copyPartialMatches(args[0], commands, resultList);
        }

        final String operation = args[0].toLowerCase();

        if (args.length == 2) {

            List<String> tableList = database.getTableMap().keySet().stream().collect(Collectors.toList());

            switch (operation) {

            case "write":
                return StringUtil.copyPartialMatches(args[1], tableList, resultList);

            case "get":
                return StringUtil.copyPartialMatches(args[1], tableList, resultList);

            case "set":
                return StringUtil.copyPartialMatches(args[1], tableList, resultList);

            case "addcolumn":
                return StringUtil.copyPartialMatches(args[1], tableList, resultList);

            case "dropcolumn":
                return StringUtil.copyPartialMatches(args[1], tableList, resultList);

            case "give":
                return StringUtil.copyPartialMatches(args[1], tableList, resultList);

            case "take":
                return StringUtil.copyPartialMatches(args[1], tableList, resultList);

            case "list":
                return StringUtil.copyPartialMatches(args[1], Arrays.asList("table", "column"), resultList);

            }
        }

        if (args.length == 3) {

            List<String> columnList = (database.getTableMap().keySet().contains(args[1])) ? database.getColumnMap(args[1]).keySet().stream().collect(Collectors.toList()) : new ArrayList<>();

            switch (operation) {

            case "get":
                return StringUtil.copyPartialMatches(args[2], columnList, resultList);

            case "set":
                return StringUtil.copyPartialMatches(args[2], columnList, resultList);

            case "addcolumn":
                return StringUtil.copyPartialMatches(args[2], Arrays.asList("<ColumnName>"), resultList);

            case "dropcolumn":
                return StringUtil.copyPartialMatches(args[2], columnList, resultList);

            case "give":
                return StringUtil.copyPartialMatches(args[2], columnList, resultList);

            case "take":
                return StringUtil.copyPartialMatches(args[2], columnList, resultList);

            case "list":
                if (args[1].equals("column"))
                    return StringUtil.copyPartialMatches(args[1], database.getTableMap().keySet().stream().collect(Collectors.toList()), resultList);
                return resultList;

            }
        }

        if (args.length == 4) {

            List<String> playerList = (database.getTableMap().keySet().contains(args[1])) ? database.getPlayersMap(args[1]).values().stream().collect(Collectors.toList()) : new ArrayList<>();

            switch (operation) {

            case "get":
                return StringUtil.copyPartialMatches(args[3], playerList, resultList);

            case "set":
                return StringUtil.copyPartialMatches(args[3], playerList, resultList);

            case "addcolumn":
                return StringUtil.copyPartialMatches(args[3], Arrays.asList("TEXT", "INTEGER", "NULL", "BROB", "REAL"), resultList);

            case "give":
                return StringUtil.copyPartialMatches(args[3], playerList, resultList);

            case "take":
                return StringUtil.copyPartialMatches(args[3], playerList, resultList);

            }
        }

        if (args.length == 5) {

            switch (operation) {

            case "set":
                return StringUtil.copyPartialMatches(args[3], Arrays.asList("<value>"), resultList);

            case "give":
                return StringUtil.copyPartialMatches(args[3], IntStream.range(1, 100).boxed().map(String::valueOf).collect(Collectors.toList()), resultList);

            case "take":
                return StringUtil.copyPartialMatches(args[3], IntStream.range(1, 100).boxed().map(String::valueOf).collect(Collectors.toList()), resultList);

            }
        }



        return resultList;
    }
}