package com.warpdmgtrainer.core;

import com.warpdmgtrainer.config.ConfigManager;
import com.warpdmgtrainer.config.PersistenceManager;
import com.warpdmgtrainer.damage.DamageApplier;
import com.warpdmgtrainer.damage.DamageApplierFactory;
import com.warpdmgtrainer.model.DamageLevel;
import com.warpdmgtrainer.model.DelayOption;
import com.warpdmgtrainer.model.EndReason;
import com.warpdmgtrainer.model.HitResult;
import com.warpdmgtrainer.model.TestState;
import com.warpdmgtrainer.util.TimeFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.WorldSettings;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Orquestra o teste de resistência. Tudo roda na thread do cliente (sem threads próprias);
 * o único trabalho no servidor integrado é o golpe, agendado com addScheduledTask.
 */
public class ResistanceTestManager {
    private final ConfigManager config;
    private final PersistenceManager persistence;
    private final ItemDropGuard itemGuard;
    private final StatsCollector stats = new StatsCollector();
    private final PrecisionTimer timer = new PrecisionTimer();
    private final ConcurrentLinkedQueue<HitResult> results = new ConcurrentLinkedQueue<HitResult>();

    private TestState state = TestState.IDLE;
    private DamageApplier applier;
    private int sessionId;
    private long lastNano = System.nanoTime();
    private long elapsedNanos;
    private float lastHealth;
    private String statusMessage = "";

    public ResistanceTestManager(ConfigManager config, PersistenceManager persistence, ItemDropGuard itemGuard) {
        this.config = config;
        this.persistence = persistence;
        this.itemGuard = itemGuard;
        persistence.loadInto(stats);
    }

    // ------------------------------------------------------------------ controle

    public boolean start(Minecraft mc) {
        if (isActive()) {
            notifyPlayer(EnumChatFormatting.YELLOW, "Já existe um teste em andamento.");
            return false;
        }
        String problem = checkConditions(mc);
        if (problem != null) {
            notifyPlayer(EnumChatFormatting.RED, problem);
            return false;
        }
        applier = DamageApplierFactory.select(mc);
        sessionId++;
        results.clear();
        DelayOption delay = config.getDelayOption();
        stats.begin(config.getDamageLevel(), delay, applier.getModeCode());
        timer.reset(delay.getNanos());
        elapsedNanos = 0L;
        lastNano = System.nanoTime();
        lastHealth = mc.thePlayer.getHealth() + mc.thePlayer.getAbsorptionAmount();
        state = TestState.RUNNING;
        if (applier.appliesDamage()) {
            itemGuard.arm(mc.thePlayer.getUniqueID());
            notifyPlayer(EnumChatFormatting.GREEN, "Teste iniciado: " + config.getDamageLevel().getDescription()
                    + " a cada " + delay.getDescription() + ".");
            notifyPlayer(EnumChatFormatting.YELLOW, "Remoção automática de itens dropados: ATIVA. "
                    + "Ela desliga sozinha quando você morrer ou o teste terminar.");
        } else {
            itemGuard.disarm();
            notifyPlayer(EnumChatFormatting.YELLOW, "Servidor sem suporte: modo MONITOR. Nenhum dano será aplicado; "
                    + "apenas o dano real recebido será registrado.");
        }
        return true;
    }

    public void togglePause(Minecraft mc) {
        if (state == TestState.RUNNING) {
            state = TestState.PAUSED;
            notifyPlayer(EnumChatFormatting.YELLOW, "Teste pausado.");
        } else if (state == TestState.PAUSED) {
            lastNano = System.nanoTime();
            if (mc.thePlayer != null) lastHealth = mc.thePlayer.getHealth() + mc.thePlayer.getAbsorptionAmount();
            state = TestState.RUNNING;
            notifyPlayer(EnumChatFormatting.GREEN, "Teste retomado.");
        }
    }

    public void stop() {
        if (!isActive()) return;
        drainResults();
        if (isActive()) finish(EndReason.MANUAL);
    }

