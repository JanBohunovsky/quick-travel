package dev.bohush.quicktravel.util;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class TeleportRequest {
    public static final long TIMEOUT_SECONDS = 120;
    private static final ArrayList<TeleportRequest> cache = new ArrayList<>();

    private final ServerPlayerEntity requester;
    private final ServerPlayerEntity target;

    private TeleportRequest(ServerPlayerEntity requester, ServerPlayerEntity target) {
        this.requester = requester;
        this.target = target;
    }

    public ServerPlayerEntity getRequester() {
        return requester;
    }

    public ServerPlayerEntity getTarget() {
        return target;
    }

    public void respond(String message) {
        requester.sendMessage(new LiteralText(message), false);
    }

    public void respondError(String errorMessage) {
        requester.sendMessage(
            new LiteralText("Teleport to " + target.getEntityName() + " has failed: ")
                .append(errorMessage)
                .formatted(Formatting.RED),
            false
        );
    }

    public static boolean create(ServerPlayerEntity requester, ServerPlayerEntity target, RequestTimedOutCallback callback) {
        var request = new TeleportRequest(requester, target);

        // Only one request per player
        if (cache.stream().anyMatch(r -> r.getRequester() == requester)) {
            return false;
        }

        cache.add(request);
        scheduleTimeout(request, callback);

        return true;
    }

    public static List<TeleportRequest> finish(@Nullable ServerPlayerEntity requester, ServerPlayerEntity target) {
        var result = new ArrayList<TeleportRequest>();

        for (int i = cache.size() - 1; i >= 0; i--) {
            var request = cache.get(i);

            if (request.getTarget() == target && (requester == null || request.getRequester() == requester)) {
                result.add(request);
                cache.remove(request);
            }
        }

        return result;
    }

    public static List<String> suggestPlayers(ServerPlayerEntity target) {
        var result = new ArrayList<String>();

        for (var request : cache) {
            if (request.target == target) {
                result.add(request.requester.getEntityName());
            }
        }

        return result;
    }

    private static void scheduleTimeout(TeleportRequest request, RequestTimedOutCallback callback) {
        var timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (cache.contains(request)) {
                    cache.remove(request);
                    callback.RequestTimedOut();
                }
            }
        }, TIMEOUT_SECONDS * 1000);
    }

    public interface RequestTimedOutCallback {
        void RequestTimedOut();
    }
}
