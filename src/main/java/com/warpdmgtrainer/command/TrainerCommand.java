package com.warpdmgtrainer.command;

import com.warpdmgtrainer.ClientEvents;
import com.warpdmgtrainer.config.ConfigManager;
import com.warpdmgtrainer.core.ResistanceTestManager;
import com.warpdmgtrainer.model.DamageLevel;
import com.warpdmgtrainer.model.DelayOption;
import com.warpdmgtrainer.util.TimeFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

/** Comando client-side /warpdmg. */
public class TrainerCommand extends CommandBase {
    private final ResistanceTestManager manager;
    private final ConfigManager config;
    private final ClientEvents events;

    public TrainerCommand(ResistanceTestManager manager, ConfigManager config, ClientEvents events) {
        this.manager = manager;
        this.config = config;
        this.events = events;
    }

    @Override public String getCommandName() { return "warpdmg"; }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/warpdmg [abrir|iniciar|pausar|encerrar|resetar|hud|status|dano <1-5 corações>|delay <segundos ou ms>]";
    }

    @Override public int getRequiredPermissionLevel() { return 0; }

    @Override public boolean canCommandSenderUseCommand(ICommandSender sender) { return true; }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        Minecraft mc = Minecraft.getMinecraft();
        String sub = args.length == 0 ? "abrir" : args[0].toLowerCase();

        if (sub.equals("abrir")) {
            events.requestOpenTrainer();
        } else if (sub.equals("iniciar")) {
            manager.start(mc);
        } else if (sub.equals("pausar") || sub.equals("retomar")) {
            manager.togglePause(mc);
        } else if (sub.equals("encerrar")) {
            manager.stop();
        } else if (sub.equals("resetar")) {
            manager.resetStats();
        } else if (sub.equals("hud")) {
            config.setHudEnabled(!config.isHudEnabled());
            reply(sender, "HUD " + (config.isHudEnabled() ? "ativado." : "desativado."));
        } else if (sub.equals("status")) {
            reply(sender, "Estado: " + manager.getState().getLabel() + " | Tempo: "
                    + TimeFormat.duration(manager.getElapsedMs()) + " | Golpes: " + manager.getStats().getHitsReceived()
                    + " | Dano: " + TimeFormat.num(manager.getStats().getRegisteredDamage())
                    + " | Nível: " + manager.getDamageLevel().getDescription()
                    + " | Delay: " + manager.getDelayOption().getDescription());
        } else if (sub.equals("dano") && args.length >= 2) {
            Double hearts = parse(args[1]);
            DamageLevel level = hearts == null ? null : DamageLevel.fromHearts(hearts);
            if (level == null) {
                reply(sender, "Valores válidos: 1, 1.5, 2, 2.5, 3, 3.5, 4, 4.5, 5 (corações).");
            } else if (!manager.setDamageLevel(level)) {
                reply(sender, "Não é possível alterar durante um teste.");
            } else {
                reply(sender, "Dano: " + level.getDescription());
            }
        } else if (sub.equals("delay") && args.length >= 2) {
            Double v = parse(args[1]);
            DelayOption opt = v == null ? null : DelayOption.fromMillis((int) Math.round(v < 100 ? v * 1000 : v));
            if (opt == null) {
                reply(sender, "Valores válidos: 2, 1.75, 1.25, 1, 0.75, 0.5, 0.25 (segundos) ou 2000, 1750, ... (ms).");
            } else if (!manager.setDelayOption(opt)) {
                reply(sender, "Não é possível alterar durante um teste.");
            } else {
                reply(sender, "Delay: " + opt.getDescription());
            }
        } else {
            reply(sender, getCommandUsage(sender));
        }
    }

    private static Double parse(String s) {
        try {
            return Double.valueOf(s.replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void reply(ICommandSender sender, String text) {
        sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GOLD + "[Warp Damage] "
                + EnumChatFormatting.WHITE + text));
    }
}
