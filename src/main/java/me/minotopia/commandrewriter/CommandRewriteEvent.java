package me.minotopia.commandrewriter;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CommandRewriteEvent extends Event implements Cancellable {

    @NotNull
    private final PlayerCommandPreprocessEvent commandPreprocessEvent;
    @NotNull
    private final String fullIssuedCommand;
    @NotNull
    private final String rewriteTrigger;
    @NotNull
    private final List<String> messageToSend;
    private boolean cancelled;
    @NotNull
    private final CommandRewriteEvent.Unsafe unsafe = new CommandRewriteEvent.Unsafe();

    public CommandRewriteEvent(@NotNull PlayerCommandPreprocessEvent commandPreprocessEvent, @NotNull String fullIssuedCommand, @NotNull String rewriteTrigger, @NotNull List<String> messageToSend) {
        this.commandPreprocessEvent = commandPreprocessEvent;
        this.fullIssuedCommand = fullIssuedCommand;
        this.rewriteTrigger = rewriteTrigger;
        this.messageToSend = messageToSend;
    }

    /**
     * optain unsafe methods of this event
     */
    @NotNull
    public CommandRewriteEvent.Unsafe unsafe() {
        return unsafe;
    }

    @NotNull
    public Player getPlayer() {
        return commandPreprocessEvent.getPlayer();
    }

    /**
     * @return The full command issued
     */
    @NotNull
    public String getFullIssuedCommand() {
        return fullIssuedCommand;
    }

    /**
     * If this starts with {@code !r}, the trigger is parsed as regex, then it checks for match
     *
     * @return The part of the issued command which is defined in the command rewriter config and lead to rewrite the command
     * @see Util#isRegex(String)
     */
    @NotNull
    public String getRewriteTrigger() {
        return rewriteTrigger;
    }

    /**
     * <ul>
     *     <li>Will not be sent if the list is empty</li>
     *     <li>A line will not be sent if its empty</li>
     *     <li>{@linkplain ChatColor#translateAlternateColorCodes(char, String)} will be called for each line with the char '&'</li>
     * </ul>
     *
     * @return The message which will be sent (mutable, no copy)
     */
    @NotNull
    public List<String> getMessageToSend() {
        return messageToSend;
    }

    /**
     * @return whether command rewriting is cancelled
     * @see #setCancelled(boolean)
     */
    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Whether to cancel the command rewriting.<br>
     * <br>
     * Not cancelled will do this:
     * <ul>
     * <li>Cancel the {@link PlayerCommandPreprocessEvent}</li>
     * <li>Sending the message optainable via {@link #getMessageToSend()} to the player</li>
     * </ul>
     * Cancelled will do this:
     * <ul>
     * <li>Not changing the state of the {@link PlayerCommandPreprocessEvent}</li>
     * <li>Not sending any message to the player.</li>
     * </ul>
     *
     * @param cancelled whether to cancel command rewriting
     */
    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public class Unsafe {
        @NotNull
        public PlayerCommandPreprocessEvent getCommandPreprocessEvent() {
            return commandPreprocessEvent;
        }
    }

    //////////////////////// Needed for custom events ////////////////////////

    private static final HandlerList handlerList = new HandlerList();

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }
}
