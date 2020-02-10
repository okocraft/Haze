package net.okocraft.haze.bukkit.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.command.CommandSender;
import org.bukkit.util.StringUtil;

public final class AddCommand extends BaseCommand {

    protected AddCommand() {
        super(
                "haze.add",
                2,
                true,
                true,
                "/haze add <point-name>"
        );
    }

    @Override
    public boolean runCommand(CommandSender sender, String[] args) {
        if (PLUGIN.getCache().getPoints().contains(args[1])) {
            MESSAGES.sendMessage(sender, "command.add.point-already-exist");
            return false;
        }
        
        if (!PLUGIN.getCache().addPoint(args[1])) {
            MESSAGES.sendMessage(sender, "command.add.error-point-add");
            return false;
        }

        MESSAGES.sendMessage(sender, "command.add.success", Map.of("%point-name%", args[1]));
        return true;
    }

    @Override
    public List<String> runTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return StringUtil.copyPartialMatches(args[1], List.of("<point-name>"), new ArrayList<>());
        }
        return new ArrayList<>();
    }
} 