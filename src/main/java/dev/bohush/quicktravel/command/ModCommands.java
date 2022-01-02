package dev.bohush.quicktravel.command;

import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;

public class ModCommands {
    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register(TeleportHomeCommand::register);
        CommandRegistrationCallback.EVENT.register(TeleportSpawnCommand::register);
//        CommandRegistrationCallback.EVENT.register(TeleportRequestCommand::register);
    }
}
