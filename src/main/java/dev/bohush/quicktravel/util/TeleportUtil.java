package dev.bohush.quicktravel.util;

import dev.bohush.quicktravel.mixin.MinecraftServerAccessor;
import dev.bohush.quicktravel.mixin.WorldSaveHandlerAccessor;
import net.minecraft.block.BedBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashSet;

public class TeleportUtil {
    public static final Text ERROR_INVALID_DIMENSION = new LiteralText("This command can only be used in the overworld.");
    public static final Text ERROR_TOO_FAR_AWAY = new LiteralText("You are too far away from a bed or world spawn.");
    public static final Text ERROR_NO_BED = new LiteralText("You have no home bed or it was obstructed.");
    public static final Text ERROR_TARGET_NO_BED = new LiteralText(" has no home bed or it was obstructed.");

    private static final Logger LOGGER = LogManager.getLogger();

    public static void teleportPlayer(ServerWorld world, ServerPlayerEntity player, Vec3d target) {
        var from = player.getPos();

        // Load the chunk
        var chunkPos = new ChunkPos(new BlockPos(target));
        world.getChunkManager().addTicket(ChunkTicketType.POST_TELEPORT, chunkPos, 1, player.getId());

        player.stopRiding();
        if (player.isSleeping()) {
            player.wakeUp(true, true);
        }

        player.networkHandler.requestTeleport(target.getX(), target.getY(), target.getZ(), 0, 0);
        player.setVelocity(Vec3d.ZERO);

        spawnParticles(world, from, target);
        playSound(world, from, target);
    }

    public static boolean canTeleport(ServerWorld world, ServerPlayerEntity player) {
        if (world.getRegistryKey() != World.OVERWORLD) {
            return false;
        }

        var from = player.getPos();

        // 1. Check world spawn and the player's bed
        if (isNearby(from, world.getSpawnPos())) {
            return true;
        }

        if (isNearby(from, getBedPosition(world, player))) {
            return true;
        }

        // 2. Check beds of all online players
        var checkedPlayers = new HashSet<String>();
        checkedPlayers.add(player.getUuidAsString());

        for (var onlinePlayer : world.getPlayers()) {
            if (checkedPlayers.contains(onlinePlayer.getUuidAsString())) {
                continue;
            }
            checkedPlayers.add(onlinePlayer.getUuidAsString());

            if (isNearby(from, getBedPosition(world, onlinePlayer))) {
                return true;
            }
        }

        // 3. Check beds of all offline players
        var saveHandler = ((MinecraftServerAccessor)world.getServer()).getSaveHandler();
        var playerDataDir = ((WorldSaveHandlerAccessor)saveHandler).getPlayerDataDir();

        for (var savedPlayerUuid : saveHandler.getSavedPlayerIds()) {
            if (checkedPlayers.contains(savedPlayerUuid)) {
                continue;
            }
            checkedPlayers.add(savedPlayerUuid);

            if (isNearby(from, getBedPositionFromSavedPlayer(world, savedPlayerUuid, playerDataDir))) {
                return true;
            }
        }

        return false;
    }

    public static boolean isNearby(Vec3d playerPos, @Nullable BlockPos targetPos) {
        if (targetPos == null) {
            return false;
        }
        return targetPos.isWithinDistance(playerPos, 25);
    }

    @Nullable
    public static Vec3d getBedWakeUpPosition(ServerWorld world, ServerPlayerEntity player) {
        var spawnPos = player.getSpawnPointPosition();
        if (spawnPos == null) {
            return null;
        }

        return PlayerEntity.findRespawnPosition(world, spawnPos, player.getSpawnAngle(), false, true)
            .orElse(null);
    }

    @Nullable
    public static BlockPos getBedPosition(ServerWorld world, ServerPlayerEntity player) {
        var spawnPos = player.getSpawnPointPosition();
        if (spawnPos == null) {
            return null;
        }

        var chunkPos = new ChunkPos(spawnPos);
        if (world.isChunkLoaded(chunkPos.toLong()) && world.getBlockState(spawnPos).getBlock() instanceof BedBlock) {
            return spawnPos;
        }

        return null;
    }

    @Nullable
    private static BlockPos getBedPositionFromSavedPlayer(ServerWorld world, String playerUuid, File playerDataDir) {
        NbtCompound nbt = null;
        try {
            var file = new File(playerDataDir, playerUuid + ".dat");
            if (file.exists() && file.isFile()) {
                nbt = NbtIo.readCompressed(file);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to load player data for {}", playerUuid);
            return null;
        }

        if (nbt == null
            || !nbt.contains("SpawnX", NbtElement.NUMBER_TYPE)
            || !nbt.contains("SpawnY", NbtElement.NUMBER_TYPE)
            || !nbt.contains("SpawnZ", NbtElement.NUMBER_TYPE)) {
            return null;
        }

        var spawnPos = new BlockPos(nbt.getInt("SpawnX"), nbt.getInt("SpawnY"), nbt.getInt("SpawnZ"));
        var chunkPos = new ChunkPos(spawnPos);

        if (world.isChunkLoaded(chunkPos.toLong()) && world.getBlockState(spawnPos).getBlock() instanceof BedBlock) {
            return spawnPos;
        }

        return null;
    }

    /**
     * Spawns teleportation particles.
     * @param origin Where the player is teleporting <b>from</b>.
     * @param target Where the player is teleporting <b>to</b>.
     */
    public static void spawnParticles(ServerWorld world, Vec3d origin, Vec3d target) {
        world.spawnParticles(ParticleTypes.POOF, origin.getX(), origin.getY() + 1, origin.getZ(), 32, 0, 0.5, 0, 0.2);
        world.spawnParticles(ParticleTypes.PORTAL, target.getX(), target.getY() + 1, target.getZ(), 32, 0, 0.5, 0, 1);
    }

    /**
     * Plays teleportation sound.
     * @param origin Where the player is teleporting <b>from</b>.
     * @param target Where the player is teleporting <b>to</b>.
     */
    public static void playSound(ServerWorld world, Vec3d origin, Vec3d target) {
        playSound(world, origin);
        playSound(world, target);
    }

    public static void playSound(ServerWorld world, Vec3d target) {
        world.playSound(
            null,
            target.getX(),
            target.getY(),
            target.getZ(),
            SoundEvents.ENTITY_ENDERMAN_TELEPORT,
            SoundCategory.PLAYERS,
            1,
            1
        );
    }
}
