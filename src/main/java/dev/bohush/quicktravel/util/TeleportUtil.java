package dev.bohush.quicktravel.util;

import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

public class TeleportUtil {
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
