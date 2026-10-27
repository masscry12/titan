package com.titanmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.titanmod.entity.TitanEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer PLACEHOLDER do M1: reaproveita o modelo humanoide do zumbi, ampliado ~3x (2 blocos -> 6 blocos).
 * Quando houver um modelo de verdade, ele é trocado por um renderer GeckoLib (marco M2/M3).
 */
public class TitanRenderer extends MobRenderer<TitanEntity, HumanoidModel<TitanEntity>> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/zombie/zombie.png");
    private static final float SCALE = 3.0F;

    public TitanRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 1.6F);
    }

    /**
     * Na primeira pessoa, quem está pilotando fica com o olho DENTRO da cabeça do modelo, então o
     * Titã não é desenhado para ele. Na terceira pessoa (F5) e para os outros jogadores, aparece normal.
     * Quando houver modelo/cabine de verdade (GeckoLib), aqui entra a cabine em primeira pessoa.
     */
    @Override
    public void render(TitanEntity titan, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.getCameraType().isFirstPerson() && titan.getControllingPassenger() == mc.player) {
            return;
        }
        super.render(titan, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    protected void scale(TitanEntity titan, PoseStack poseStack, float partialTick) {
        poseStack.scale(SCALE, SCALE, SCALE);
    }

    @Override
    public ResourceLocation getTextureLocation(TitanEntity titan) {
        return TEXTURE;
    }
}
