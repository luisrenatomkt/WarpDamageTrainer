package com.warpdmgtrainer.core;

import com.warpdmgtrainer.model.DamageLevel;
import com.warpdmgtrainer.model.DelayOption;
import com.warpdmgtrainer.model.EndReason;

/** Resultado imutável de um teste concluído. */
public final class SessionResult {
    public final long startEpochMs;
    public final long durationMs;
    public final DamageLevel level;
    public final DelayOption delay;
    public final int attempts;
    public final int hitsReceived;
    public final int blockedHits;
    public final int partialHits;
    public final long skippedIntervals;
    public final double requestedDamage;
    public final double registeredDamage;
    public final EndReason reason;
    public final String mode; // "LOCAL" ou "MONITOR"
    public final String blockReason;

    public SessionResult(long startEpochMs, long durationMs, DamageLevel level, DelayOption delay,
                         int attempts, int hitsReceived, int blockedHits, int partialHits,
                         long skippedIntervals, double requestedDamage, double registeredDamage,
                         EndReason reason, String mode, String blockReason) {
        this.startEpochMs = startEpochMs;
        this.durationMs = durationMs;
        this.level = level;
        this.delay = delay;
        this.attempts = attempts;
        this.hitsReceived = hitsReceived;
        this.blockedHits = blockedHits;
        this.partialHits = partialHits;
        this.skippedIntervals = skippedIntervals;
        this.requestedDamage = requestedDamage;
        this.registeredDamage = registeredDamage;
        this.reason = reason;
        this.mode = mode;
        this.blockReason = blockReason == null ? "" : blockReason;
    }

    public boolean isLocalMode() { return "LOCAL".equals(mode); }

    public String serialize() {
        return startEpochMs + ";" + durationMs + ";" + level.ordinal() + ";" + delay.ordinal() + ";"
                + attempts + ";" + hitsReceived + ";" + blockedHits + ";" + partialHits + ";"
                + skippedIntervals + ";" + requestedDamage + ";" + registeredDamage + ";"
                + reason.name() + ";" + mode + ";" + blockReason.replace(';', ',');
    }

    /** Retorna null se a linha estiver corrompida. */
    public static SessionResult parse(String line) {
        if (line == null) return null;
        String[] p = line.split(";", -1);
        if (p.length < 14) return null;
        try {
            return new SessionResult(Long.parseLong(p[0]), Long.parseLong(p[1]),
                    DamageLevel.fromIndex(Integer.parseInt(p[2])), DelayOption.fromIndex(Integer.parseInt(p[3])),
                    Integer.parseInt(p[4]), Integer.parseInt(p[5]), Integer.parseInt(p[6]),
                    Integer.parseInt(p[7]), Long.parseLong(p[8]), Double.parseDouble(p[9]),
                    Double.parseDouble(p[10]), EndReason.valueOf(p[11]), p[12], p[13]);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
