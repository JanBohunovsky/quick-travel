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
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
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
            throw new SimpleCommandExceptionType(new LiteralText("This command can only be used in the overworld.")).create();
        }

        var targetPosition = getTargetPosition(player, world);
        if (targetPosition == null) {
            throw new SimpleCommandExceptionType(new LiteralText("You have no home bed or it was obstructed.")).create();
        }

        var blockPos = new BlockPos(targetPosition);
        var chunkPos = new ChunkPos(blockPos);
        world.getChunkManager().addTicket(ChunkTicketType.POST_TELEPORT, chunkPos, 1, player.getId());

        player.stopRiding();
        if (player.isSleeping()) {
            player.wakeUp(true, true);
        }

        player.networkHandler.requestTeleport(targetPosition.getX(), targetPosition.getY(), targetPosition.getZ(), player.getYaw(), player.getPitch());
        player.onLanding();
        player.setVelocity(Vec3d.ZERO);
        player.setOnGround(true);

        TeleportUtil.spawnParticles(world, source.getPosition(), targetPosition);
        TeleportUtil.playSound(world, source.getPosition(), targetPosition);

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
