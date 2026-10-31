import com.warpdmgtrainer.core.*;
import com.warpdmgtrainer.model.*;
import java.util.*;
import com.warpdmgtrainer.core.DropGuardState;

/** Testes da lógica pura (sem Minecraft). Execução: veja README / tools/run-logic-tests.sh */
public class LogicSelfTest {
    static int pass = 0, fail = 0;
    static void check(String name, boolean ok) {
        System.out.println((ok ? "[OK]   " : "[FALHA] ") + name);
        if (ok) pass++; else fail++;
    }

    public static void main(String[] a) {
        // 1. tabela de dano
        float[] hp = {2,3,4,5,6,7,8,9,10};
        boolean ok = DamageLevel.values().length == 9;
        for (int i = 0; i < 9; i++) ok &= DamageLevel.values()[i].getHealthPoints() == hp[i]
                && Math.abs(DamageLevel.values()[i].getHearts() - (1 + i * 0.5)) < 1e-6;
        check("Tabela de dano: 1..5 corações = 2..10 pontos", ok);
        check("Descrição '1 coração (2 pts)'", DamageLevel.H1_0.getDescription().equals("1 coração (2 pts)"));
        check("Descrição '2,5 corações (5 pts)'", DamageLevel.H2_5.getDescription().equals("2,5 corações (5 pts)"));
        check("fromHearts(3.5) = 7 pts", DamageLevel.fromHearts(3.5).getHealthPoints() == 7f);
        check("fromHearts(6) = null", DamageLevel.fromHearts(6) == null);

        // 2. tabela de delay
        int[] ms = {2000,1750,1250,1000,750,500,250};
        ok = DelayOption.values().length == 7;
        for (int i = 0; i < 7; i++) ok &= DelayOption.values()[i].getMillis() == ms[i]
                && DelayOption.values()[i].getNanos() == ms[i] * 1000000L;
        check("Tabela de delay (7 opções, ms e ns)", ok);

        // 3. timer: cadência normal a 60 FPS por 10 s com delay 500 ms => 20 golpes
        PrecisionTimer t = new PrecisionTimer();
        t.reset(500000000L);
        int fires = 0;
        long frame = 16666667L;
        long total = 0;
        while (total < 10000000000L) { if (t.advance(frame)) fires++; total += frame; }
        check("Timer 500ms/10s @60FPS => 19..20 golpes (obtido " + fires + ")", fires >= 19 && fires <= 20);
        check("Timer sem intervalos perdidos em FPS normal", t.getSkipped() == 0);

        // 4. timer: lag spike de 3,5 intervalos => UM golpe só, resto contado como perdido
        t.reset(1000000000L);
        boolean f1 = t.advance(3500000000L);
        boolean f2 = t.advance(1000000L);
        check("Lag spike dispara apenas 1 golpe (sem rajada)", f1 && !f2);
        check("Lag spike: intervalos perdidos = 2 (obtido " + t.getSkipped() + ")", t.getSkipped() == 2);

        // 5. timer: 250 ms a 20 TPS (50 ms) => 1 golpe a cada 5 passos
        t.reset(250000000L);
        int c = 0; for (int i = 1; i <= 100; i++) if (t.advance(50000000L)) c++;
        check("Timer 250ms a 50ms/passo, 100 passos => 20 golpes (obtido " + c + ")", c == 20);

        // 6. estatísticas: dano configurado x registrado
        StatsCollector s = new StatsCollector();
        s.begin(DamageLevel.H2_0, DelayOption.D500, "LOCAL");
        for (int i = 0; i < 4; i++) s.registerAttempt(4f);
        s.registerHit(HitResult.hit(1, 4f, 4f, false, false, ""));
        s.registerHit(HitResult.blocked(1, 4f, "Invulnerabilidade temporária"));
        s.registerHit(HitResult.hit(1, 4f, 1.5f, false, true, "parcial"));
        s.registerHit(HitResult.hit(1, 4f, 4f, true, false, ""));
        SessionResult r = s.finish(EndReason.DEATH, 12345, 0);
        check("Tentativas = 4", r.attempts == 4);
        check("Golpes recebidos (dano>0) = 3", r.hitsReceived == 3);
        check("Golpes sem dano = 1", r.blockedHits == 1);
        check("Golpes parciais = 1", r.partialHits == 1);
        check("Dano configurado = 16", Math.abs(r.requestedDamage - 16.0) < 1e-6);
        check("Dano registrado = 9,5 (separado do configurado)", Math.abs(r.registeredDamage - 9.5) < 1e-6);
        check("Melhor tempo atualizado (morte)", s.getSessionBestMs() == 12345 && s.getAllTimeBestMs() == 12345);

        // 7. interrupção não conta para recorde
        s.begin(DamageLevel.H1_0, DelayOption.D1000, "LOCAL");
        s.finish(EndReason.INTERRUPTED, 99999, 0);
        check("Interrupção não altera o recorde", s.getAllTimeBestMs() == 12345);
        s.begin(DamageLevel.H1_0, DelayOption.D1000, "MONITOR");
        s.registerMonitoredDamage(3f);
        s.finish(EndReason.MANUAL, 5000, 0);
        check("Tempo menor não substitui recorde", s.getAllTimeBestMs() == 12345);
        check("Histórico com 3 testes", s.getHistory().size() == 3 && s.getTotalTests() == 3);

        // 8. serialização / persistência
        String line = r.serialize();
        SessionResult p = SessionResult.parse(line);
        check("Serialização ida e volta", p != null && p.durationMs == 12345 && p.level == DamageLevel.H2_0
                && p.delay == DelayOption.D500 && p.hitsReceived == 3 && p.reason == EndReason.DEATH
                && Math.abs(p.registeredDamage - 9.5) < 1e-6 && p.blockReason.equals("Invulnerabilidade temporária"));
        check("Linha corrompida => null", SessionResult.parse("lixo;1") == null && SessionResult.parse(null) == null);
        StatsCollector s2 = new StatsCollector();
        s2.restore(12345, 3, Arrays.asList(p));
        check("Restauração do recorde e histórico", s2.getAllTimeBestMs() == 12345 && s2.getLastResult() == p);

        // 9. limite do histórico
        StatsCollector s3 = new StatsCollector();
        for (int i = 0; i < 15; i++) { s3.begin(DamageLevel.H1_0, DelayOption.D1000, "LOCAL"); s3.finish(EndReason.MANUAL, i, 0); }
        check("Histórico limitado a 10", s3.getHistory().size() == 10);

        // 10. reset
        s3.resetAll();
        check("Reset zera tudo", s3.getAllTimeBestMs() == 0 && s3.getHistory().isEmpty() && s3.getTotalTests() == 0);

        // 11. formatação
        check("TimeFormat 65250ms = 01:05,250", com.warpdmgtrainer.util.TimeFormat.duration(65250).equals("01:05,250"));
        check("TimeFormat num(2.5) = 2,5", com.warpdmgtrainer.util.TimeFormat.num(2.5).equals("2,5"));

 		dropGuardScenarios();

        System.out.println("\nResumo: " + pass + " OK, " + fail + " falha(s)");
        System.exit(fail == 0 ? 0 : 1);
    }