    /** Chamado quando o mundo/conexão fecha. */
    public void interrupt() {
        if (isActive()) finish(EndReason.INTERRUPTED);
    }

    public void resetStats() {
        if (isActive()) {
            notifyPlayer(EnumChatFormatting.RED, "Encerre o teste antes de resetar as estatísticas.");
            return;
        }
        stats.resetAll();
        persistence.save(stats);
        state = TestState.IDLE;
        notifyPlayer(EnumChatFormatting.GREEN, "Estatísticas resetadas.");
    }

    public boolean setDamageLevel(DamageLevel level) {
        if (isActive()) return false;
        config.setDamageLevel(level);
        return true;
    }

    public boolean setDelayOption(DelayOption option) {
        if (isActive()) return false;
        config.setDelayOption(option);
        return true;
    }

    // ------------------------------------------------------------------ laço principal

    public void onClientTick(Minecraft mc) { update(mc, true); }

    public void onRenderTick(Minecraft mc) { update(mc, false); }

    private void update(Minecraft mc, boolean fromTick) {
        long now = System.nanoTime();
        long dt = now - lastNano;
        lastNano = now;
        if (!isActive()) return;

        drainResults();
        if (!isActive()) return;

        EntityPlayerSP p = mc.thePlayer;
        if (mc.theWorld == null || p == null) {
            finish(EndReason.INTERRUPTED);
            return;
        }
        if (state == TestState.PAUSED || isGamePaused(mc)) return;

        elapsedNanos += dt;

        if (p.getHealth() <= 0.0F || p.isDead) {
            finish(EndReason.DEATH);
            return;
        }

        if (applier.appliesDamage()) {
            if (timer.advance(dt)) {
                float amount = config.getDamageLevel().getHealthPoints();
                stats.registerAttempt(amount);
                applier.apply(mc, amount, sessionId, results);
            }
        } else if (fromTick) {
            float cur = p.getHealth() + p.getAbsorptionAmount();
            if (cur < lastHealth - 0.001F) stats.registerMonitoredDamage(lastHealth - cur);
            lastHealth = cur;
        }
    }

    private void drainResults() {
        HitResult r;
        while ((r = results.poll()) != null) {
            if (r.getSessionId() != sessionId || !isActive()) continue;
            stats.registerHit(r);
            if (r.isFatal()) {
                finish(EndReason.DEATH);
                return;
            }
        }
    }

    private void finish(EndReason reason) {
        itemGuard.disarm(); // drops voltam ao normal imediatamente
        long ms = elapsedNanos / 1000000L;
        SessionResult r = stats.finish(reason, ms, timer.getSkipped());
        persistence.save(stats);
        state = TestState.FINISHED;
        results.clear();
        statusMessage = "Teste finalizado (" + reason.getLabel() + ").";
        postResultsToChat(r);
    }

