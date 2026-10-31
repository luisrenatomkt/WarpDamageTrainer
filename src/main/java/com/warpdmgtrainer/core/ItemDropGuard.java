package com.warpdmgtrainer.core;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.UUID;

/**
 * Remove os itens que o jogador dropa enquanto o teste está ativo (somente no modo LOCAL / servidor integrado).
 *
 * Funcionamento: o item é cancelado em EntityJoinWorldEvent, ou seja, a entidade nunca entra no mundo
 * (não aparece no chão, não gera acúmulo e não há nada para duplicar). O inventário não é tocado: o jogo já
 * retirou o item dele antes de criar o drop.
 *
 * Em servidores multiplayer o cliente não tem autoridade sobre entidades; lá esta proteção nunca é armada.
 * Registrado em MinecraftForge.EVENT_BUS.
 */
public class ItemDropGuard {
    private final DropGuardState state = new DropGuardState();

    public void arm(UUID playerId) { state.arm(playerId); }

    public void disarm() { state.disarm(); }

    public boolean isArmed() { return state.isArmed(); }

    public int getRemovedCount() { return state.getRemovedCount(); }

    /** Dispara antes dos drops da morte: desarma de imediato, sem esperar o tick do cliente. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLivingDeath(LivingDeathEvent event) {
        if (event.entityLiving != null) state.onEntityDeath(event.entityLiving.getUniqueID());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (event.world.isRemote || !state.isArmed() || !(event.entity instanceof EntityItem)) return;
        UUID id = state.getArmedPlayer();
        if (id == null) return;
        EntityPlayer p = event.world.getPlayerEntityByUUID(id);
        if (p == null) return;
        EntityItem item = (EntityItem) event.entity;
        if (state.shouldRemove(id, p.getHealth(), p.isDead,
                item.posX, item.posY, item.posZ, p.posX, p.posY, p.posZ, p.getEyeHeight())) {
            event.setCanceled(true);
        }
    }
}
