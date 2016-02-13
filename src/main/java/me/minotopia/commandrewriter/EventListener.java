package me.minotopia.commandrewriter;

import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static me.minotopia.commandrewriter.Util.not;

@RequiredArgsConstructor
public class EventListener implements Listener {
    private final CRPlugin plugin;

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        // remove leading slash, trm and lowercase
        String standardizedMessage = event.getMessage().trim().toLowerCase();
        if (standardizedMessage.startsWith("/")) {
            standardizedMessage = standardizedMessage.substring(1);
        }

        // check for direct matches
        String[] parts = standardizedMessage.split(" ");

        if (!Util.isRegex(standardizedMessage)) { // no "normal" match should be possible with regex pattern
            // remove argument for argument, starting with longest possible
            for (int i = parts.length; i > 0; i--) {
                String check = "";
                for (int w = 0; w < i; w++) {
                    check += parts[w] + " ";
                }
                check = check.trim();

                String configuredOutputRaw = plugin.getCommands().get(check);
                if (configuredOutputRaw != null) {
                    if (handleMatch(event, configuredOutputRaw, check)) {
                        return;
                    }
                    event.setCancelled(true);
                    return;
                }
            }
        }

        // check regex matching
        for (Map.Entry<String, String> entry : plugin.getCommands().entrySet()) {
            String configuredCommand = entry.getKey();
            if (!Util.isRegex(configuredCommand)) {
                continue;
            }
            String regex = configuredCommand.substring("!r".length()).trim() + ".*";

            Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(standardizedMessage);
            if (matcher.matches()) {
                if (handleMatch(event, entry.getValue(), "!r" + regex)) {
                    return;
                }
                event.setCancelled(true);
                return;
            }
        }
        // no regex match
        // forbid usage of /<pluginname>:<command> if user does not have appropiate permission
        if (parts.length != 0 && plugin.isPluginPrefixUsageRestricted()
            && !event.getPlayer().hasPermission("CommandRewriter.pluginprefix")
            && parts[0].contains(":")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("Dieser Befehl ist uns nicht bekannt. Probiere /" + parts[0].split(":")[1]);
        }
    }

    private boolean handleMatch(PlayerCommandPreprocessEvent event, String configuredOutputRaw, String matching) {
        //replace {player}
        String configuredOutput = configuredOutputRaw.replace("{player}", event.getPlayer().getName());
        //split into multiple lines at |
        List<String> configDefinedMessage = new ArrayList<>(Arrays.asList(configuredOutput.split("\\|")));
        //call the event
        CommandRewriteEvent evt = new CommandRewriteEvent(event, event.getMessage(), matching, configDefinedMessage);
        plugin.getServer().getPluginManager().callEvent(evt);
        //handle and use event output
        if (evt.isCancelled()) {
            return true;
        }
        configDefinedMessage = evt.getMessageToSend();
        if (!configDefinedMessage.isEmpty()) {
            configDefinedMessage.stream()
                .filter(not(String::isEmpty))
                .map(Util::translateColorCodes)
                .forEach(event.getPlayer()::sendMessage);
        }
        return false;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (plugin.getCreators().containsKey(uuid)) {
            if (event.getMessage().equalsIgnoreCase("!abort")) {
                player.sendMessage(ChatColor.RED + "You have aborted the CommandRewriter assistent.");
            } else {
                String command = plugin.getCreators().get(uuid);
                if (!Util.isRegex(command)) {
                    command = command.toLowerCase();
                }
                String message = event.getMessage();
                if (plugin.getCommands().containsKey(command)) {
                    player.sendMessage(ChatColor.RED + "The command '" + command + "' is already rewritten.");
                    player.sendMessage(ChatColor.RED + "The value text will be overwritten with your one.");
                }
                plugin.setRewrite(command, message);
                if (plugin.isInvalidConfig()) {
                    player.sendMessage(ChatColor.RED + "The current loaded commandrewriter configuration is invalid and therefore the change is only done in memory!");
                } else {
                    plugin.saveConfig();
                }

                player.sendMessage(ChatColor.GREEN + "Successfully assigned the text to the command '" + command + "'.");
            }
            plugin.getCreators().remove(uuid);
            event.setCancelled(true);
        }
    }

}
