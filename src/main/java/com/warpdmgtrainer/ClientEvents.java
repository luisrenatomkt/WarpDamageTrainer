package com.warpdmgtrainer;

import com.warpdmgtrainer.config.ConfigManager;
import com.warpdmgtrainer.core.ResistanceTestManager;
import com.warpdmgtrainer.gui.ResultsGui;
import com.warpdmgtrainer.gui.TrainerGui;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Eventos FML (tick, render tick e teclado). Registrado em FMLCommonHandler.instance().bus(). */
public class ClientEvents {
    private final ResistanceTestManager manager;
    private final ConfigManager config;
    private boolean openRequested;

    public ClientEvents(ResistanceTestManager manager, ConfigManager config) {
        this.manager = manager;
        this.config = config;
    }

    /** Usado pelo comando: a tela é aberta no próximo tick, depois que o chat fecha. */
    public void requestOpenTrainer() { openRequested = true; }

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (WarpDamageTrainer.KEY_OPEN.isPressed() && mc.currentScreen == null) {
            mc.displayGuiScreen(new TrainerGui(manager, config));
        }
        if (WarpDamageTrainer.KEY_HUD.isPressed() && mc.thePlayer != null) {
            config.setHudEnabled(!config.isHudEnabled());
            mc.thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.GOLD + "[Warp Damage] HUD "
                    + (config.isHudEnabled() ? "ativado." : "desativado.")));
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        manager.onClientTick(mc);

        if (mc.thePlayer == null) {
            openRequested = false;
            return;
        }
        if (openRequested && mc.currentScreen == null) {
            openRequested = false;
            mc.displayGuiScreen(new TrainerGui(manager, config));
        } else if (manager.hasPendingResults() && mc.thePlayer.getHealth() > 0.0F
                && (mc.currentScreen == null || mc.currentScreen instanceof TrainerGui)) {
            mc.displayGuiScreen(new ResultsGui(manager, config, manager.consumePendingResults()));
        }
    }

    /** Polling extra a cada frame para maior precisão do temporizador. */
    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) manager.onRenderTick(Minecraft.getMinecraft());
    }
}
