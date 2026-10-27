package com.warpdmgtrainer.core;

import com.warpdmgtrainer.model.DamageLevel;
import com.warpdmgtrainer.model.DelayOption;
import com.warpdmgtrainer.model.EndReason;
import com.warpdmgtrainer.model.HitResult;

import java.util.ArrayList;
import java.util.List;

/** Coleta estatísticas do teste atual, da sessão e o recorde persistido. Sem dependência do Minecraft. */
public class StatsCollector {
    public static final int MAX_HISTORY = 10;

    private long startEpochMs;
    private DamageLevel level = DamageLevel.H1_0;
    private DelayOption delay = DelayOption.D1000;
    private String mode = "LOCAL";
    private int attempts, hitsReceived, blockedHits, partialHits;
    private double requestedDamage, registeredDamage;
    private String lastBlockReason = "";

    private long sessionBestMs;
    private long allTimeBestMs;
    private int totalTests;
    private SessionResult lastResult;
    private final List<SessionResult> history = new ArrayList<SessionResult>();

    public void begin(DamageLevel level, DelayOption delay, String modeCode) {
        this.level = level;
        this.delay = delay;
        this.mode = modeCode;
        this.startEpochMs = System.currentTimeMillis();
        attempts = hitsReceived = blockedHits = partialHits = 0;
        requestedDamage = registeredDamage = 0.0;
        lastBlockReason = "";
    }

    public void registerAttempt(float amount) {
        attempts++;
        requestedDamage += amount;
    }

    /** Resultado real vindo do servidor integrado (modo LOCAL). */
    public void registerHit(HitResult r) {
        if (r.getApplied() > 0.0001f) {
            hitsReceived++;
            registeredDamage += r.getApplied();
            if (r.isIframeLimited() && r.getApplied() < r.getRequested() - 0.01f) partialHits++;
        } else {
            blockedHits++;
            lastBlockReason = r.getReason();
        }
    }

    /** Dano observado pelo cliente (modo MONITOR). */
    public void registerMonitoredDamage(float amount) {
        hitsReceived++;
        registeredDamage += amount;
    }

    public SessionResult finish(EndReason reason, long durationMs, long skipped) {
        SessionResult r = new SessionResult(startEpochMs, durationMs, level, delay, attempts, hitsReceived,
                blockedHits, partialHits, skipped, requestedDamage, registeredDamage, reason, mode, lastBlockReason);
        lastResult = r;
        totalTests++;
        if (reason != EndReason.INTERRUPTED) {
            if (durationMs > sessionBestMs) sessionBestMs = durationMs;
            if (durationMs > allTimeBestMs) allTimeBestMs = durationMs;
        }
        history.add(0, r);
        while (history.size() > MAX_HISTORY) history.remove(history.size() - 1);
        return r;
    }

    public void restore(long allTimeBest, int total, List<SessionResult> saved) {
        this.allTimeBestMs = Math.max(0L, allTimeBest);
        this.totalTests = Math.max(0, total);
        history.clear();
        history.addAll(saved);
        lastResult = history.isEmpty() ? null : history.get(0);
    }

    public void resetAll() {
        attempts = hitsReceived = blockedHits = partialHits = 0;
        requestedDamage = registeredDamage = 0.0;
        sessionBestMs = 0L;
        allTimeBestMs = 0L;
        totalTests = 0;
        lastResult = null;
        history.clear();
    }

    public int getAttempts() { return attempts; }
    public int getHitsReceived() { return hitsReceived; }
    public int getBlockedHits() { return blockedHits; }
    public double getRequestedDamage() { return requestedDamage; }
    public double getRegisteredDamage() { return registeredDamage; }
    public long getSessionBestMs() { return sessionBestMs; }
    public long getAllTimeBestMs() { return allTimeBestMs; }
    public int getTotalTests() { return totalTests; }
    public SessionResult getLastResult() { return lastResult; }
    public List<SessionResult> getHistory() { return history; }
}
