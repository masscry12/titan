package com.titanmod.registry;

import com.titanmod.TitanMod;
import com.titanmod.entity.TitanEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, TitanMod.MODID);

    /** Hitbox de 2,2 x 6 blocos, como definido no planejamento. */
    public static final DeferredHolder<EntityType<?>, EntityType<TitanEntity>> TITAN =
            ENTITY_TYPES.register("titan", () -> EntityType.Builder
                    .of(TitanEntity::new, MobCategory.MISC)
                    .sized(2.2F, 6.0F)
                    // Ponto onde o piloto "senta" (altura da cabine). Ajuste fino aqui se o olho ficar baixo/alto.
                    .passengerAttachments(4.0F)
                    .clientTrackingRange(16)
                    .build("titan"));

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(TITAN.get(), TitanEntity.createAttributes().build());
    }
}
