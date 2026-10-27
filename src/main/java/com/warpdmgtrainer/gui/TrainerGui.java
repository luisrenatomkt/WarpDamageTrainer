package com.warpdmgtrainer.gui;

import com.warpdmgtrainer.WarpDamageTrainer;
import com.warpdmgtrainer.config.ConfigManager;
import com.warpdmgtrainer.core.ResistanceTestManager;
import com.warpdmgtrainer.core.StatsCollector;
import com.warpdmgtrainer.model.DamageLevel;
import com.warpdmgtrainer.model.DelayOption;
import com.warpdmgtrainer.model.TestState;
import com.warpdmgtrainer.util.TimeFormat;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;

/** Painel principal do mod. */
public class TrainerGui extends GuiScreen {
    private static final ResourceLocation ICONS = new ResourceLocation("textures/gui/icons.png");

    private static final int ID_DAMAGE = 0;   // 0..8
    private static final int ID_DELAY = 20;   // 20..26
    private static final int ID_START = 100, ID_PAUSE = 101, ID_STOP = 102, ID_RESET = 103,
            ID_HUD = 104, ID_CLOSE = 105;
    private static final int PANEL_W = 330, PANEL_H = 262;

    private final ResistanceTestManager manager;
    private final ConfigManager config;
    private final GuiButton[] damageBtns = new GuiButton[DamageLevel.values().length];
    private final GuiButton[] delayBtns = new GuiButton[DelayOption.values().length];
    private GuiButton startBtn, pauseBtn, stopBtn, resetBtn, hudBtn;
    private int panelX, panelY;

    public TrainerGui(ResistanceTestManager manager, ConfigManager config) {
        this.manager = manager;
        this.config = config;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        panelX = (width - PANEL_W) / 2;
        panelY = Math.max(2, (height - PANEL_H) / 2);
        int x0 = panelX + 10;

        DamageLevel[] dls = DamageLevel.values();
        for (int i = 0; i < dls.length; i++) {
            damageBtns[i] = new GuiButton(ID_DAMAGE + i, x0 + i * 34, panelY + 32, 33, 18, dls[i].getLabel());
            buttonList.add(damageBtns[i]);
        }
        DelayOption[] dos = DelayOption.values();
        for (int i = 0; i < dos.length; i++) {
            delayBtns[i] = new GuiButton(ID_DELAY + i, x0 + i * 44, panelY + 78, 43, 18, dos[i].getShortLabel());
            buttonList.add(delayBtns[i]);
        }
        int cy = panelY + 114;
        startBtn = new GuiButton(ID_START, x0, cy, 150, 20, "INICIAR TESTE");
        pauseBtn = new GuiButton(ID_PAUSE, x0 + 160, cy, 150, 20, "PAUSAR TESTE");
        stopBtn = new GuiButton(ID_STOP, x0, cy + 24, 150, 20, "ENCERRAR TESTE");
        resetBtn = new GuiButton(ID_RESET, x0 + 160, cy + 24, 150, 20, "RESETAR ESTATÍSTICAS");
        hudBtn = new GuiButton(ID_HUD, x0, cy + 48, 150, 20, "");
        GuiButton closeBtn = new GuiButton(ID_CLOSE, x0 + 160, cy + 48, 150, 20, "FECHAR");
        buttonList.add(startBtn);
        buttonList.add(pauseBtn);
        buttonList.add(stopBtn);
        buttonList.add(resetBtn);
        buttonList.add(hudBtn);
        buttonList.add(closeBtn);
        refreshButtons();
    }

    private void refreshButtons() {
        boolean idle = !manager.isActive();
        startBtn.enabled = idle;
        pauseBtn.enabled = !idle;
        pauseBtn.displayString = manager.getState() == TestState.PAUSED ? "RETOMAR TESTE" : "PAUSAR TESTE";
        stopBtn.enabled = !idle;
        resetBtn.enabled = idle;
        hudBtn.displayString = "HUD: " + (config.isHudEnabled() ? "ATIVADO" : "DESATIVADO");

        int selDmg = manager.getDamageLevel().ordinal();
        for (int i = 0; i < damageBtns.length; i++) {
            damageBtns[i].enabled = idle;
            String lbl = DamageLevel.values()[i].getLabel();
            damageBtns[i].displayString = i == selDmg ? EnumChatFormatting.GREEN + lbl : lbl;
        }
        int selDelay = manager.getDelayOption().ordinal();
        for (int i = 0; i < delayBtns.length; i++) {
            delayBtns[i].enabled = idle;
            String lbl = DelayOption.values()[i].getShortLabel();
            delayBtns[i].displayString = i == selDelay ? EnumChatFormatting.GREEN + lbl : lbl;
        }
    }

