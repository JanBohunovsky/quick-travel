package dev.bohush.quicktravel;

import dev.bohush.quicktravel.command.ModCommands;
import net.fabricmc.api.DedicatedServerModInitializer;


public class QuickTravel implements DedicatedServerModInitializer {
    @Override
    public void onInitializeServer() {
        ModCommands.registerCommands();
    }
}
