package com.titanmod.command;

import java.util.List;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.titanmod.TitanMod;
import com.titanmod.entity.TitanEntity;
import com.titanmod.registry.ModEntities;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * /titan spawn          -> cria um Titã na sua posição (você vira o dono)
 * /titan shield <valor> -> define o escudo do Titã mais próximo (teste)
 * /titan clear          -> remove todos os Titãs que são seus
 */
@EventBusSubscriber(modid = TitanMod.MODID)
public class TitanCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("titan")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("spawn").executes(TitanCommand::spawn))
                .then(Commands.literal("clear").executes(TitanCommand::clear))
                .then(Commands.literal("shield")
                        .then(Commands.argument("valor", IntegerArgumentType.integer(0, TitanEntity.MAX_SHIELD))
                                .executes(TitanCommand::setShield))));
    }

    private static int spawn(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        ServerLevel level = src.getLevel();
        BlockPos pos = BlockPos.containing(src.getPosition());

        TitanEntity titan = ModEntities.TITAN.get().spawn(level, pos, MobSpawnType.COMMAND);
        if (titan == null) {
            src.sendFailure(Component.translatable("command.titanmod.spawn.fail"));
            return 0;
        }
        if (src.getEntity() instanceof ServerPlayer player) {
            titan.setOwnerUUID(player.getUUID());
            titan.setYRot(player.getYRot());
        }
        src.sendSuccess(() -> Component.translatable("command.titanmod.spawn.ok"), true);
        return 1;
    }

    private static int clear(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            src.sendFailure(Component.translatable("command.titanmod.player_only"));
            return 0;
        }
        List<TitanEntity> titans = src.getLevel().getEntitiesOfClass(TitanEntity.class,
                player.getBoundingBox().inflate(512.0D), t -> t.isOwnedBy(player));
        titans.forEach(TitanEntity::discard);
        int count = titans.size();
        src.sendSuccess(() -> Component.translatable("command.titanmod.clear.ok", count), true);
        return count;
    }

    private static int setShield(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        int value = IntegerArgumentType.getInteger(ctx, "valor");
        List<TitanEntity> titans = src.getLevel().getEntitiesOfClass(TitanEntity.class,
                new net.minecraft.world.phys.AABB(BlockPos.containing(src.getPosition())).inflate(32.0D));
        if (titans.isEmpty()) {
            src.sendFailure(Component.translatable("command.titanmod.none_nearby"));
            return 0;
        }
        TitanEntity nearest = titans.stream()
                .min((a, b) -> Double.compare(a.distanceToSqr(src.getPosition()), b.distanceToSqr(src.getPosition())))
                .get();
        nearest.setShield(value);
        src.sendSuccess(() -> Component.translatable("command.titanmod.shield.ok", value), true);
        return 1;
    }
}
