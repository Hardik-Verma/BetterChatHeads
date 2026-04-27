package com.pheonix.betterchatheads;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class BetterChatHeadsUpdateChecker {
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final String PROJECT_SLUG = "betterchatheads";
    private static final String GAME_VERSION = "1.21.11";

    private static boolean started;
    private static boolean promptShown;
    private static UpdateInfo updateInfo;

    private BetterChatHeadsUpdateChecker() {
    }

    public static void start() {
        if (started || !BetterChatHeadsClient.config().enableUpdateChecker) {
            return;
        }

        started = true;
        String apiUrl = "https://api.modrinth.com/v2/project/" + PROJECT_SLUG + "/version"
                + "?loaders=" + urlEncode("[\"fabric\"]")
                + "&game_versions=" + urlEncode("[\"" + GAME_VERSION + "\"]");

        HttpRequest request = HttpRequest.newBuilder(URI.create(apiUrl))
                .timeout(Duration.ofSeconds(7))
                .header("User-Agent", "BetterChatHeads/" + BetterChatHeadsClient.currentVersion())
                .GET()
                .build();

        CompletableFuture<HttpResponse<String>> future = HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        future.thenApply(HttpResponse::body)
                .thenApply(BetterChatHeadsUpdateChecker::parseUpdateInfo)
                .thenAccept(info -> {
                    if (info != null && isNewer(info.version(), BetterChatHeadsClient.currentVersion())) {
                        updateInfo = info;
                    }
                })
                .exceptionally(throwable -> {
                    BetterChatHeadsClient.LOGGER.debug("Update check failed", throwable);
                    return null;
                });
    }

    public static void onConfigUpdated() {
        if (!started && BetterChatHeadsClient.config().enableUpdateChecker) {
            start();
        }
    }

    public static void tick(MinecraftClient client) {
        if (promptShown || updateInfo == null) {
            return;
        }
        if (!BetterChatHeadsClient.config().enableUpdateChecker) {
            promptShown = true;
            return;
        }
        if (!BetterChatHeadsClient.config().allowUpdatePromptInGame) {
            if (client.world != null || client.currentScreen == null) {
                return;
            }
        } else if (client.currentScreen == null && client.world == null) {
            return;
        }

        promptShown = true;
        client.setScreen(new BetterChatHeadsUpdateScreen(client.currentScreen, updateInfo));
    }

    private static UpdateInfo parseUpdateInfo(String responseBody) {
        JsonArray versions = JsonParser.parseString(responseBody).getAsJsonArray();
        if (versions.isEmpty()) {
            return null;
        }

        JsonObject latest = versions.get(0).getAsJsonObject();
        String version = getString(latest, "version_number");
        String versionId = getString(latest, "id");
        String changelog = getString(latest, "changelog");
        if (version == null || version.isBlank()) {
            return null;
        }

        String updateUrl = versionId == null || versionId.isBlank()
                ? "https://modrinth.com/mod/" + PROJECT_SLUG
                : "https://modrinth.com/mod/" + PROJECT_SLUG + "/version/" + versionId;
        return new UpdateInfo(version, updateUrl, changelog);
    }

    private static String getString(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return null;
        }
        return element.getAsString();
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static boolean isNewer(String candidate, String current) {
        VersionParts candidateParts = VersionParts.parse(candidate);
        VersionParts currentParts = VersionParts.parse(current);
        int baseComparison = compareTokens(candidateParts.base(), currentParts.base());
        if (baseComparison != 0) {
            return baseComparison > 0;
        }
        if (candidateParts.preRelease().length == 0 && currentParts.preRelease().length == 0) {
            return false;
        }
        if (candidateParts.preRelease().length == 0) {
            return true;
        }
        if (currentParts.preRelease().length == 0) {
            return false;
        }
        return compareTokens(candidateParts.preRelease(), currentParts.preRelease()) > 0;
    }

    private static int compareTokens(String[] left, String[] right) {
        int count = Math.max(left.length, right.length);
        for (int index = 0; index < count; index++) {
            if (index >= left.length) {
                return -1;
            }
            if (index >= right.length) {
                return 1;
            }

            String leftToken = left[index];
            String rightToken = right[index];
            boolean leftNumeric = leftToken.chars().allMatch(Character::isDigit);
            boolean rightNumeric = rightToken.chars().allMatch(Character::isDigit);
            int comparison;
            if (leftNumeric && rightNumeric) {
                comparison = Integer.compare(Integer.parseInt(leftToken), Integer.parseInt(rightToken));
            } else {
                comparison = normalizeQualifier(leftToken).compareTo(normalizeQualifier(rightToken));
            }

            if (comparison != 0) {
                return comparison;
            }
        }
        return 0;
    }

    private static String normalizeQualifier(String token) {
        return switch (token.toLowerCase(Locale.ROOT)) {
            case "alpha", "a" -> "0";
            case "beta", "b" -> "1";
            case "pre", "preview" -> "2";
            case "rc" -> "3";
            default -> "4-" + token.toLowerCase(Locale.ROOT);
        };
    }

    public record UpdateInfo(String version, String updateUrl, String changelog) {
    }

    private record VersionParts(String[] base, String[] preRelease) {
        private static VersionParts parse(String raw) {
            String[] split = raw.split("-", 2);
            return new VersionParts(tokenize(split[0]), split.length > 1 ? tokenize(split[1]) : new String[0]);
        }

        private static String[] tokenize(String value) {
            return value.toLowerCase(Locale.ROOT).split("[^a-z0-9]+");
        }
    }
}
