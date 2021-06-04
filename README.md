CommandRewriter [![Build Status](https://s.janmm14.de/commandrewriter-buildstatus)](https://s.janmm14.de/commandrewriter-ci)
===============

A plugin for the [Bukkit](https://www.spigotmc.org/) minecraft server.

About
-----

CommandRewriter is a small plugin that allows you to assign custom texts to custom commands. That means that you can assign a list of rules to the command /rules for example.

You can use colors and define as many lines as you want. If the Minecraft chat does not contain as many chars as you need, you can directly edit the config.yml in /plugins/CommandRewriter/. Just reload after the editing.

Features
--------

- Show the player a text if he executes a command
- Subcommand differentiation
- Colors
- Multiple lines
- In-Game configuration
- No complex configuration

Usage
-----

Assigning a text to a command:

- Use the command /cr set <command> to start the assistent
  - The command has to be without the leading /
  - The command can have arguments (/help 2 or /rules pvp)
- Now type the text you want to assign
  - You can use color definitions like &6
  - The symbol "|" marks a new line

Removing a text from a command:

- Type in /cr remove <command>

List all assigned texts:

- Use /cr list

Reload the config:

- Use /cr reload

Help:

- Use /cr help

Permissions
-----------

*commandrewriter.seecmd*
- Base permission required to see and use the command

*commandrewriter.set*
- Permission to assign messages to commands

*commandrewriter.remove*
- Permission to remove the assignment from a command

*commandrewriter.list*
- Permissions for listing all assigned texts

*commandrewriter.reload*
- Permissions for reloading the config

*commandrewriter.pluginprefix*
- If `permission-required-for-plugin-prefix` is enabled in config, this is the permission to still execute plugin-prefixed commands.