    @Override
    public void updateScreen() {
        refreshButtons();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        int id = button.id;
        if (id >= ID_DAMAGE && id < ID_DAMAGE + DamageLevel.values().length) {
            manager.setDamageLevel(DamageLevel.values()[id - ID_DAMAGE]);
        } else if (id >= ID_DELAY && id < ID_DELAY + DelayOption.values().length) {
            manager.setDelayOption(DelayOption.values()[id - ID_DELAY]);
        } else if (id == ID_START) {
            manager.start(mc);
        } else if (id == ID_PAUSE) {
            manager.togglePause(mc);
        } else if (id == ID_STOP) {
            manager.stop();
        } else if (id == ID_RESET) {
            manager.resetStats();
        } else if (id == ID_HUD) {
            config.setHudEnabled(!config.isHudEnabled());
        } else if (id == ID_CLOSE) {
            mc.displayGuiScreen(null);
        }
        refreshButtons();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == WarpDamageTrainer.KEY_OPEN.getKeyCode()) {
            mc.displayGuiScreen(null);
            return;
        }
        super.keyTyped(typedChar, keyCode); // ESC fecha
    }

    /** Não pausa o jogo: o teste continua rodando com o painel aberto. */
    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int x0 = panelX + 10;
        drawRect(panelX - 1, panelY - 1, panelX + PANEL_W + 1, panelY + PANEL_H + 1, 0xFF000000);
        drawRect(panelX, panelY, panelX + PANEL_W, panelY + PANEL_H, 0xF0202020);
        drawRect(panelX, panelY, panelX + PANEL_W, panelY + 3, 0xFFFF5555);
        drawCenteredString(fontRendererObj, EnumChatFormatting.BOLD + "WARP DAMAGE TRAINER", width / 2, panelY + 10, 0xFFFFFF);

        drawString(fontRendererObj, "NÍVEL DE DANO (corações)", x0, panelY + 22, 0xAAAAAA);
        DamageLevel dl = manager.getDamageLevel();
        drawString(fontRendererObj, "Selecionado: " + EnumChatFormatting.GREEN + dl.getDescription(),
                x0, panelY + 56, 0xFFFFFF);
        drawHearts(dl.getHealthPoints(), panelX + PANEL_W - 10 - 5 * 9, panelY + 55);

        drawString(fontRendererObj, "DELAY ENTRE GOLPES (segundos)", x0, panelY + 68, 0xAAAAAA);
        drawString(fontRendererObj, "Selecionado: " + EnumChatFormatting.GREEN + manager.getDelayOption().getDescription(),
                x0, panelY + 100, 0xFFFFFF);

        // Estatísticas
        StatsCollector s = manager.getStats();
        TestState st = manager.getState();
        int c1 = x0, c2 = x0 + 160, y = panelY + 188;
        EntityPlayerSP p = mc.thePlayer;
        String life = p == null ? "-" : TimeFormat.num(p.getHealth()) + "/" + TimeFormat.num(p.getMaxHealth())
                + (p.getAbsorptionAmount() > 0 ? " (+" + TimeFormat.num(p.getAbsorptionAmount()) + ")" : "");
        boolean local = manager.getModeName(mc).startsWith("LOCAL");

        stat("Estado", st.getLabel(), c1, y);
        stat("Cronômetro", TimeFormat.duration(manager.getElapsedMs()), c2, y);
        stat("Golpes recebidos", String.valueOf(s.getHitsReceived()), c1, y + 11);
        stat("Tentativas", local ? String.valueOf(s.getAttempts()) : "-", c2, y + 11);
        stat("Dano configurado", local ? TimeFormat.num(s.getRequestedDamage()) : "-", c1, y + 22);
        stat("Dano registrado", TimeFormat.num(s.getRegisteredDamage()), c2, y + 22);
        stat("Vida atual", life, c1, y + 33);
        stat("Melhor (sessão)", TimeFormat.duration(s.getSessionBestMs()), c2, y + 33);
        stat("Modo", manager.getModeName(mc), c1, y + 44);
        stat("Recorde salvo", TimeFormat.duration(s.getAllTimeBestMs()), c2, y + 44);

        String msg = manager.getStatusMessage();
        if (!msg.isEmpty()) {
            drawString(fontRendererObj, fontRendererObj.trimStringToWidth(msg, PANEL_W - 20), x0, y + 58, 0xFFAA55);
        }
        super.drawScreen(mouseX, mouseY, partialTicks); // botões
    }

    private void stat(String label, String value, int x, int y) {
        drawString(fontRendererObj, EnumChatFormatting.GRAY + label + ": " + EnumChatFormatting.WHITE + value, x, y, 0xFFFFFF);
    }

    /** Desenha até 5 corações usando o sprite nativo do Minecraft (cheio/meio). */
    private void drawHearts(float hp, int x, int y) {
        mc.getTextureManager().bindTexture(ICONS);
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.enableBlend();
        for (int i = 0; i < 5; i++) {
            drawTexturedModalRect(x + i * 9, y, 16, 0, 9, 9);
            if (hp >= (i + 1) * 2) drawTexturedModalRect(x + i * 9, y, 52, 0, 9, 9);
            else if (hp >= i * 2 + 1) drawTexturedModalRect(x + i * 9, y, 61, 0, 9, 9);
        }
        GlStateManager.disableBlend();
    }
}
