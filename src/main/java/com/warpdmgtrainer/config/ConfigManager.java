package com.warpdmgtrainer.config;

import com.warpdmgtrainer.model.DamageLevel;
import com.warpdmgtrainer.model.DelayOption;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import java.io.File;

/** Configurações do usuário, salvas em config/warpdmgtrainer.cfg (formato Forge). */
public class ConfigManager {
    public static final String CAT_GENERAL = "general";

    private final Configuration config;
    private final Property damageProp;
    private final Property delayProp;
    private final Property hudProp;

    public ConfigManager(File file) {
        config = new Configuration(file);
        config.load();
        damageProp = config.get(CAT_GENERAL, "nivelDano", 2,
                "Indice do nivel de dano (0 = 1 coracao ... 8 = 5 coracoes)", 0, DamageLevel.values().length - 1);
        delayProp = config.get(CAT_GENERAL, "indiceDelay", 3,
                "Indice do delay (0 = 2,00s, 1 = 1,75s, 2 = 1,25s, 3 = 1,00s, 4 = 0,75s, 5 = 0,50s, 6 = 0,25s)",
                0, DelayOption.values().length - 1);
        hudProp = config.get(CAT_GENERAL, "hudAtivado", true, "Exibir o HUD durante o teste");
        config.save();
    }

    public Configuration getConfiguration() { return config; }

    public DamageLevel getDamageLevel() { return DamageLevel.fromIndex(damageProp.getInt()); }
    public DelayOption getDelayOption() { return DelayOption.fromIndex(delayProp.getInt()); }
    public boolean isHudEnabled() { return hudProp.getBoolean(); }

    public void setDamageLevel(DamageLevel level) {
        damageProp.set(level.ordinal());
        config.save();
    }

    public void setDelayOption(DelayOption option) {
        delayProp.set(option.ordinal());
        config.save();
    }

    public void setHudEnabled(boolean enabled) {
        hudProp.set(enabled);
        config.save();
    }
}