    // ---------------------------------------------------------------------------------------------
    // Simulação dos cenários A-F da remoção de itens, sobre a regra real (DropGuardState).
    // Isto NÃO roda o Minecraft: o "mundo" abaixo imita a ordem de eventos do jogo.
    // ---------------------------------------------------------------------------------------------
    static class FakePlayer { UUID id = UUID.randomUUID(); double x = 10.5, y = 64, z = -3.25; float eye = 1.62f; float health = 20f; boolean dead; }

    static class FakeWorld {
        final DropGuardState guard; final FakePlayer p; final List<String> ground = new ArrayList<String>();
        FakeWorld(DropGuardState g, FakePlayer p) { this.guard = g; this.p = p; }

        /** Drop do jogador (Q, inventário, Ctrl+Q): nasce na posição exata do jogador. */
        void playerDrop(String item) {
            double sy = p.y - 0.30000001192092896D + (double) p.eye;
            if (!guard.shouldRemove(p.id, p.health, p.dead, p.x, sy, p.z, p.x, p.y, p.z, p.eye)) ground.add(item);
        }
        /** Item de bloco/mob perto do jogador, posição aleatória. */
        void blockDrop(String item, double dx, double dy, double dz) {
            if (!guard.shouldRemove(p.id, p.health, p.dead, p.x + dx, p.y + dy, p.z + dz, p.x, p.y, p.z, p.eye)) ground.add(item);
        }
        /** Morte: LivingDeathEvent dispara ANTES dos drops da morte. */
        void die(String... inventory) {
            guard.onEntityDeath(p.id);
            p.health = 0f; p.dead = true;
            for (String it : inventory) playerDrop(it);
        }
        void respawn() { p.health = 20f; p.dead = false; }
    }

