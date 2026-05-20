package com.openway.periplus.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Money {
    private static final Pattern RUPIAH = Pattern.compile("Rp\\s*([0-9.,]+)");

    private Money() {
    }

    public static long parseRupiah(String text) {
        Matcher matcher = RUPIAH.matcher(text);
        if (!matcher.find()) {
            throw new IllegalArgumentException("No Rupiah amount found in: " + text);
        }
        return Long.parseLong(matcher.group(1).replaceAll("[^0-9]", ""));
    }

    public static String formatRupiah(long value) {
        return "Rp " + String.format("%,d", value);
    }
}
