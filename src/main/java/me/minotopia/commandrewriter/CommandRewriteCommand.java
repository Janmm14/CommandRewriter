package me.minotopia.commandrewriter;

import com.google.common.collect.ImmutableList;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
            sender.sendMessage(ChatColor.GOLD + "/cr list {searchword] [pagenumber]" + ChatColor.GRAY + " List set commands, optionally with a searchword");
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
                sender.sendMessage("§cYou do not have the required permission!");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage("§cUsage: /cr set <command> [&*& <message>]");
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
                    sender.sendMessage("§cWrong syntax for console. Console can only use: /cr set <command> &*& <message>");
                    return true;
                }
            } else if (split.length != 2) {
                sender.sendMessage("§cWrong syntax. Usage: /cr set <command> [&*& <message>]");
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
                    sender.sendMessage("§cThe current loaded commandrewriter configuration is invalid and therefore the change is only done in memory!");
                } else {
                    plugin.saveConfig();
                }

                if (sender instanceof Player) {
                    if (overridden) {
                        sender.sendMessage("§cThe command '" + command + "' is already rewritten.");
                        sender.sendMessage("§cThe value text will be overwritten with your one.");
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
                sender.sendMessage("§cYou do not have the required permission!");
                return true;
            }
            List<Map.Entry<String, String>> entries = new ArrayList<>(plugin.getCommands().entrySet());
            if (entries.isEmpty()) {
                sender.sendMessage("§cNo command rewrites configured.");
                return true;
            }

            int pageNumber = 1;

            boolean hasNumber = true;
            if (args.length > 1) {
                int pageNumberArgPos;
                if (args.length > 2) {
                    pageNumberArgPos = 2;
                } else {
                    pageNumberArgPos = 1;
                }

                String pageNumberString = args[pageNumberArgPos];

                if (!StringUtils.isNumeric(pageNumberString)) { //pageNumberString can't be empty, no extra check needed
                    hasNumber = false;
                } else {
                    try {
                        pageNumber = Integer.parseInt(pageNumberString);
                    } catch (NumberFormatException ex) { //occurres at very high numbers
                        sender.sendMessage("§cThis is no valid number: §6" + pageNumberString);
                        return true;
                    }
                    if (pageNumber == 0) { // no need of negative check as we already check for numeric literals only
                        sender.sendMessage("§cThis is no valid number: §6" + pageNumberString);
                        return true;
                    }


                }
            }

            //sort entries by keys alphabetical
            Collections.sort(entries, (o1, o2) -> o1.getKey().compareTo(o2.getKey()));

            String searchString = null;
            if (!hasNumber || args.length > 2) {
                searchString = args[1].toLowerCase().trim();

                String searchStringFinal = searchString;

                //check key match
                List<Map.Entry<String, String>> filteredEntries = entries.stream()
                    .filter(entry -> entry.getKey().trim().toLowerCase().contains(searchStringFinal))
                    .collect(Collectors.toList());
                Collections.sort(entries, (o1, o2) -> {
                    String o1key = o1.getKey().trim().toLowerCase();
                    String o2key = o2.getKey().trim().toLowerCase();
                    return o1key.compareTo(o2key);
                });

                //sort exact key match to the top
                ArrayList<Map.Entry<String, String>> filteredEntryCopy = new ArrayList<>(filteredEntries);
                for (Map.Entry<String, String> entry : filteredEntryCopy) {
                    if (entry.getKey().toLowerCase().trim().equals(searchStringFinal)) {
                        filteredEntries.remove(entry);
                        filteredEntries.add(0, entry);
                        break;
                    }
                }

                //add value matches afterwards
                entries.removeAll(filteredEntries);
                entries.stream()
                    .filter(entry -> {
                        String output = entry.getValue().trim().toLowerCase();
                        String translatedOutput = ChatColor.translateAlternateColorCodes('&', output).trim();
                        String strippedOutput = ChatColor.stripColor(translatedOutput).trim();
                        return output.contains(searchStringFinal)
                            || translatedOutput.contains(searchStringFinal)
                            || strippedOutput.contains(searchStringFinal);
                    })
                    .forEach(filteredEntries::add);

                entries = filteredEntries;
            }

            int lowerPageCount = entries.size() / LIST_PAGE_SIZE;
            double exactDivision = ((double) entries.size()) / ((double) LIST_PAGE_SIZE);
            int pageCount = lowerPageCount;
            if (lowerPageCount < exactDivision) { //last page partly filled
                pageCount++;
            }

            if (pageNumber > pageCount) {
                sender.sendMessage("§cThe page number is too high. Currently only " + pageCount + " pages exist.");
                return true;
            }

            int start = LIST_PAGE_SIZE * (pageNumber - 1);

            if (searchString != null) {
                sender.sendMessage("§cCommandRewriter: list search: §6" + searchString + " §c" + pageNumber + '/' + pageCount);
            } else {
                sender.sendMessage("§cCommandRewriter: list " + pageNumber + '/' + pageCount);
            }
            sendEntries(sender, entries, start);
        } else if (args[0].equalsIgnoreCase("remove")) {
            if (!sender.hasPermission("CommandRewriter.remove")) {
                sender.sendMessage("§cYou do not have the required permission!");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage("§cUse: /cr remove <command>");
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
                sender.sendMessage("§cThe command '" + args[1] + "' is not used in CommandRewriter!");
                return true;
            }
            removeCommand(com);
            if (plugin.isInvalidConfig()) {
                sender.sendMessage("§cThe current loaded commandrewriter configuration is invalid and therefore the change is only done in memory!");
            } else {
                plugin.saveConfig();
            }
            sender.sendMessage(ChatColor.GREEN + "Successfully remove the command '" + com + "' from the CommandRewriter list.");
        } else if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("CommandRewriter.reload")) {
                sender.sendMessage("§cYou do not have the required permission!");
                return true;
            }
            plugin.reload(sender);
            plugin.getLogger().info("has been reloaded.");
            sender.sendMessage(ChatColor.GREEN + "CommandRewriter has been successfully reloaded.");
        } else {
            sender.sendMessage("§cSubcommand not found. See /cr help for help.");
            return true;
        }
        return true;
    }

    private void sendEntries(CommandSender sender, List<Map.Entry<String, String>> entries, int start) {
        int end = Math.min(start + LIST_PAGE_SIZE, entries.size());
        for (int i = start; i < end; i++) {
            Map.Entry<String, String> entry = entries.get(i);
            sender.sendMessage(ChatColor.GOLD + entry.getKey() + ChatColor.GRAY + ": " + ChatColor.RESET + Util.translateColorCodes(entry.getValue()));
        }
    }

    private void removeCommand(String com) {
        plugin.getCommands().remove(com);
        plugin.getConfig().set(CRPlugin.COMMANDS_PATH + "." + com, null);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("CommandRewriter.seecmd")) {
            return ImmutableList.of();
        }
        if (args.length == 1) {
            Arrays.asList("list", "reload", "remove", "set");
        }
        return null;
    }
}
