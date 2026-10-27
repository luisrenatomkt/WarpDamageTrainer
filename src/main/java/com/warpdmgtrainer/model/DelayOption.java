package com.warpdmgtrainer.model;

/** Intervalos entre golpes, em milissegundos. */
public enum DelayOption {
    D2000(2000, "2,00"), D1750(1750, "1,75"), D1250(1250, "1,25"), D1000(1000, "1,00"),
    D750(750, "0,75"), D500(500, "0,50"), D250(250, "0,25");

    private final int millis;
    private final String seconds;

    DelayOption(int millis, String seconds) {
        this.millis = millis;
        this.seconds = seconds;
    }

    public int getMillis() { return millis; }
    public long getNanos() { return millis * 1000000L; }
    public String getShortLabel() { return seconds; }
    public String getDescription() { return seconds + " s (" + millis + " ms)"; }

    public static DelayOption fromIndex(int index) {
        DelayOption[] v = values();
        return v[Math.max(0, Math.min(v.length - 1, index))];
    }

    public static DelayOption fromMillis(int ms) {
        for (DelayOption d : values()) if (d.millis == ms) return d;
        return null;
    }
}
