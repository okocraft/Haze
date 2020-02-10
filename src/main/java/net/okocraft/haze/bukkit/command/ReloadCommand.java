package net.okocraft.haze.bukkit.command;

import java.util.List;

import org.bukkit.command.CommandSender;

import net.okocraft.haze.bukkit.config.Config;

public final class ReloadCommand extends BaseCommand {

    protected ReloadCommand() {
        super(
                "haze.reload",
                1,
                true,
                true,
                "/haze reload"
        );
    }

    @Override
    public boolean runCommand(CommandSender sender, String[] args) {
        Config.getInstance().reloadAllConfigs();
        MESSAGES.sendMessage(sender, "command.reload.success");
        return false;
    }

    @Override
    public List<String> runTabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
} 