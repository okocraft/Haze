package net.okocraft.haze.command;

import java.util.List;
import java.util.Map;

import org.bukkit.command.CommandSender;

public class PointListCommand extends BaseCommand {
    protected PointListCommand() {
        super(
                "haze.pointlist",
                1,
                true,
                true,
                "/haze pointlist"
        );
    }

    @Override
    public boolean runCommand(CommandSender sender, String[] args) {
        MESSAGES.sendMessage(sender, "command.pointlist.header");
        PLUGIN.getPointManager().getPoints().forEach(pointName -> {
            MESSAGES.sendMessage(sender, false, "command.pointlist.format", Map.of("%point-name%", pointName));
        });
        return true;
    }

    @Override
    public List<String> runTabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}