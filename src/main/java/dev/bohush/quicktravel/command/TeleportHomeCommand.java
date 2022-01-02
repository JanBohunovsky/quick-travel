package dev.bohush.quicktravel.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.bohush.quicktravel.util.TeleportUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

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

        if (!TeleportUtil.canTeleport(world, player, source.getPosition())) {
            throw new SimpleCommandExceptionType(TeleportUtil.ERROR_TOO_FAR_AWAY).create();
        }

        var targetPosition = getTargetPosition(player, world);
        if (targetPosition == null) {
            throw new SimpleCommandExceptionType(new LiteralText("You have no home bed or it was obstructed.")).create();
        }

        TeleportUtil.teleportPlayer(world, player, source.getPosition(), targetPosition);

        return Command.SINGLE_SUCCESS;
    }

    @Nullable
    private static Vec3d getTargetPosition(ServerPlayerEntity player, ServerWorld world) {
        var spawnPos = player.getSpawnPointPosition();
        if (spawnPos == null) {
            return null;
        }

        return PlayerEntity.findRespawnPosition(world, spawnPos, player.getSpawnAngle(), false, true)
            .orElse(null);
    }
}
