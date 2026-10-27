package com.warpdmgtrainer.gui;

import com.warpdmgtrainer.config.ConfigManager;
import com.warpdmgtrainer.core.ResistanceTestManager;
import com.warpdmgtrainer.core.SessionResult;
import com.warpdmgtrainer.util.TimeFormat;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.EnumChatFormatting;

import java.io.IOException;

/** Tela de resultados exibida ao final do teste. */
public class ResultsGui extends GuiScreen {
    private final ResistanceTestManager manager;
    private final ConfigManager config;
    private final SessionResult r;

    public ResultsGui(ResistanceTestManager manager, ConfigManager config, SessionResult r) {
        this.manager = manager;
        this.config = config;
        this.r = r;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        buttonList.add(new GuiButton(1, width / 2 - 155, height / 2 + 107, 150, 20, "ABRIR PAINEL"));
        buttonList.add(new GuiButton(2, width / 2 + 5, height / 2 + 107, 150, 20, "FECHAR"));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 1) mc.displayGuiScreen(new TrainerGui(manager, config));
        else mc.displayGuiScreen(null);
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int w = 340, h = 266;
        int x = (width - w) / 2, top = height / 2 - 133;
        drawRect(x - 1, top - 1, x + w + 1, top + h + 1, 0xFF000000);
        drawRect(x, top, x + w, top + h, 0xF0202020);
        drawRect(x, top, x + w, top + 3, 0xFFFF5555);
        drawCenteredString(fontRendererObj, EnumChatFormatting.BOLD + "RESULTADOS DO TESTE", width / 2, top + 10, 0xFFFFFF);

        int lx = x + 12, y = top + 24;
        boolean local = r.isLocalMode();
        line(lx, y, "Início", TimeFormat.dateTime(r.startEpochMs)); y += 10;
        line(lx, y, "Duração (sobrevivência)", TimeFormat.duration(r.durationMs)); y += 10;
        line(lx, y, "Motivo do encerramento", r.reason.getLabel()); y += 10;
        line(lx, y, "Modo", local ? "LOCAL (servidor integrado)" : "MONITOR (somente leitura)"); y += 10;
        line(lx, y, "Dano configurado", local ? r.level.getDescription() : "n/a (monitor)"); y += 10;
        line(lx, y, "Delay configurado", local ? r.delay.getDescription() : "n/a (monitor)"); y += 12;
        line(lx, y, "Tentativas de golpe", local ? String.valueOf(r.attempts) : "n/a"); y += 10;
        line(lx, y, "Golpes recebidos (dano real)", String.valueOf(r.hitsReceived)); y += 10;
        line(lx, y, "Golpes sem dano / bloqueados", local ? String.valueOf(r.blockedHits) : "n/a"); y += 10;
        line(lx, y, "Golpes parciais (invulnerabilidade)", local ? String.valueOf(r.partialHits) : "n/a"); y += 10;
        line(lx, y, "Dano total configurado", local ? TimeFormat.num(r.requestedDamage) + " pts" : "n/a"); y += 10;
        line(lx, y, "Dano efetivamente registrado", TimeFormat.num(r.registeredDamage) + " pts"); y += 10;
        line(lx, y, "Intervalos perdidos por lag", local ? String.valueOf(r.skippedIntervals) : "n/a"); y += 10;
        line(lx, y, "Melhor tempo da sessão", TimeFormat.duration(manager.getStats().getSessionBestMs())); y += 10;
        line(lx, y, "Recorde salvo", TimeFormat.duration(manager.getStats().getAllTimeBestMs())); y += 13;

        int ny = y;
        if (local && r.blockedHits > 0) {
            String why = r.blockReason.isEmpty() ? "proteção do jogo" : r.blockReason;
            ny = note(lx, ny, r.blockedHits + " tentativa(s) não causaram dano (" + why
                    + "). Elas NÃO foram contadas como golpes recebidos.", w - 24);
        }
        if (local && r.delay.getMillis() <= 500) {
            ny = note(lx, ny, "Com delay de 0,50 s ou menos, a invulnerabilidade temporária do Minecraft (0,5 s) "
                    + "reduz ou anula golpes.", w - 24);
        }
        if (!local) {
            note(lx, ny, "Modo monitor: o mod não aplicou dano. O dano mostrado é o real recebido do servidor "
                    + "(qualquer fonte).", w - 24);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void line(int x, int y, String label, String value) {
        drawString(fontRendererObj, EnumChatFormatting.GRAY + label + ": " + EnumChatFormatting.WHITE + value, x, y, 0xFFFFFF);
    }

    private int note(int x, int y, String text, int maxWidth) {
        for (Object o : fontRendererObj.listFormattedStringToWidth(text, maxWidth)) {
            drawString(fontRendererObj, (String) o, x, y, 0xFFAA55);
            y += 10;
        }
        return y + 2;
    }
}
