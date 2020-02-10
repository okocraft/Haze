package net.okocraft.haze.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.util.StringUtil;

public final class GetCommand extends BaseCommand {

    protected GetCommand() {
        super(
                "haze.get",
                3,
                true,
                true,
                "/haze get <point-name> <player>"
        );
    }

    @Override
    public boolean runCommand(CommandSender sender, String[] args) {
        String pointName = args[1];
        if (!PLUGIN.getPointManager().getPoints().contains(pointName)) {
            MESSAGES.sendNoPointNameFound(sender, pointName);
            return false;
        }

        @SuppressWarnings("deprecation")
        OfflinePlayer player = Bukkit.getOfflinePlayer(args[2]);
        if (player.getName() == null) {
            MESSAGES.sendNoPlayerFound(sender, args[2]);
            return false;
        }

        long amount = PLUGIN.getPointManager().get(pointName, player.getUniqueId());
        MESSAGES.sendMessage(sender, "command.get.result", Map.of("%player%", player.getName(), "%point-name%", pointName, "%amount%", String.valueOf(amount)));

        return true;
    }

    @Override
    public List<String> runTabComplete(CommandSender sender, String[] args) {
        List<String> result = new ArrayList<>();
        List<String> points = new ArrayList<>(PLUGIN.getPointManager().getPoints());
        if (args.length == 2) {
            return StringUtil.copyPartialMatches(args[1], points, result);
        }

        if (!points.contains(args[1])) {
            return result;
        }

        List<String> players = new ArrayList<>(PLUGIN.getPointManager().getPlayers().values());

        if (args.length == 3) {
            return StringUtil.copyPartialMatches(args[2], players, result);
        }
        
        return result;
    }
} 