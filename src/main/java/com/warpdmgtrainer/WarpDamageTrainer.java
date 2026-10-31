package com.warpdmgtrainer;

import com.warpdmgtrainer.command.TrainerCommand;
import com.warpdmgtrainer.config.ConfigManager;
import com.warpdmgtrainer.config.PersistenceManager;
import com.warpdmgtrainer.core.ItemDropGuard;
import com.warpdmgtrainer.core.ResistanceTestManager;
import com.warpdmgtrainer.hud.TrainerHud;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

/** Classe principal do mod (somente cliente). */
@Mod(modid = WarpDamageTrainer.MODID, name = WarpDamageTrainer.NAME, version = WarpDamageTrainer.VERSION,
        clientSideOnly = true, acceptedMinecraftVersions = "[1.8.9]")
public class WarpDamageTrainer {
    public static final String MODID = "warpdmgtrainer";
    public static final String NAME = "Warp Damage Trainer";
    public static final String VERSION = "1.1.1";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public static final KeyBinding KEY_OPEN =
            new KeyBinding("Abrir painel Warp Damage Trainer", Keyboard.KEY_K, NAME);
    public static final KeyBinding KEY_HUD =
            new KeyBinding("Ativar/desativar HUD do teste", Keyboard.KEY_L, NAME);

    private ConfigManager config;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        config = new ConfigManager(event.getSuggestedConfigurationFile());
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        PersistenceManager persistence = new PersistenceManager(config.getConfiguration());
        ItemDropGuard itemGuard = new ItemDropGuard();
        ResistanceTestManager manager = new ResistanceTestManager(config, persistence, itemGuard);

        ClientRegistry.registerKeyBinding(KEY_OPEN);
        ClientRegistry.registerKeyBinding(KEY_HUD);

        ClientEvents events = new ClientEvents(manager, config);
        FMLCommonHandler.instance().bus().register(events);
        MinecraftForge.EVENT_BUS.register(new TrainerHud(manager, config));
        MinecraftForge.EVENT_BUS.register(itemGuard);
        ClientCommandHandler.instance.registerCommand(new TrainerCommand(manager, config, events));

        LOGGER.info("{} {} carregado.", NAME, VERSION);
    }
}
