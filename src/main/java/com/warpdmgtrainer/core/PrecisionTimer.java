package com.warpdmgtrainer.core;

/**
 * Temporizador por contagem regressiva baseado em deltas de System.nanoTime().
 * Nunca dispara mais de um golpe por chamada: se houver travamento (queda de FPS),
 * os intervalos perdidos são apenas contabilizados em "skipped", sem rajada de golpes.
 * Não usa threads.
 */
public class PrecisionTimer {
    /** Atraso máximo (2 ticks) tratado como frame normal; acima disso é travamento. */
    private static final long MAX_CATCHUP_NANOS = 100000000L;

    private long intervalNanos = 1000000000L;
    private long remainingNanos = intervalNanos;
    private long skipped;

    public void reset(long intervalNanos) {
        this.intervalNanos = Math.max(1L, intervalNanos);
        this.remainingNanos = this.intervalNanos;
        this.skipped = 0L;
    }

    /** Compatibilidade: true se pelo menos um golpe está devido agora. */
    public boolean advance(long deltaNanos) {
        return advanceCount(deltaNanos) > 0;
    }

    /**
     * Avança o relógio e retorna quantos golpes devem ser disparados agora.
     * Em frames normais (atraso de até MAX_CATCHUP_NANOS) retorna todos os golpes devidos, o que é
     * necessário para delays menores que a duração de um frame (ex.: 10 ms a 60 FPS).
     * Em travamentos maiores que isso retorna no máximo 1 e conta o restante como "skipped",
     * sem rajada de golpes acumulados.
     */
    public int advanceCount(long deltaNanos) {
        if (deltaNanos < 0) deltaNanos = 0;
        remainingNanos -= deltaNanos;
        if (remainingNanos > 0) return 0;
        long overdue = -remainingNanos;
        long due = overdue / intervalNanos + 1L;
        if (overdue > MAX_CATCHUP_NANOS) {
            skipped += due - 1L;
            remainingNanos = intervalNanos;
            return 1;
        }
        remainingNanos += due * intervalNanos;
        return (int) due;
    }

    public long getSkipped() { return skipped; }

    /** 0..1 do progresso até o próximo golpe. */
    public float getProgress() {
        float p = 1f - (float) remainingNanos / (float) intervalNanos;
        return Math.max(0f, Math.min(1f, p));
    }
}
