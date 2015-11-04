package me.minotopia.commandrewriter;

import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class CommandRewriteCommand implements TabExecutor {

    private final CRPlugin plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "CommandRewriter: Help");
            sender.sendMessage(ChatColor.GOLD + "/cr set <command>" + ChatColor.GRAY + " Start the rewrite assistent to the given command.");
            sender.sendMessage(ChatColor.GOLD + "/cr list" + ChatColor.GRAY + " List all set commands");
            sender.sendMessage(ChatColor.GOLD + "/cr remove <command>" + ChatColor.GRAY + "Unassign a text from a command.");
            sender.sendMessage(ChatColor.GOLD + "/cr reload" + ChatColor.GRAY + "Reload the config.");
            sender.sendMessage(ChatColor.GRAY + "You can use color codes like " + ChatColor.GOLD + "&6" + ChatColor.GRAY + " in the texts.");
            sender.sendMessage(ChatColor.GRAY + "The symbol " + ChatColor.GOLD + "|" + ChatColor.GRAY + " will be parsed as new line.");
            return true;
        }
        if (args[0].equalsIgnoreCase("set")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Use this command in-game!");
                return true;
            }
            Player plr = (Player) sender;
            if (sender.hasPermission("CommandRewriter.set")) {
                if (args.length >= 2) {
                    String com = "";
                    for (int i = 1; i < args.length; i++) {
                        com += args[i] + " ";
                    }
                    plugin.getCreators().put(plr.getUniqueId(), com.trim().toLowerCase());
                    sender.sendMessage(ChatColor.GREEN + "Now type the message that should be assigned to the command.");
                    sender.sendMessage(ChatColor.GREEN + "Type !abort to abort");
                } else {
                    sender.sendMessage(ChatColor.RED + "Use: /cr set <command>");
                }
            } else {
                sender.sendMessage(ChatColor.RED + "You do not have the required permission!");
            }
        } else if (args[0].equalsIgnoreCase("list")) {
            if (sender.hasPermission("CommandRewriter.list")) {
                sender.sendMessage(ChatColor.GRAY + "The following messages are assigned:");
                for (Map.Entry<String, String> entry : plugin.getCommands().entrySet()) {
                    sender.sendMessage(ChatColor.GOLD + entry.getKey() + ChatColor.GRAY + ": " + ChatColor.RESET + Util.translateColorCodes(entry.getValue()));
                }
            } else {
                sender.sendMessage(ChatColor.RED + "You do not have the required permission!");
            }
        } else if (args[0].equalsIgnoreCase("remove")) {
            if (sender.hasPermission("CommandRewriter.remove")) {
                if (args.length >= 2) {
                    String com = "";
                    for (int i = 1; i < args.length; i++) {
                        com += args[i] + " ";
                    }
                    com = com.trim();
                    if (plugin.getCommands().containsKey(com.toLowerCase())) {
                        plugin.getCommands().remove(com.toLowerCase());
                        plugin.getConfig().set(CRPlugin.COMMANDS_PATH + "." + com, null);
                        plugin.saveConfig();
                        sender.sendMessage(ChatColor.GREEN + "Successfully remove the command '" + com + "' from the CommandRewriter list.");
                    } else {
                        sender.sendMessage(ChatColor.RED + "The command '" + args[1] + "' is not used in CommandRewriter!");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Use: /cr remove <command>");
                }
            } else {
                sender.sendMessage(ChatColor.RED + "You do not have the required permission!");
            }
        } else if (args[0].equalsIgnoreCase("reload")) {
            if (sender.hasPermission("CommandRewriter.reload")) {
                plugin.reload();
                plugin.getLogger().info("has been reloaded.");
                sender.sendMessage(ChatColor.GREEN + "CommandRewriter has been successfully reloaded.");
            } else {
                sender.sendMessage(ChatColor.RED + "You do not have the required permission!");
            }
        } else {
            sender.sendMessage(ChatColor.RED + "See /cr help for help.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        return null;
    }
}
