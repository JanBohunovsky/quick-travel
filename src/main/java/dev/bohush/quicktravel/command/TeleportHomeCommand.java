package dev.bohush.quicktravel.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.bohush.quicktravel.util.TeleportUtil;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.world.World;

import static net.minecraft.server.command.CommandManager.literal;

public class TeleportHomeCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, boolean dedicated) {
        dispatcher.register(
            literal("home").executes(TeleportHomeCommand::teleportHome)
        );
    }

    private static int teleportHome(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        var source = context.getSource();
        var player = source.getPlayer();
        var world = source.getWorld();

        if (player.world.getRegistryKey() != World.OVERWORLD) {
            throw new SimpleCommandExceptionType(TeleportUtil.ERROR_INVALID_DIMENSION).create();
        }

        if (!TeleportUtil.canTeleport(world, player)) {
            throw new SimpleCommandExceptionType(TeleportUtil.ERROR_TOO_FAR_AWAY).create();
        }

        var targetPosition = TeleportUtil.getBedWakeUpPosition(world, player);
        if (targetPosition == null) {
            throw new SimpleCommandExceptionType(TeleportUtil.ERROR_NO_BED).create();
        }

        TeleportUtil.teleportPlayer(world, player, targetPosition);

        return Command.SINGLE_SUCCESS;
    }
}
