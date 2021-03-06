package net.okocraft.haze.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.util.StringUtil;

public final class SetCommand extends BaseCommand {

    protected SetCommand() {
        super(
                "haze.set",
                4,
                true,
                true,
                "/haze set <point-name> <player> <number>"
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

        long amount;
        try {
            amount = Long.parseLong(args[3]);
        } catch (NumberFormatException e) {
            MESSAGES.sendInvalidNumber(sender, args[3]);
            return false;
        }
        
        if (PLUGIN.getPointManager().set(pointName, player.getUniqueId(), amount)) {
            MESSAGES.sendMessage(sender, "command.set.success", Map.of("%player%", player.getName(), "%point-name%", pointName, "%amount%", String.valueOf(amount)));
            return true;
        }
        
        MESSAGES.sendNoPointNameFound(sender, pointName);
        return false;
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

        if (!players.contains(args[2])) {
            return result;
        }

        if (args.length == 4) {
            return StringUtil.copyPartialMatches(args[3], List.of("1", "10", "100"), result);
        }

        return result;
    }
}