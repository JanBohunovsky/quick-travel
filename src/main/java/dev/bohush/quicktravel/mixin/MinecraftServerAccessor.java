package dev.bohush.quicktravel.mixin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldSaveHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MinecraftServer.class)
public interface MinecraftServerAccessor {
    @Accessor
    WorldSaveHandler getSaveHandler();
}
