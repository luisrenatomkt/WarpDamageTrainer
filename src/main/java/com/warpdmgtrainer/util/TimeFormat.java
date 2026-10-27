package com.warpdmgtrainer.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class TimeFormat {
    private TimeFormat() {}

    /** mm:ss,mmm */
    public static String duration(long ms) {
        if (ms < 0) ms = 0;
        long totalSec = ms / 1000L;
        return String.format(Locale.ROOT, "%02d:%02d,%03d", totalSec / 60L, totalSec % 60L, ms % 1000L);
    }

    /** Número com 1 casa decimal e vírgula. */
    public static String num(double v) {
        return String.format(Locale.ROOT, "%.1f", v).replace('.', ',');
    }

    public static String dateTime(long epochMs) {
        return new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date(epochMs));
    }
}
