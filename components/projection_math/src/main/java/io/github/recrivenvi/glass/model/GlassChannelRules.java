package io.github.recrivenvi.glass.model;

import java.util.Locale;

public final class GlassChannelRules {
    public static final int MAX_NAME_LENGTH = 15;
    public static final String PUBLIC_OWNER = "public";

    private GlassChannelRules() {}

    public static String create(String playerName, String requestedName, boolean publicChannel) {
        String name = sanitizeName(requestedName);
        if (name.isEmpty()) {
            return "";
        }
        String owner = publicChannel ? PUBLIC_OWNER : sanitizeOwner(playerName);
        return owner.isEmpty() ? "" : owner + ":" + name;
    }

    public static String sanitizeName(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sanitized = new StringBuilder(MAX_NAME_LENGTH);
        value.codePoints()
                .filter(codePoint -> codePoint >= 0x20 && codePoint != 0x7f && codePoint != ':')
                .limit(MAX_NAME_LENGTH)
                .forEach(sanitized::appendCodePoint);
        return sanitized.toString().trim();
    }

    public static boolean isValid(String channel) {
        if (channel == null || channel.isEmpty()) {
            return false;
        }
        int separator = channel.indexOf(':');
        return separator > 0
                && separator == channel.lastIndexOf(':')
                && separator < channel.length() - 1
                && sanitizeName(channel.substring(separator + 1))
                        .equals(channel.substring(separator + 1));
    }

    public static boolean canAccess(String channel, String playerName) {
        if (!isValid(channel)) {
            return false;
        }
        String owner = channel.substring(0, channel.indexOf(':'));
        return owner.equalsIgnoreCase(PUBLIC_OWNER) || owner.equalsIgnoreCase(playerName);
    }

    public static String displayName(String channel) {
        int separator = channel == null ? -1 : channel.indexOf(':');
        return separator >= 0 && separator < channel.length() - 1
                ? channel.substring(separator + 1)
                : "";
    }

    public static boolean isPublic(String channel) {
        return channel != null && channel.toLowerCase(Locale.ROOT).startsWith(PUBLIC_OWNER + ":");
    }

    private static String sanitizeOwner(String value) {
        if (value == null) {
            return "";
        }
        return value.codePoints()
                .filter(codePoint -> codePoint >= 0x21 && codePoint != 0x7f && codePoint != ':')
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }
}
