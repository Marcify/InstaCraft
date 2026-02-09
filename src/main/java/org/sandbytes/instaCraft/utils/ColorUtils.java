package org.sandbytes.instaCraft.utils;

import org.bukkit.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtils {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    /**
     * Translates both legacy &-codes and hex color codes (&#RRGGBB) into Minecraft color.
     */
    public static String colorize(String message) {
        if (message == null) return "";

        // Translate hex colors first
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                replacement.append("§").append(c);
            }
            matcher.appendReplacement(buffer, replacement.toString());
        }
        matcher.appendTail(buffer);

        // Then translate legacy color codes
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }
}
