package dev.bohush.quicktravel;

import dev.bohush.quicktravel.command.ModCommands;
import net.fabricmc.api.ModInitializer;

public class QuickTravel implements ModInitializer {
    @Override
    public void onInitialize() {
        ModCommands.registerCommands();
    }
}
