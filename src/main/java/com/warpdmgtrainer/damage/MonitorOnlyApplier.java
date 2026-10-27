package com.warpdmgtrainer.damage;

import com.warpdmgtrainer.model.HitResult;
import net.minecraft.client.Minecraft;

import java.util.Queue;

/** Multiplayer sem suporte: não aplica dano; o gerenciador apenas observa a vida real do jogador. */
public class MonitorOnlyApplier implements DamageApplier {
    @Override public String getModeCode() { return "MONITOR"; }
    @Override public String getModeName() { return "MONITOR (somente leitura)"; }
    @Override public boolean appliesDamage() { return false; }

    @Override
    public void apply(Minecraft mc, float amount, int sessionId, Queue<HitResult> sink) {
        // Intencionalmente vazio: nenhum dano é aplicado em servidores.
    }
}