    static void dropGuardScenarios() {
        DropGuardState g = new DropGuardState();
        FakePlayer pl = new FakePlayer();
        FakeWorld w = new FakeWorld(g, pl);

        check("Estado inicial: remoção desarmada", !g.isArmed());
        w.playerDrop("espada_antes_de_iniciar");
        check("Antes de iniciar o teste, drops permanecem no chão", w.ground.size() == 1);
        w.ground.clear();

        // Teste A
        g.arm(pl.id);
        w.playerDrop("espada");
        check("Teste A: espada dropada com o mod ativo desaparece", w.ground.isEmpty() && g.getRemovedCount() == 1);

        // Teste B
        String[] itens = {"espada_diamante","armadura","picareta","bloco_pedra","maca","pocao","arco","flecha"};
        for (String it : itens) w.playerDrop(it);
        check("Teste B: 8 itens dropados, todos desaparecem (nenhuma entidade no chão)", w.ground.isEmpty());
        check("Teste B: contador = 9 removidos", g.getRemovedCount() == 9);

        // Não remover o que não é drop do jogador
        w.blockDrop("drop_de_bloco", 0.3, 0.4, -0.2);
        w.blockDrop("drop_de_mob", -0.6, 0.0, 0.9);
        check("Itens de blocos/mobs próximos NÃO são removidos", w.ground.size() == 2);
        w.ground.clear();

        // Outro jogador (LAN/multiplayer) nunca é afetado
        FakePlayer outro = new FakePlayer();
        outro.x = pl.x; outro.y = pl.y; outro.z = pl.z;
        FakeWorld w2 = new FakeWorld(g, outro);
        w2.playerDrop("item_de_outro_jogador");
        check("Drop de outro jogador não é removido", w2.ground.size() == 1);

        // Teste C: morrer durante o teste -> desarma imediatamente, antes dos drops da morte
        w.die("espada_da_morte", "armadura_da_morte");
        check("Teste C: morte desarma a remoção imediatamente", !g.isArmed());
        check("Teste C: itens dropados NA morte permanecem no chão", w.ground.size() == 2);
        w.ground.clear();

        // Teste D
        w.playerDrop("espada_apos_morte");
        check("Teste D: após morrer, espada dropada permanece no chão", w.ground.size() == 1);
        w.ground.clear();

        // Teste E
        w.respawn();
        w.playerDrop("espada_apos_respawn");
        check("Teste E: após renascer, remoção continua desligada", !g.isArmed() && w.ground.size() == 1);
        w.ground.clear();

        // Teste F
        g.arm(pl.id);
        w.playerDrop("espada_teste2");
        check("Teste F: novo início do teste religa a remoção", g.isArmed() && w.ground.isEmpty());
        check("Teste F: contador zerado ao armar e agora = 1", g.getRemovedCount() == 1);

        // Fim manual e interrupção desarmam
        g.disarm();
        w.playerDrop("espada_apos_encerrar");
        check("Encerrar o teste desarma e drops voltam ao normal", w.ground.size() == 1);
        w.ground.clear();

        // Segurança: armado mas jogador já com vida 0 (ex.: morte ainda não notificada) não remove
        g.arm(pl.id);
        pl.health = 0f;
        w.playerDrop("drop_com_vida_zero");
        check("Jogador com vida 0 nunca tem drop removido (mesmo armado)", w.ground.size() == 1);
        pl.health = 20f;
        g.disarm();
    }
}
