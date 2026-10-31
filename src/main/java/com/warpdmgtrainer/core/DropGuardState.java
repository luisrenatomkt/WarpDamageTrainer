package com.warpdmgtrainer.core;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Estado e regra de decisão da remoção automática de itens dropados (sem dependência do Minecraft,
 * por isso é testável isoladamente).
 *
 * Seguro entre threads: o gerenciador (thread do cliente) arma/desarma; os eventos rodam na thread do servidor.
 */
public class DropGuardState {
    /** Tolerâncias para reconhecer o ponto de spawn de um drop do jogador. */
    private static final double EPS_XZ = 0.001D;
    private static final double EPS_Y = 0.01D;

    private volatile UUID armedPlayer;
    private final AtomicInteger removed = new AtomicInteger();

    /** Liga a remoção para este jogador e zera o contador. */
    public void arm(UUID playerId) {
        removed.set(0);
        armedPlayer = playerId;
    }

    /** Desliga imediatamente. */
    public void disarm() {
        armedPlayer = null;
    }

    public boolean isArmed() { return armedPlayer != null; }

    public UUID getArmedPlayer() { return armedPlayer; }

    public int getRemovedCount() { return removed.get(); }

    /** Morte de qualquer entidade: se for o jogador armado, desarma na hora (antes dos drops da morte). */
    public void onEntityDeath(UUID deadId) {
        UUID id = armedPlayer;
        if (id != null && id.equals(deadId)) armedPlayer = null;
    }

    /**
     * Decide se um item que está entrando no mundo deve ser removido.
     * O jogo cria drops do jogador (Q, Ctrl+Q, arrastar para fora do inventário) em
     * (posX, posY - 0.3 + alturaDosOlhos, posZ); drops de blocos e mobs usam posições aleatórias,
     * então essa assinatura não atinge outros itens.
     */
    public boolean shouldRemove(UUID playerId, float playerHealth, boolean playerDead,
                                double itemX, double itemY, double itemZ,
                                double playerX, double playerY, double playerZ, float eyeHeight) {
        UUID id = armedPlayer;
        if (id == null || !id.equals(playerId)) return false;
        if (playerDead || playerHealth <= 0.0F) return false;
        double spawnY = playerY - 0.30000001192092896D + (double) eyeHeight;
        boolean fromPlayer = Math.abs(itemX - playerX) < EPS_XZ
                && Math.abs(itemZ - playerZ) < EPS_XZ
                && Math.abs(itemY - spawnY) < EPS_Y;
        if (fromPlayer) removed.incrementAndGet();
        return fromPlayer;
    }
}
