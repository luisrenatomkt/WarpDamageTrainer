package com.warpdmgtrainer.damage;

import com.warpdmgtrainer.WarpDamageTrainer;
import com.warpdmgtrainer.model.HitResult;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;

import java.util.Queue;
import java.util.UUID;

/**
 * Modo A: aplica dano real ao jogador no servidor integrado (single-player) usando
 * EntityPlayerMP.attackEntityFrom, respeitando armadura, poções, absorção e invulnerabilidade temporária.
 * O golpe é executado na thread do servidor via addScheduledTask.
 */
public class LocalDamageApplier implements DamageApplier {
    /** Fonte de dano própria (mensagem de morte em lang). Não ignora armadura. */
    public static final DamageSource TRAINER_SOURCE = new DamageSource("warpdmgtrainer") {};

    @Override public String getModeCode() { return "LOCAL"; }
    @Override public String getModeName() { return "LOCAL (servidor integrado)"; }
    @Override public boolean appliesDamage() { return true; }

    @Override
    public void apply(Minecraft mc, final float amount, final int sessionId, final Queue<HitResult> sink) {
        final MinecraftServer server = mc.getIntegratedServer();
        if (server == null || mc.thePlayer == null) {
            sink.add(HitResult.blocked(sessionId, amount, "Servidor integrado indisponível"));
            return;
        }
        final UUID id = mc.thePlayer.getUniqueID();
        server.addScheduledTask(new Runnable() {
            @Override
            public void run() {
                try {
                    sink.add(performHit(server, id, amount, sessionId));
                } catch (RuntimeException e) {
                    WarpDamageTrainer.LOGGER.error("Falha ao aplicar golpe do treino", e);
                    sink.add(HitResult.blocked(sessionId, amount, "Erro interno ao aplicar o golpe"));
                }
            }
        });
    }

    private static HitResult performHit(MinecraftServer server, UUID id, float amount, int sessionId) {
        EntityPlayerMP p = server.getConfigurationManager().getPlayerByUUID(id);
        if (p == null) return HitResult.blocked(sessionId, amount, "Jogador não encontrado no servidor integrado");
        if (p.isDead || p.getHealth() <= 0.0F) return HitResult.blocked(sessionId, amount, "Jogador já está morto");
        if (p.capabilities.disableDamage || p.isSpectator()) {
            return HitResult.blocked(sessionId, amount, "Jogador não pode receber dano (criativo/espectador)");
        }

        boolean iframes = p.hurtResistantTime > (float) p.maxHurtResistantTime / 2.0F;
        float before = p.getHealth() + p.getAbsorptionAmount();
        p.attackEntityFrom(TRAINER_SOURCE, amount);
        float after = p.getHealth() + p.getAbsorptionAmount();
        float applied = Math.max(0.0F, before - after);
        boolean fatal = p.getHealth() <= 0.0F || p.isDead;

        String reason = "";
        if (applied <= 0.0001F) {
            if (iframes) reason = "Invulnerabilidade temporária após o último golpe";
            else if (p.isEntityInvulnerable(TRAINER_SOURCE)) reason = "Jogador invulnerável";
            else reason = "Dano reduzido a zero (armadura/resistência/efeitos)";
        } else if (iframes) {
            reason = "Dano parcial por invulnerabilidade temporária";
        }
        return HitResult.hit(sessionId, amount, applied, fatal, iframes, reason);
    }
}
