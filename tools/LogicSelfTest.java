import com.warpdmgtrainer.core.*;
import com.warpdmgtrainer.model.*;
import java.util.*;

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

        System.out.println("\nResumo: " + pass + " OK, " + fail + " falha(s)");
        System.exit(fail == 0 ? 0 : 1);
    }
}
