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
             Haze.getInstance().getVersion();

             return true;
        }

        return true;
    }
}
