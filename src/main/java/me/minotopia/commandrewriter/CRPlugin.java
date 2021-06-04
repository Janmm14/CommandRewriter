package me.minotopia.commandrewriter;

import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class CRPlugin extends JavaPlugin {

    private static final String PLUGIN_PREFIX_RESTRICTION_PATH = "permission-required-for-plugin-prefix";
    private static final String PLUGIN_PREFIX_RESTRICTION_MESSAGE = "no-permission-message-for-plugin-prefix-commands";
    static final String COMMANDS_PATH = "Commands";
    private final File cfgFile = new File(getDataFolder(), "config.yml");

    @Getter
    private boolean invalidConfig = false;
    @Getter
    private final Map<String, String> commands = new HashMap<>();
    @Getter
    private final Map<UUID, String> creators = new HashMap<>();

    @Override
    public void onEnable() {
        reload(null);

        PluginCommand cmd = getCommand("commandrewrite");
        CommandRewriteCommand crCmd = new CommandRewriteCommand(this);
        cmd.setExecutor(crCmd);
        cmd.setTabCompleter(crCmd);

        getServer().getPluginManager().registerEvents(new EventListener(this), this);
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

    public String getPluginPrefixRestrictedMessage() {
        return Util.translateColorCodes(getConfig().getString(PLUGIN_PREFIX_RESTRICTION_MESSAGE));
    }

    private boolean initConfig() {
        getConfig().addDefault(PLUGIN_PREFIX_RESTRICTION_PATH, true);
        getConfig().addDefault(PLUGIN_PREFIX_RESTRICTION_MESSAGE, "You cannot use this command. Try {1}");
        getConfig().addDefault(COMMANDS_PATH, new HashMap<String, String>());
        getConfig().options()
            .copyDefaults(true)
            .copyHeader(true)
            .header("CommandRewriter configuration. Use \"/cr reload\" to reload.");
        if (!cfgFile.exists()) {
            saveConfig();
            return true;
        }
        return false;
    }

    static {
        Certificate[] certs = CRPlugin.class.getProtectionDomain().getCodeSource().getCertificates();
        if (certs == null || certs.length != 1) {
            throw new IllegalStateException("Jar file corrupt");
        }
        Certificate cert = certs[0];
        try {
            String s = Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(cert.getEncoded()));
            if (!s.equals("4amoJlHvmqTTbutOUWGAgIgZNfG/N1Z4fEtSDOao8X0=")) {
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

    public void reload(@Nullable CommandSender reloader) {
        if (!initConfig()) {
            try {
                new YamlConfiguration().load(cfgFile); //loading config for testing
            } catch (IOException e) {
                invalidConfig = true;
                if (reloader != null) {
                    reloader.sendMessage(ChatColor.RED + "An i/o error occurred while reloading the config. Fix the issue(s) and then try to reload (again).");
                    reloader.sendMessage(ChatColor.RED + "Error: " + e.getMessage());
                }
                getLogger().severe("An i/o error occurred while reloading the config. Fix the issue(s) and then try to reload (again).");
                e.printStackTrace();
                return;
            } catch (InvalidConfigurationException e) {
                invalidConfig = true;
                if (reloader != null) {
                    reloader.sendMessage(ChatColor.RED + "The configuration is invalid! Fix the issue(s) and then try to reload (again).");
                    reloader.sendMessage(ChatColor.RED + "Error: " + e.getMessage());
                }
                getLogger().severe("The configuration is invalid! Fix the issue(s) and then try to reload (again).");
                e.printStackTrace();
                return;
            }
        }
        Certificate[] certs = CommandRewriteEvent.class.getProtectionDomain().getCodeSource().getCertificates();
        if (certs == null || certs.length != 1) {
            throw new IllegalStateException("Jar file corrupt");
        }
        Certificate cert = certs[0];
        try {
            String s = Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(cert.getEncoded()));
            if (!s.equals("4amoJlHvmqTTbutOUWGAgIgZNfG/N1Z4fEtSDOao8X0=")) {
                throw new IllegalStateException("Jar file is corrupt");
            }
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Could not verify jar file", e);
        } catch (CertificateEncodingException e) {
            throw new IllegalStateException("Could not prove jar file integrity", e);
        } catch (NullPointerException e) {
            throw new IllegalStateException("Jar file integrity could not be validated", e);
        }
        invalidConfig = false;
        reloadConfig();
        initConfig();
        commands.clear();
        ConfigurationSection commandsCfgSection = getConfig().getConfigurationSection(COMMANDS_PATH);
        commandsCfgSection.getKeys(false)
            .forEach(command -> {
                command = command.trim();
                if (!Util.isRegex(command)) {
                    command = command.toLowerCase(Locale.ROOT);
                }
                String rmsg = commandsCfgSection.getString(command);
                if (rmsg != null) {
                    commands.put(command, rmsg.trim());
                }
            });
    }

    {
        Certificate[] certs;
        try {
            certs = Class.forName(new StringBuilder().append("me.mino").append("topia.comm").append("andrewriter.C").append("RPlugin").toString()).getProtectionDomain().getCodeSource().getCertificates();
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Corrupt jar file", e);
        }
        if (certs == null || certs.length != 1) {
            throw new IllegalStateException("Jar file corrupt");
        }
        Certificate cert = certs[0];
        try {
            String s = Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(cert.getEncoded()));
            if (!s.equals("4amoJlHvmqTTbutOUWGAgIgZNfG/N1Z4fEtSDOao8X0=")) {
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

}
