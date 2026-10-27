package com.titanmod;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.titanmod.network.ModNetwork;
import com.titanmod.registry.ModEntities;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/**
 * Classe principal do mod. Roda tanto no cliente quanto no servidor (inclusive o servidor integrado
 * do singleplayer). O FML injeta o IEventBus do mod automaticamente no construtor.
 */
@Mod(TitanMod.MODID)
public class TitanMod {
    public static final String MODID = "titanmod";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TitanMod(IEventBus modEventBus) {
        // Registra os DeferredRegisters no bus do mod
        ModEntities.ENTITY_TYPES.register(modEventBus);
        // Os atributos da entidade precisam ser registrados no evento próprio
        modEventBus.addListener(ModEntities::registerAttributes);
        // Pacotes de rede (cliente -> servidor)
        modEventBus.addListener(ModNetwork::register);
    }
}
