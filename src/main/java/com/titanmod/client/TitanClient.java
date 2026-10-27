package com.titanmod.client;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;
import com.titanmod.TitanMod;
import com.titanmod.registry.ModEntities;

import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

/** Registros exclusivos do cliente. Nunca é carregado em servidor dedicado (value = Dist.CLIENT). */
@EventBusSubscriber(modid = TitanMod.MODID, value = Dist.CLIENT)
public class TitanClient {

    /**
     * Dash do Titã. Não usamos Shift porque o Shift já é "descer do veículo" no Minecraft.
     * O jogador pode trocar a tecla em Opções > Controles.
     */
    public static final KeyMapping DASH_KEY = new KeyMapping(
            "key.titanmod.dash", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, "key.categories.titanmod");

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.TITAN.get(), TitanRenderer::new);
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(DASH_KEY);
    }

    @SubscribeEvent
    public static void registerHud(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(TitanMod.MODID, "titan_hud"), TitanHud::render);
    }
}
