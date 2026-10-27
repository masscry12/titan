package com.titanmod.network;

import com.titanmod.TitanMod;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Cliente -> servidor: "o piloto apertou o dash". Não carrega dados; o servidor valida tudo. */
public record DashPayload() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<DashPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(TitanMod.MODID, "dash"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DashPayload> STREAM_CODEC =
            StreamCodec.unit(new DashPayload());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