    /** Resultados do teste no chat (substitui a antiga tela de resultados). */
    private void postResultsToChat(SessionResult r) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) return; // mundo fechado: nada a exibir
        boolean local = r.isLocalMode();
        EnumChatFormatting g = EnumChatFormatting.GRAY;
        EnumChatFormatting w = EnumChatFormatting.WHITE;

        chat(EnumChatFormatting.GOLD + "" + EnumChatFormatting.BOLD + "===== RESULTADOS DO TESTE =====");
        chat(g + "Início: " + w + TimeFormat.dateTime(r.startEpochMs));
        chat(g + "Duração (sobrevivência): " + EnumChatFormatting.GREEN + TimeFormat.duration(r.durationMs));
        chat(g + "Motivo do encerramento: " + (r.reason == EndReason.DEATH ? EnumChatFormatting.RED : w)
                + r.reason.getLabel());
        chat(g + "Modo: " + w + (local ? "LOCAL (servidor integrado)" : "MONITOR (somente leitura)"));
        if (local) {
            chat(g + "Dano configurado: " + w + r.level.getDescription()
                    + g + " | Delay: " + w + r.delay.getDescription());
            chat(g + "Tentativas de golpe: " + w + r.attempts);
        }
        chat(g + "Golpes recebidos (dano real): " + w + r.hitsReceived);
        if (local) {
            chat(g + "Golpes sem dano / bloqueados: " + w + r.blockedHits
                    + g + " | Parciais (invulnerabilidade): " + w + r.partialHits);
            chat(g + "Dano total configurado: " + w + TimeFormat.num(r.requestedDamage) + " pts");
        }
        chat(g + "Dano efetivamente registrado: " + w + TimeFormat.num(r.registeredDamage) + " pts");
        if (local) chat(g + "Intervalos perdidos por lag: " + w + r.skippedIntervals);
        chat(g + "Melhor tempo da sessão: " + w + TimeFormat.duration(stats.getSessionBestMs())
                + g + " | Recorde salvo: " + w + TimeFormat.duration(stats.getAllTimeBestMs()));

        if (local && r.blockedHits > 0) {
            String why = r.blockReason.isEmpty() ? "proteção do jogo" : r.blockReason;
            chat(EnumChatFormatting.YELLOW + "" + r.blockedHits + " tentativa(s) não causaram dano (" + why
                    + "). Não foram contadas como golpes recebidos.");
        }
        if (local && r.delay.getMillis() <= 500) {
            chat(EnumChatFormatting.YELLOW + "Com delay de 0,50 s ou menos, a invulnerabilidade temporária do "
                    + "Minecraft (0,5 s) reduz ou anula golpes.");
        }
        if (!local) {
            chat(EnumChatFormatting.YELLOW + "Modo monitor: nenhum dano foi aplicado pelo mod; o dano mostrado "
                    + "é o real recebido (qualquer fonte).");
        }
        chat(EnumChatFormatting.AQUA + "Remoção automática de itens: desativada.");
    }

    private void chat(String text) {
        Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(text));
    }

    /** No single-player, qualquer tela que pausa o jogo também congela o relógio do teste. */
    private boolean isGamePaused(Minecraft mc) {
        return mc.isSingleplayer() && mc.getIntegratedServer() != null && mc.currentScreen != null
                && mc.currentScreen.doesGuiPauseGame() && !mc.getIntegratedServer().getPublic();
    }

    private String checkConditions(Minecraft mc) {
        if (mc.theWorld == null || mc.thePlayer == null) return "Entre em um mundo para iniciar o teste.";
        if (mc.playerController != null) {
            WorldSettings.GameType gt = mc.playerController.getCurrentGameType();
            if (!gt.isSurvivalOrAdventure()) {
                return "O teste precisa de condições que permitam receber dano: use Sobrevivência ou Aventura.";
            }
        }
        if (mc.thePlayer.capabilities.disableDamage) {
            return "O teste precisa de condições que permitam receber dano: o jogador está invulnerável.";
        }
        return null;
    }

    // ------------------------------------------------------------------ mensagens/estado

    private void notifyPlayer(EnumChatFormatting color, String text) {
        statusMessage = text;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null && mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText(
                    EnumChatFormatting.GOLD + "[Warp Damage] " + color + text));
        }
    }

    public boolean isActive() { return state == TestState.RUNNING || state == TestState.PAUSED; }
    public TestState getState() { return state; }
    public String getStatusMessage() { return statusMessage; }
    public long getElapsedMs() { return elapsedNanos / 1000000L; }
    public StatsCollector getStats() { return stats; }
    public ItemDropGuard getItemGuard() { return itemGuard; }
    public DamageLevel getDamageLevel() { return config.getDamageLevel(); }
    public DelayOption getDelayOption() { return config.getDelayOption(); }

    public String getModeName(Minecraft mc) {
        return (isActive() && applier != null ? applier : DamageApplierFactory.select(mc)).getModeName();
    }

    /** 0..1 até o próximo golpe, ou -1 se não se aplica (monitor/parado). */
    public float getNextHitProgress() {
        return isActive() && applier != null && applier.appliesDamage() ? timer.getProgress() : -1f;
    }
}
