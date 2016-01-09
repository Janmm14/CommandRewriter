package me.minotopia.commandrewriter;

import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.mcstats.Metrics;
import org.mcstats.Metrics.Graph;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static me.minotopia.commandrewriter.Util.not;

public class CRPlugin extends JavaPlugin implements Listener {

    static final String COMMANDS_PATH = "Commands";

    @Getter
    private Map<String, String> commands = new HashMap<>();
    @Getter
    private Map<UUID, String> creators = new HashMap<>();
    private Metrics metrics;

    @Override
    public void onEnable() {
        reload();

        PluginCommand cmd = getCommand("commandrewrite");
        CommandRewriteCommand crCmd = new CommandRewriteCommand(this);
        cmd.setExecutor(crCmd);
        cmd.setTabCompleter(crCmd);

        getServer().getPluginManager().registerEvents(this, this);

        startMetrics();
    }

    private void startMetrics() {
        try {
            metrics = new Metrics(this);
            Graph graphabbr = metrics.createGraph("Defined texts");
            graphabbr.addPlotter(new Metrics.Plotter(Integer.toString(commands.size())) {
                @Override
                public int getValue() {
                    return 1;
                }
            });
            metrics.start();
        } catch (IOException ex) {
            getLogger().warning("Error while enabling Metrics: " + ex);
            ex.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        saveConfig();
        commands.clear();
        creators.clear();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (creators.containsKey(uuid)) {
            if (event.getMessage().equalsIgnoreCase("!abort")) {
                player.sendMessage(ChatColor.RED + "You have aborted the CommandRewriter assistent.");
            } else {
                String command = creators.get(uuid).toLowerCase();
                String message = event.getMessage();
                if (commands.containsKey(command)) {
                    player.sendMessage(ChatColor.RED + "The command '" + command + "' is already rewritten.");
                    player.sendMessage(ChatColor.RED + "The value text will be overwritten with your one.");
                }
                setRewrite(command, message);
                player.sendMessage(ChatColor.GREEN + "Successfully assigned the text to the command '" + command + "'.");
            }
            creators.remove(uuid);
            event.setCancelled(true);
        }
    }

    public void setRewrite(String command, String message) {
        commands.put(command, message);
        getConfig().set(COMMANDS_PATH + "." + command, message);
        saveConfig();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String standardizedMessage = event.getMessage().trim().toLowerCase();
        if (standardizedMessage.startsWith("/")) {
            standardizedMessage = standardizedMessage.substring(1);
        }
        String[] parts = standardizedMessage.split(" ");
        String matching = null;
        //remove argument for argument, starting with longest possible
        for (int i = parts.length; i > 0; i--) {
            String check = "";
            for (int w = 0; w < i; w++) {
                check += parts[w] + " ";
            }
            check = check.trim();
            if (commands.containsKey(check)) {
                matching = check;
                break;
            }
        }
        if (matching != null) {
            String configuredOutput = commands.get(matching);
            //replace {player}
            configuredOutput = configuredOutput.replace("{player}", event.getPlayer().getName());
            //split into multiple lines at |
            List<String> configDefinedMessage = new ArrayList<>(Arrays.asList(configuredOutput.split("\\|")));
            //call the event
            CommandRewriteEvent evt = new CommandRewriteEvent(event, event.getMessage(), matching, configDefinedMessage);
            getServer().getPluginManager().callEvent(evt);
            //handle and use event output
            if (evt.isCancelled()) {
                return;
            }
            configDefinedMessage = evt.getMessageToSend();
            if (!configDefinedMessage.isEmpty()) {
                configDefinedMessage.stream()
                    .filter(not(String::isEmpty))
                    .map(Util::translateColorCodes)
                    .forEach(event.getPlayer()::sendMessage);
            }
            event.setCancelled(true);
        }
    }

    public void reload() {
        reloadConfig();
        getConfig().addDefault(COMMANDS_PATH, new HashMap<String, String>());
        getConfig().options().copyDefaults(true);
        saveConfig();
        commands.clear();
        reloadConfig();
        ConfigurationSection commandsCfgSection = getConfig().getConfigurationSection(COMMANDS_PATH);
        commandsCfgSection.getKeys(false)
            .forEach(command -> commands.put(command.trim().toLowerCase(), commandsCfgSection.getString(command)));
        updateMetrics();
    }

    private void updateMetrics() {
        if (metrics != null) {
            try {
                Field taskField = Metrics.class.getDeclaredField("task");
                boolean accessible = taskField.isAccessible();
                taskField.setAccessible(true);
                BukkitTask task = (BukkitTask) taskField.get(metrics);
                taskField.setAccessible(accessible);

                if (task != null) {
                    if (getServer().getScheduler().isCurrentlyRunning(task.getTaskId()) || getServer().getScheduler().isQueued(task.getTaskId())) {
                        task.cancel();
                    }
                }
            } catch (ReflectiveOperationException ex) {
                ex.printStackTrace();
            }
        }
        startMetrics();
    }
}
