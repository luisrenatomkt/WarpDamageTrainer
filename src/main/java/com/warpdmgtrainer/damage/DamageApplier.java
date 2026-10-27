package com.warpdmgtrainer.damage;

import com.warpdmgtrainer.model.HitResult;
import net.minecraft.client.Minecraft;

import java.util.Queue;

/**
 * Ponto de extensão do Modo B. Uma implementação só deve existir quando o servidor
 * AUTORIZA e SUPORTA o recurso (ex.: via protocolo definido pelo próprio servidor).
 * Este mod não traz nenhuma implementação que aplique dano em servidores de terceiros.
 */
public interface DamageApplier {
    /** "LOCAL" ou "MONITOR" (usado na persistência). */
    String getModeCode();

    /** Nome exibido ao usuário. */
    String getModeName();

    /** true se este modo aplica dano; false se apenas observa o dano real. */
    boolean appliesDamage();

    /** Solicita um golpe. O resultado real é entregue de forma assíncrona em {@code sink}. */
    void apply(Minecraft mc, float amount, int sessionId, Queue<HitResult> sink);
}
