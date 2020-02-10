package net.okocraft.haze.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.command.CommandSender;
import org.bukkit.util.StringUtil;

public final class RemoveCommand extends BaseCommand {

    protected RemoveCommand() {
        super(
                "haze.remove",
                2,
                true,
                true,
                "/haze remove <point-name>"
        );
    }

    @Override
    public boolean runCommand(CommandSender sender, String[] args) {
        String pointName = args[1];
        if (!PLUGIN.getPointManager().getPoints().contains(pointName)) {
            MESSAGES.sendNoPointNameFound(sender, pointName);
            return false;
        }

        if (PLUGIN.getPointManager().remove(pointName)) {
            MESSAGES.sendMessage(sender, "command.remove.success", Map.of("%point-name%", pointName));
            return true;
        }
        
        MESSAGES.sendMessage(sender, "command.remove.failure", Map.of("%point-name%", pointName));
        return true;
    }

    @Override
    public List<String> runTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return StringUtil.copyPartialMatches(args[1], new ArrayList<>(PLUGIN.getPointManager().getPoints()), new ArrayList<>());
        }
        return List.of();
    }
} 