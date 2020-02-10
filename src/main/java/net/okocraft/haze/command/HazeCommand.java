package net.okocraft.haze.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

public class HazeCommand extends BaseCommand implements CommandExecutor, TabCompleter {

    private static final HazeCommand INSTANCE = new HazeCommand();

    protected HazeCommand() {
        super("", 0, true, true, "/haze <args...>");
    }

    private enum SubCommands {
        INCREASE(new IncreaseCommand()), DECREASE(new DecreaseCommand()), POINT_LIST(new PointListCommand()),
        SET(new SetCommand()), ADD(new AddCommand()), REMOVE(new RemoveCommand()), RELOAD(new ReloadCommand()),
        GET(new GetCommand());

        private final BaseCommand subCommand;

        private SubCommands(BaseCommand subCommand) {
            this.subCommand = subCommand;
        }

        public BaseCommand get() {
            return subCommand;
        }

        public static BaseCommand getByName(String name) {
            for (SubCommands subCommand : values()) {
                if (subCommand.get().getName().equalsIgnoreCase(name)) {
                    return subCommand.get();
                }
            }

            throw new IllegalArgumentException("There is no command with the name " + name);
        }

        public static List<String> getPermittedCommandNames(CommandSender sender) {
            List<String> result = new ArrayList<>();
            for (SubCommands subCommand : values()) {
                if (subCommand.get().hasPermission(sender)) {
                    result.add(subCommand.get().getName().toLowerCase(Locale.ROOT));
                }
            }
            return result;
        }
    }

    public static HazeCommand getInstance() {
        return INSTANCE;
    }

    public void init() {
        PluginCommand pluginCommand = PLUGIN.getCommand("haze");
        if (pluginCommand == null) {
            PLUGIN.getLogger().severe("Command \"/haze\" is not written in plugin.yml");
            Bukkit.getPluginManager().disablePlugin(PLUGIN);
        }

        pluginCommand.setExecutor(this);
        pluginCommand.setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            return this.runCommand(sender, args);
        }

        BaseCommand subCommand;
        try {
            subCommand = SubCommands.getByName(args[0]);
        } catch (IllegalArgumentException e) {
            MESSAGES.sendInvalidArgument(sender, args[0]);
            this.runCommand(sender, args);
            return false;
        }

        if (!subCommand.isConsoleCommand()
                && (sender instanceof ConsoleCommandSender || sender instanceof RemoteConsoleCommandSender)) {
            MESSAGES.sendConsoleSenderCannotUse(sender);
            return false;
        }

        if (!subCommand.isPlayerCommand() && sender instanceof Player) {
            MESSAGES.sendPlayerCannotUse(sender);
            return false;
        }

        if (!subCommand.hasPermission(sender)) {
            MESSAGES.sendNoPermission(sender, subCommand.getPermissionNode());
            return false;
        }

        if (!subCommand.isValidArgsLength(args.length)) {
            MESSAGES.sendNotEnoughArguments(sender);
            MESSAGES.sendUsage(sender, subCommand.getUsage());
            return false;
        }

        return subCommand.runCommand(sender, args);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> permittedCommands = SubCommands.getPermittedCommandNames(sender);
        permittedCommands.add("help");
        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], permittedCommands, new ArrayList<>());
        }

        if (!permittedCommands.contains(args[0].toLowerCase(Locale.ROOT))) {
            return List.of();
        }

        return SubCommands.getByName(args[0]).runTabComplete(sender, args);
    }

    /**
     * Show help.
     * 
     * @param sender
     * @param args
     * 
     * @return true
     */
    @Override
    public boolean runCommand(CommandSender sender, String[] args) {
        MESSAGES.sendMessage(sender, "command.help.header");
        MESSAGES.sendMessage(sender, false, "command.help.format",
                Map.of("%usage%", getUsage(), "%description%", MESSAGES.getMessage("command.help.description")));
        for (SubCommands subCommand : SubCommands.values()) {
            MESSAGES.sendMessage(sender, false, "command.help.format",
                    Map.of("%usage%", subCommand.get().getUsage(), "%description%", subCommand.get().getDescription()));
        }
        return true;
    }

    @Override
    public List<String> runTabComplete(CommandSender sender, String[] args) {
        // Not used.
        return null;
    }

    @Override
    public String getName() {
        return "haze";
    }
}