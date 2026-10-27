package com.titanmod.network;

import com.titanmod.TitanMod;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Cliente -> servidor: "o piloto está segurando o botão de tiro".
 * Enviado a cada tick enquanto segura; o servidor aplica cadência, calor e dano.
 * A direção do tiro vem da mira do jogador no servidor, não do pacote, então não dá para forjar alvo.
 */
public record FirePayload() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<FirePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(TitanMod.MODID, "fire"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FirePayload> STREAM_CODEC =
            StreamCodec.unit(new FirePayload());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
