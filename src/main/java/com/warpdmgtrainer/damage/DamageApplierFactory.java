package com.warpdmgtrainer.damage;

import net.minecraft.client.Minecraft;

/** Escolhe o modo: single-player => LOCAL; qualquer servidor => MONITOR (somente leitura). */
public final class DamageApplierFactory {
    private static final DamageApplier LOCAL = new LocalDamageApplier();
    private static final DamageApplier MONITOR = new MonitorOnlyApplier();

    private DamageApplierFactory() {}

    public static DamageApplier select(Minecraft mc) {
        return mc.isSingleplayer() ? LOCAL : MONITOR;
    }
}
