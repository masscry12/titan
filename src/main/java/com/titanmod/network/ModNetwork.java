package com.titanmod.network;

import com.titanmod.entity.TitanEntity;

import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModNetwork {
    /** Mude esta versão quando o formato dos pacotes mudar (impede cliente/servidor incompatíveis). */
    private static final String PROTOCOL_VERSION = "1";

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(DashPayload.TYPE, DashPayload.STREAM_CODEC, ModNetwork::handleDash);
    }

    /** Roda na thread principal do servidor. Valida que quem pediu realmente pilota um Titã. */
    private static void handleDash(DashPayload payload, IPayloadContext context) {
        Player player = context.player();
        if (player.getVehicle() instanceof TitanEntity titan && titan.getControllingPassenger() == player) {
            titan.tryConsumeDash();
        }
    }
}
