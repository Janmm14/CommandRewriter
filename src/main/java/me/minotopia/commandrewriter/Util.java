package me.minotopia.commandrewriter;

import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

/**
 * Provides general utilities for the plugin
 */
public final class Util {

    private Util() {
        throw new UnsupportedOperationException();
    }

    /**
     * Translates the color code {@code &} to the minecraft color code {@code §}
     * @param inputMsg the message to convert
     * @return the converted message
     * @see ChatColor#translateAlternateColorCodes(char, String)
     */
    public static String translateColorCodes(@NotNull String inputMsg) {
        return ChatColor.translateAlternateColorCodes('&', inputMsg);
    }

    /**
     * Negates a predicate
     * @param toNegate the predicate to negate
     * @param <T> the argument of the {@link Predicate#test(Object)} method
     * @return the negated predicate
     * @see Predicate#negate()
     */
    @NotNull
    public static <T> Predicate<T> not(@NotNull Predicate<T> toNegate) {
        return toNegate.negate();
    }
}
