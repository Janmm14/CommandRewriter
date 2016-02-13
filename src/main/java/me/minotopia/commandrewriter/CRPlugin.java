package me.minotopia.commandrewriter;

import lombok.Getter;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;
import org.mcstats.Metrics;
import org.mcstats.Metrics.Graph;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CRPlugin extends JavaPlugin {

    private static final String PLUGIN_PREFIX_RESTRICTION_PATH = "permission-required-for-plugin-prefix";
    static final String COMMANDS_PATH = "Commands";
    private final File cfgFile = new File(getDataFolder(), "config.yml");

    @Getter
    private boolean invalidConfig = false;
    @Getter
    private Map<String, String> commands = new HashMap<>();
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    @Getter
    private Map<UUID, String> creators = new HashMap<>();
    private Metrics metrics;

    @Override
    public void onEnable() {
        reload(null);

        PluginCommand cmd = getCommand("commandrewrite");
        CommandRewriteCommand crCmd = new CommandRewriteCommand(this);
        cmd.setExecutor(crCmd);
        cmd.setTabCompleter(crCmd);

        getServer().getPluginManager().registerEvents(new EventListener(this), this);

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
        commands.clear();
        creators.clear();
    }

    public void setRewrite(String command, String message) {
        commands.put(command, message);
        getConfig().set(COMMANDS_PATH + "." + command, message);
    }

    public boolean isPluginPrefixUsageRestricted() {
        return getConfig().getBoolean(PLUGIN_PREFIX_RESTRICTION_PATH);
    }

    public void reload(@Nullable CommandSender reloader) {
        try {
            new YamlConfiguration().load(cfgFile); //loading config for testing
        } catch (IOException e) {
            invalidConfig = true;
            if (reloader != null) {
                reloader.sendMessage("§cAn i/o error occurred while reloading the config. Fix the issue(s) and then try to reload (again).");
                reloader.sendMessage("§cError: " + e.getMessage());
            }
            getLogger().severe("An i/o error occurred while reloading the config. Fix the issue(s) and then try to reload (again).");
            e.printStackTrace();
            return;
        } catch (InvalidConfigurationException e) {
            invalidConfig = true;
            if (reloader != null) {
                reloader.sendMessage("§cThe configuration is invalid! Fix the issue(s) and then try to reload (again).");
                reloader.sendMessage("§cError: " + e.getMessage());
            }
            getLogger().severe("The configuration is invalid! Fix the issue(s) and then try to reload (again).");
            e.printStackTrace();
            return;
        }
        invalidConfig = false;
        reloadConfig();
        getConfig().addDefault(PLUGIN_PREFIX_RESTRICTION_PATH, true);
        getConfig().addDefault(COMMANDS_PATH, new HashMap<String, String>());
        getConfig().options()
            .copyDefaults(true)
            .copyHeader(true)
            .header("CommandRewriter configuration. Use \"/cr reload\" to reload.\n" +
                "The permission node for the plugin prefix command usage is: CommandRewriter.pluginprefix");
        if (!cfgFile.exists()) {
            saveConfig();
        }
        commands.clear();
        ConfigurationSection commandsCfgSection = getConfig().getConfigurationSection(COMMANDS_PATH);
        commandsCfgSection.getKeys(false)
            .forEach(command -> {
                command = command.trim();
                if (!Util.isRegex(command)) {
                    command = command.toLowerCase();
                }
                String rmsg = commandsCfgSection.getString(command);
                if (rmsg != null) {
                    commands.put(command, rmsg.trim());
                }
            });
        updateMetrics();
    }

    private void updateMetrics() {
        //stop metrics if its running
        if (metrics != null) {
            try {
                stopMetrics();
            } catch (ReflectiveOperationException ex) {
                ex.printStackTrace();
                return;
            }
        }
        startMetrics();
    }

    private void stopMetrics() throws NoSuchFieldException, IllegalAccessException {
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
    }
}
