package ru.wishmine.ktrapleave.utils;

import net.md_5.bungee.api.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HexColorUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern LEGACY_HEX_PATTERN = Pattern.compile("&x(&[A-Fa-f0-9]){6}");
    private static final boolean SUPPORTS_HEX = isHexSupported();

    public static String translateHexColors(String message) {
        if (message == null) return null;

        if (SUPPORTS_HEX) {
            Matcher matcher = HEX_PATTERN.matcher(message);
            StringBuffer buffer = new StringBuffer();
            while (matcher.find()) {
                String hex = matcher.group(1);
                matcher.appendReplacement(buffer, ChatColor.of("#" + hex).toString());
            }
            message = matcher.appendTail(buffer).toString();

            matcher = LEGACY_HEX_PATTERN.matcher(message);
            buffer = new StringBuffer();
            while (matcher.find()) {
                String legacyHex = matcher.group();
                String hexCode = legacyHex.replace("&x", "")
                        .replace("&", "");
                matcher.appendReplacement(buffer, ChatColor.of("#" + hexCode).toString());
            }
            message = matcher.appendTail(buffer).toString();
        }

        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private static boolean isHexSupported() {
        try {
            ChatColor.of("#FFFFFF");
            return true;
        } catch (NoSuchMethodError | Exception e) {
            return false;
        }
    }
}