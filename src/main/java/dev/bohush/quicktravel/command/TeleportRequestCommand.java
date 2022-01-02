package dev.bohush.quicktravel.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.bohush.quicktravel.util.TeleportRequest;
import dev.bohush.quicktravel.util.TeleportUtil;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class TeleportRequestCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, boolean dedicated) {
        dispatcher.register(
            literal("tpa")
                .then(argument("player", EntityArgumentType.player())
                    .executes(context -> requestTeleport(context, EntityArgumentType.getPlayer(context, "player")))
                )
        );

        dispatcher.register(
            literal("tpaccept")
                .executes(context -> acceptTeleport(context, null))
                .then(
                    argument("player", EntityArgumentType.player())
                        .suggests(TeleportRequestCommand::suggestRequesterResponse)
                        .executes(context -> acceptTeleport(context, EntityArgumentType.getPlayer(context, "player")))
                )
        );

        dispatcher.register(
            literal("tpdeny")
                .executes(context -> denyTeleport(context, null))
                .then(
                    argument("player", EntityArgumentType.player())
                        .suggests(TeleportRequestCommand::suggestRequesterResponse)
                        .executes(context -> denyTeleport(context, EntityArgumentType.getPlayer(context, "player")))
                )
        );
    }

    private static int requestTeleport(CommandContext<ServerCommandSource> context, ServerPlayerEntity target) throws CommandSyntaxException {
        var source = context.getSource();
        var player = source.getPlayer();
        var world = source.getWorld();

        if (player.world.getRegistryKey() != World.OVERWORLD) {
            throw new SimpleCommandExceptionType(TeleportUtil.ERROR_INVALID_DIMENSION).create();
        }

        if (!TeleportUtil.canTeleport(world, player)) {
            throw new SimpleCommandExceptionType(TeleportUtil.ERROR_TOO_FAR_AWAY).create();
        }

        if (TeleportUtil.getBedWakeUpPosition(world, target) == null) {
            throw new SimpleCommandExceptionType(new LiteralText(target.getEntityName()).append(TeleportUtil.ERROR_TARGET_NO_BED)).create();
        }

        var requesterName = player.getEntityName();
        var targetName = target.getEntityName();

        if (!TeleportRequest.create(player, target, () -> {
            player.sendMessage(
                new LiteralText("Teleport request to " + targetName  + " has timed out.")
                    .formatted(Formatting.RED),
                false
            );
        })) {
            throw new SimpleCommandExceptionType(new LiteralText("You already have a teleport request pending.")).create();
        }

        var message = new LiteralText("")
            .append(new LiteralText(requesterName).formatted(Formatting.YELLOW))
            .append(" has requested to teleport to your home:\n ")
            .append(new LiteralText("[Accept]")
                .styled(style -> style
                    .withColor(Formatting.GREEN)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpaccept " + requesterName))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText("/tpaccept " + requesterName)))
                )
            )
            .append(" ")
            .append(new LiteralText("[Deny]")
                .styled(style -> style
                    .withColor(Formatting.RED)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpdeny " + requesterName))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText("/tpdeny " + requesterName)))
                )
            )
            .append(new LiteralText(".\nThis request will time out in " + TeleportRequest.TIMEOUT_SECONDS + " seconds.")
                .formatted(Formatting.ITALIC, Formatting.GRAY)
            );

        target.sendMessage(message, false);
        source.sendFeedback(new LiteralText("Teleport to " + targetName + " has been requested."), false);

        return Command.SINGLE_SUCCESS;
    }

    private static int acceptTeleport(CommandContext<ServerCommandSource> context, @Nullable ServerPlayerEntity requester) throws CommandSyntaxException {
        var source = context.getSource();
        var player = source.getPlayer();
        var world = source.getWorld();
        var playerName = player.getEntityName();

        var requests = TeleportRequest.finish(requester, player);

        // Check if the target player has home bed and get its wake-up position.
        var targetPosition = TeleportUtil.getBedWakeUpPosition(world, player);
        if (targetPosition == null) {
            for (var request : requests) {
                request.respondError(playerName + TeleportUtil.ERROR_TARGET_NO_BED);
            }

            throw new SimpleCommandExceptionType(TeleportUtil.ERROR_NO_BED).create();
        }

        // Try to teleport all requesters
        for (var request : requests) {
            if (request.getRequester().world.getRegistryKey() != World.OVERWORLD) {
                request.respondError(TeleportUtil.ERROR_INVALID_DIMENSION.asString());
                continue;
            }

            if (!TeleportUtil.canTeleport(world, request.getRequester())) {
                request.respondError(TeleportUtil.ERROR_TOO_FAR_AWAY.asString());
                continue;
            }

            request.respond(playerName + " has accepted your teleport request.");
            TeleportUtil.teleportPlayer(world, request.getRequester(), targetPosition);
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int denyTeleport(CommandContext<ServerCommandSource> context, @Nullable ServerPlayerEntity requester) throws CommandSyntaxException {
        var source = context.getSource();
        var player = source.getPlayer();

        var requests = TeleportRequest.finish(requester, player);
        for (var request : requests) {
            request.respond(player.getEntityName() + " has denied your teleport request.");
        }

        return Command.SINGLE_SUCCESS;
    }

    private static CompletableFuture<Suggestions> suggestRequesterResponse(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        var player = context.getSource().getPlayer();
        var suggestions = TeleportRequest.suggestPlayers(player);

        for (var suggestion : suggestions) {
            builder.suggest(suggestion);
        }

        return builder.buildFuture();
    }
}
