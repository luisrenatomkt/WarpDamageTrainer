package com.warpdmgtrainer.model;

/** Resultado real de uma tentativa de golpe (imutável, seguro entre threads). */
public final class HitResult {
    private final int sessionId;
    private final float requested;
    private final float applied;
    private final boolean fatal;
    private final boolean iframeLimited;
    private final String reason;

    private HitResult(int sessionId, float requested, float applied, boolean fatal,
                      boolean iframeLimited, String reason) {
        this.sessionId = sessionId;
        this.requested = requested;
        this.applied = applied;
        this.fatal = fatal;
        this.iframeLimited = iframeLimited;
        this.reason = reason == null ? "" : reason;
    }

    public static HitResult hit(int sessionId, float requested, float applied, boolean fatal,
                                boolean iframeLimited, String reason) {
        return new HitResult(sessionId, requested, applied, fatal, iframeLimited, reason);
    }

    public static HitResult blocked(int sessionId, float requested, String reason) {
        return new HitResult(sessionId, requested, 0f, false, false, reason);
    }

    public int getSessionId() { return sessionId; }
    public float getRequested() { return requested; }
    public float getApplied() { return applied; }
    public boolean isFatal() { return fatal; }
    public boolean isIframeLimited() { return iframeLimited; }
    public String getReason() { return reason; }
}
