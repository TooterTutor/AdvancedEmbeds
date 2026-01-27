package io.github.tootertutor.embedbot;

import java.awt.Color;

public final class Util {
    private static final int HEX_RADIX = 16;
    private static final String HEX_PATTERN = "(?i)^[0-9a-f]{6}$";
    private static final String ELLIPSIS = "…";

    private Util() {
    }

    public static Color parseColor(String input) {
        if (input == null) {
            return null;
        }
        String s = input.trim();
        if (s.isEmpty()) {
            return null;
        }
        if (s.startsWith("#")) {
            s = s.substring(1);
        }
        if (s.matches(HEX_PATTERN)) {
            return new Color(Integer.parseInt(s, HEX_RADIX));
        }
        return null;
    }

    public static String nullIfBlank(String s) {
        if (s == null) {
            return null;
        }
        s = s.trim();
        return s.isEmpty() ? null : s;
    }

    public static String safe(String s) {
        return s == null ? "" : s;
    }

    public static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max - 1) + ELLIPSIS;
    }

    public static String boolToOnOff(boolean b) {
        return b ? "On" : "Off";
    }
}
