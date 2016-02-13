package me.minotopia.commandrewriter;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class CommandRewriteCommand implements TabExecutor {

    private final CRPlugin plugin;
    private static final Pattern SPLIT_PATTERN = Pattern.compile("&*&", Pattern.LITERAL);
    private static final int LIST_PAGE_SIZE = 6;

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage(ChatColor.GOLD + ChatColor.BOLD.toString() + "CommandRewriter: Help");
            sender.sendMessage(ChatColor.GOLD + "/cr set <command>" + ChatColor.GRAY + " Start the rewrite assistent to the given command.");
            sender.sendMessage(ChatColor.GOLD + "/cr set <command> &*& <message>" + ChatColor.GRAY + " Rewrite the command with the given message.");
            sender.sendMessage(ChatColor.GOLD + "/cr list [pagenumber]" + ChatColor.GRAY + " List all set commands");
            sender.sendMessage(ChatColor.GOLD + "/cr remove <command>" + ChatColor.GRAY + " Unassign a text from a command.");
            sender.sendMessage(ChatColor.GOLD + "/cr reload" + ChatColor.GRAY + " Reload the config.");
            sender.sendMessage(ChatColor.GRAY + "You can use color codes like " + ChatColor.GOLD + "&6" + ChatColor.GRAY + " in the texts.");
            sender.sendMessage(ChatColor.GRAY + "The symbol " + ChatColor.GOLD + "|" + ChatColor.GRAY + " will be parsed as new line.");
            sender.sendMessage(ChatColor.GRAY + "Use " + ChatColor.GOLD + "{player}" + ChatColor.GRAY + " to insert the player's name.");
            sender.sendMessage(ChatColor.GRAY + "Use " + ChatColor.GOLD + "!r" + ChatColor.GRAY + " at the start of the command to input regex for matching.");
            sender.sendMessage(ChatColor.GRAY + "The given regex will be suffixed with " + ChatColor.GOLD + ".*" + ChatColor.GRAY + '.');
            return true;
        }
        if (args[0].equalsIgnoreCase("set")) {
            if (!sender.hasPermission("CommandRewriter.set")) {
                sender.sendMessage(ChatColor.RED + "You do not have the required permission!");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /cr set <command> [&*& <message>]");
                return true;
            }
            String combined = "";
            for (int i = 1; i < args.length; i++) {
                combined += args[i] + " ";
            }

            String combinedTrim = combined.trim();
            String[] split = SPLIT_PATTERN.split(combinedTrim);
            if (split.length == 1) {
                if ((sender instanceof Player)) {
                    Player plr = (Player) sender;
                    plugin.getCreators().put(plr.getUniqueId(), combinedTrim.toLowerCase());
                    sender.sendMessage(ChatColor.GREEN + "Now type the message that should be assigned to the command.");
                    sender.sendMessage(ChatColor.GREEN + "Type !abort to abort");
                } else {
                    sender.sendMessage(ChatColor.RED + "Wrong syntax for console. Usage: /cr set <command> [&*& <message>]");
                    return true;
                }
            } else if (split.length != 2) {
                sender.sendMessage(ChatColor.RED + "Wrong syntax. Usage: /cr set <command> [&*& <message>]");
                return true;
            } else {
                String command = split[0].trim();
                if (!Util.isRegex(command)) {
                    command = command.toLowerCase();
                }
                String message = split[1].trim();
                boolean overridden = plugin.getCommands().containsKey(command);

                plugin.setRewrite(command, message);
                if (plugin.isInvalidConfig()) {
                    sender.sendMessage(ChatColor.RED + "The current loaded commandrewriter configuration is invalid and therefore the change is only done in memory!");
                } else {
                    plugin.saveConfig();
                }

                if (sender instanceof Player) {
                    if (overridden) {
                        sender.sendMessage(ChatColor.RED + "The command '" + command + "' is already rewritten.");
                        sender.sendMessage(ChatColor.RED + "The value text will be overwritten with your one.");
                    }
                    sender.sendMessage(ChatColor.GREEN + "Successfully assigned the text to the command '" + command + "'.");
                } else {
                    if (overridden) {
                        sender.sendMessage(ChatColor.GREEN + "Successfully re-assigned the text to the command '" + command + "'.");
                    } else {
                        sender.sendMessage(ChatColor.GREEN + "Successfully assigned the text to the command '" + command + "'.");
                    }
                }
            }
        } else if (args[0].equalsIgnoreCase("list")) {
            if (!sender.hasPermission("CommandRewriter.list")) {
                sender.sendMessage(ChatColor.RED + "You do not have the required permission!");
                return true;
            }
            ArrayList<Map.Entry<String, String>> entries = new ArrayList<>(plugin.getCommands().entrySet());
            int start = 0;
            int pageNumber = 1;

            int lower = entries.size() / 6;
            double exact = ((double) entries.size()) / ((double) LIST_PAGE_SIZE);
            int pageCount = lower;
            if (lower < exact) { //last page partly filled
                pageCount++;
            }
            if (args.length > 1) {
                if (!StringUtils.isNumeric(args[1])) { // args[1] can't be empty, no extra check needed
                    sender.sendMessage(ChatColor.RED + "This is no valid number: §6" + args[1]);
                    return true;
                }
                try {
                    pageNumber = Integer.parseInt(args[1]);
                } catch (NumberFormatException ex) { //occurres at very high numbers
                    sender.sendMessage(ChatColor.RED + "This is no valid number: §6" + args[1]);
                    return true;
                }
                if (pageNumber == 0) { // no need of negative check as we already check for numeric literals only
                    sender.sendMessage(ChatColor.RED + "This is no valid number: §6" + args[1]);
                    return true;
                }
                start = LIST_PAGE_SIZE * (pageNumber - 1);

                if (start > entries.size()) {
                    sender.sendMessage(ChatColor.RED + "The page number is too high. Currently " + pageCount + " pages exist.");
                    return true;
                }
            }
            sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "CommandRewriter: list " + pageNumber + '/' + pageCount);
            int end = Math.min(start + LIST_PAGE_SIZE, entries.size());
            for (int i = start; i < end; i++) {
                Map.Entry<String, String> entry = entries.get(i);
                sender.sendMessage(ChatColor.GOLD + entry.getKey() + ChatColor.GRAY + ": " + ChatColor.RESET + Util.translateColorCodes(entry.getValue()));
            }
        } else if (args[0].equalsIgnoreCase("remove")) {
            if (!sender.hasPermission("CommandRewriter.remove")) {
                sender.sendMessage(ChatColor.RED + "You do not have the required permission!");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Use: /cr remove <command>");
                return true;
            }
            String com = "";
            for (int i = 1; i < args.length; i++) {
                com += args[i] + " ";
            }
            com = com.trim();
            if (!Util.isRegex(com)) {
                com = com.toLowerCase();
            }
            if (!plugin.getCommands().containsKey(com)) {
                sender.sendMessage(ChatColor.RED + "The command '" + args[1] + "' is not used in CommandRewriter!");
                return true;
            }
            removeCommand(com);
            if (plugin.isInvalidConfig()) {
                sender.sendMessage(ChatColor.RED + "The current loaded commandrewriter configuration is invalid and therefore the change is only done in memory!");
            } else {
                plugin.saveConfig();
            }
            sender.sendMessage(ChatColor.GREEN + "Successfully remove the command '" + com + "' from the CommandRewriter list.");
        } else if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("CommandRewriter.reload")) {
                sender.sendMessage(ChatColor.RED + "You do not have the required permission!");
                return true;
            }
            plugin.reload(sender);
            plugin.getLogger().info("has been reloaded.");
            sender.sendMessage(ChatColor.GREEN + "CommandRewriter has been successfully reloaded.");
        } else {
            sender.sendMessage(ChatColor.RED + "See /cr help for help.");
            return true;
        }
        return true;
    }

    private void removeCommand(String com) {
        plugin.getCommands().remove(com);
        plugin.getConfig().set(CRPlugin.COMMANDS_PATH + "." + com, null);
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        return null;
    }
}
