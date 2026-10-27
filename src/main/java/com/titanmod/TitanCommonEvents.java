package com.titanmod;

import com.titanmod.entity.TitanEntity;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Eventos de jogo que valem nos dois lados (cliente e servidor). */
@EventBusSubscriber(modid = TitanMod.MODID)
public class TitanCommonEvents {

    /**
     * Segurar o botão esquerdo mira blocos: sem isto o piloto quebraria o terreno enquanto atira.
     * O evento dispara no cliente e no servidor, então cancelamos nos dois.
     */
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getEntity().getVehicle() instanceof TitanEntity) {
            event.setCanceled(true);
        }
    }
}
