package com.warpdmgtrainer.hud;

import com.warpdmgtrainer.config.ConfigManager;
import com.warpdmgtrainer.core.ResistanceTestManager;
import com.warpdmgtrainer.model.TestState;
import com.warpdmgtrainer.util.TimeFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** HUD opcional exibido durante o teste (registrado no MinecraftForge.EVENT_BUS). */
public class TrainerHud extends Gui {
    private final ResistanceTestManager manager;
    private final ConfigManager config;

    public TrainerHud(ResistanceTestManager manager, ConfigManager config) {
        this.manager = manager;
        this.config = config;
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        if (!config.isHudEnabled() || !manager.isActive()) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.gameSettings.showDebugInfo) return;

        FontRenderer fr = mc.fontRendererObj;
        int x = 4, y = 4, w = 128;
        boolean paused = manager.getState() == TestState.PAUSED;
        drawRect(x - 2, y - 2, x + w, y + 57, 0x90000000);
        fr.drawStringWithShadow(EnumChatFormatting.RED + "WARP DAMAGE" + EnumChatFormatting.GRAY
                + (paused ? " [PAUSADO]" : ""), x, y, 0xFFFFFF);
        fr.drawStringWithShadow("Tempo: " + EnumChatFormatting.WHITE + TimeFormat.duration(manager.getElapsedMs()),
                x, y + 11, 0xAAAAAA);
        fr.drawStringWithShadow("Golpes: " + EnumChatFormatting.WHITE + manager.getStats().getHitsReceived()
                + EnumChatFormatting.GRAY + "  Dano: " + EnumChatFormatting.WHITE
                + TimeFormat.num(manager.getStats().getRegisteredDamage()), x, y + 22, 0xAAAAAA);

        float prog = manager.getNextHitProgress();
        if (prog >= 0f) {
            int bx = x, by = y + 35, bw = w - 6;
            drawRect(bx, by, bx + bw, by + 5, 0xFF404040);
            drawRect(bx, by, bx + (int) (bw * prog), by + 5, 0xFFFF5555);
        } else {
            fr.drawStringWithShadow("Modo monitor", x, y + 35, 0xFFFF55);
        }

        if (manager.getItemGuard().isArmed()) {
            fr.drawStringWithShadow("Itens: " + EnumChatFormatting.GREEN + "remoção ATIVA"
                    + EnumChatFormatting.GRAY + " (" + manager.getItemGuard().getRemovedCount() + ")", x, y + 46, 0xAAAAAA);
        } else {
            fr.drawStringWithShadow("Itens: " + EnumChatFormatting.GRAY + "normal", x, y + 46, 0xAAAAAA);
        }
    }
}
