package com.titanmod.client;

import com.titanmod.TitanMod;
import com.titanmod.entity.TitanEntity;
import com.titanmod.network.DashPayload;
import com.titanmod.network.FirePayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Eventos de jogo do lado do cliente: aqui o dash é detectado e aplicado. */
@EventBusSubscriber(modid = TitanMod.MODID, value = Dist.CLIENT)
public class TitanClientEvents {

    /** Intervalo mínimo entre dashes (ticks), para não gastar 2 cargas antes do servidor responder. */
    private static final int DASH_COOLDOWN_TICKS = 10;
    private static int lastDashTick = -1000;

    /** Enquanto pilota, o botão esquerdo é o gatilho do Titã: cancela o ataque/quebra de bloco do vanilla. */
    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.isAttack() && mc.player != null && mc.player.getVehicle() instanceof TitanEntity) {
            event.setCanceled(true);
            event.setSwingHit(false);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        // Segurar o botão esquerdo = atirar. O servidor controla cadência, calor e dano.
        if (player != null && mc.screen == null && mc.mouseHandler.isMouseGrabbed()
                && mc.options.keyAttack.isDown()
                && player.getVehicle() instanceof TitanEntity gunTitan
                && gunTitan.getControllingPassenger() == player
                && !gunTitan.isOverheated()) {
            PacketDistributor.sendToServer(new FirePayload());
        }

        while (TitanClient.DASH_KEY.consumeClick()) {
            if (player == null
                    || !(player.getVehicle() instanceof TitanEntity titan)
                    || titan.getControllingPassenger() != player) {
                continue;
            }
            if (titan.getDashCharges() <= 0 || player.tickCount - lastDashTick < DASH_COOLDOWN_TICKS) {
                continue;
            }
            lastDashTick = player.tickCount;
            applyDashImpulse(player, titan);
            // O servidor valida, gasta a carga e toca som/partículas para todos.
            PacketDistributor.sendToServer(new DashPayload());
        }
    }

    /** Impulso na direção em que o piloto está apertando (sem teclas = para frente). */
    private static void applyDashImpulse(LocalPlayer player, TitanEntity titan) {
        float forward = player.input.forwardImpulse;
        float strafe = player.input.leftImpulse;
        if (forward == 0.0F && strafe == 0.0F) {
            forward = 1.0F;
        }
        float yaw = player.getYRot() * Mth.DEG_TO_RAD;
        double sin = Mth.sin(yaw);
        double cos = Mth.cos(yaw);
        double dx = strafe * cos - forward * sin;
        double dz = forward * cos + strafe * sin;
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 1.0E-4D) {
            return;
        }
        dx /= length;
        dz /= length;

        titan.setDeltaMovement(dx * TitanEntity.DASH_STRENGTH,
                Math.max(titan.getDeltaMovement().y, 0.1D),
                dz * TitanEntity.DASH_STRENGTH);
    }
}
