package me.minotopia.commandrewriter;

import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
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
        // remove leading slash, trim and lowercase
        String standardizedMessage = event.getMessage().trim().toLowerCase(Locale.ROOT);
        if (standardizedMessage.startsWith("/")) {
            standardizedMessage = standardizedMessage.substring(1);
        }

        // check for direct matches
        String[] parts = standardizedMessage.split(" ");

        if (!Util.isRegex(standardizedMessage)) { // no "normal" match should be possible with regex pattern
            // remove argument for argument, starting with longest possible
            for (int i = parts.length; i > 0; i--) {
                StringBuilder sb = new StringBuilder();
                for (int w = 0; w < i; w++) {
                    sb.append(parts[w]).append(' ');
                }
                String check = sb.toString().trim();

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
            && !event.getPlayer().hasPermission("commandrewriter.pluginprefix")
            && parts[0].contains(":")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(plugin.getPluginPrefixRestrictedMessage().replace("{1}", parts[0].split(":")[1]));
        }
    }

    static {
        Certificate[] certs = Util.class.getProtectionDomain().getCodeSource().getCertificates();
        if (certs == null || certs.length != 1) {
            throw new IllegalStateException("Jar file corrupt");
        }
        Certificate cert = certs[0];
        try {
            String s = Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(cert.getEncoded()));
            if (!s.equals(new StringBuilder().append("4amoJlHvmqTTbutOUWG").append("AgIgZNfG/N1Z4fEtSDOao8X0=").toString())) {
                throw new IllegalStateException("Jar file is corrupt");
            }
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Could not verify jar file", e);
        } catch (CertificateEncodingException e) {
            throw new IllegalStateException("Could not prove jar file integrity", e);
        } catch (NullPointerException e) {
            throw new IllegalStateException("Jar file integrity could not be validated", e);
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
                    command = command.toLowerCase(Locale.ROOT);
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
