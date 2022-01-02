package dev.bohush.quicktravel.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.bohush.quicktravel.util.TeleportUtil;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import static net.minecraft.server.command.CommandManager.literal;

public class TeleportSpawnCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, boolean dedicated) {
        dispatcher.register(
            literal("spawn").executes(TeleportSpawnCommand::teleportToSpawn)
        );
    }

    private static int teleportToSpawn(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        var source = context.getSource();
        var player = source.getPlayer();
        var world = source.getWorld();

        if (player.world.getRegistryKey() != World.OVERWORLD) {
            throw new SimpleCommandExceptionType(TeleportUtil.ERROR_INVALID_DIMENSION).create();
        }

        if (!TeleportUtil.canTeleport(world, player, source.getPosition())) {
            throw new SimpleCommandExceptionType(TeleportUtil.ERROR_TOO_FAR_AWAY).create();
        }

        var spawnPos = world.getSpawnPos();
        var targetPosition = new Vec3d(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
        TeleportUtil.teleportPlayer(world, player, source.getPosition(), targetPosition);

        return Command.SINGLE_SUCCESS;
    }
}
