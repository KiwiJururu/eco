package com.livingecology.command;

import com.livingecology.debug.DebugFormatter;
import com.livingecology.territory.TerritoryManager;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;

public final class ModCommands {
    private ModCommands() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("livingecology")
                .then(Commands.literal("timescale")
                        .executes(ctx -> {
                            ServerLevel level = ctx.getSource().getLevel();
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    "Living Ecology timescale = x" + TerritoryManager.simulationScale(level)), false);
                            return 1;
                        })
                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.01D, 100.0D))
                                .requires(source -> source.hasPermission(2))
                                .executes(ctx -> {
                                    double scale = DoubleArgumentType.getDouble(ctx, "value");
                                    ServerLevel level = ctx.getSource().getLevel();
                                    TerritoryManager.setSimulationScale(level, scale);
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "Living Ecology timescale alterado para x" + scale), true);
                                    return 1;
                                })))
                .then(Commands.literal("debug")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            BlockPos pos = BlockPos.containing(ctx.getSource().getPosition());
                            DebugFormatter.sendLocation(player, ctx.getSource().getLevel(), pos);
                            return 1;
                        })));
    }
}
