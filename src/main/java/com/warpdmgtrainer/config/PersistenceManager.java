package com.warpdmgtrainer.config;

import com.warpdmgtrainer.core.SessionResult;
import com.warpdmgtrainer.core.StatsCollector;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import java.util.ArrayList;
import java.util.List;

/** Salva/carrega recorde, contagem e histórico no mesmo arquivo .cfg do Forge. */
public class PersistenceManager {
    private static final String CAT_STATS = "estatisticas";

    private final Configuration config;
    private final Property bestProp;
    private final Property totalProp;
    private final Property historyProp;

    public PersistenceManager(Configuration config) {
        this.config = config;
        bestProp = config.get(CAT_STATS, "melhorTempoMs", "0", "Melhor tempo de sobrevivencia (ms)");
        totalProp = config.get(CAT_STATS, "totalTestes", 0, "Total de testes concluidos", 0, Integer.MAX_VALUE);
        historyProp = config.get(CAT_STATS, "historico", new String[0],
                "Ultimos testes (nao edite manualmente)");
        config.save();
    }

    public void loadInto(StatsCollector stats) {
        long best = 0L;
        try {
            best = Long.parseLong(bestProp.getString().trim());
        } catch (NumberFormatException ignored) {
            // valor corrompido: usa 0
        }
        List<SessionResult> list = new ArrayList<SessionResult>();
        for (String line : historyProp.getStringList()) {
            SessionResult r = SessionResult.parse(line);
            if (r != null) list.add(r);
        }
        stats.restore(best, totalProp.getInt(), list);
    }

    public void save(StatsCollector stats) {
        bestProp.set(Long.toString(stats.getAllTimeBestMs()));
        totalProp.set(stats.getTotalTests());
        List<SessionResult> h = stats.getHistory();
        String[] lines = new String[h.size()];
        for (int i = 0; i < lines.length; i++) lines[i] = h.get(i).serialize();
        historyProp.set(lines);
        config.save();
    }
}
