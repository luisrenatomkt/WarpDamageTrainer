package com.warpdmgtrainer.core;

/**
 * Temporizador por contagem regressiva baseado em deltas de System.nanoTime().
 * Nunca dispara mais de um golpe por chamada: se houver travamento (queda de FPS),
 * os intervalos perdidos são apenas contabilizados em "skipped", sem rajada de golpes.
 * Não usa threads.
 */
public class PrecisionTimer {
    private long intervalNanos = 1000000000L;
    private long remainingNanos = intervalNanos;
    private long skipped;

    public void reset(long intervalNanos) {
        this.intervalNanos = Math.max(1L, intervalNanos);
        this.remainingNanos = this.intervalNanos;
        this.skipped = 0L;
    }

    /** Avança o relógio. Retorna true se UM golpe deve ser disparado agora. */
    public boolean advance(long deltaNanos) {
        if (deltaNanos < 0) deltaNanos = 0;
        remainingNanos -= deltaNanos;
        if (remainingNanos > 0) return false;
        remainingNanos += intervalNanos;
        if (remainingNanos <= 0) {
            skipped += (-remainingNanos) / intervalNanos + 1L;
            remainingNanos = intervalNanos;
        }
        return true;
    }

    public long getSkipped() { return skipped; }

    /** 0..1 do progresso até o próximo golpe. */
    public float getProgress() {
        float p = 1f - (float) remainingNanos / (float) intervalNanos;
        return Math.max(0f, Math.min(1f, p));
    }
}
