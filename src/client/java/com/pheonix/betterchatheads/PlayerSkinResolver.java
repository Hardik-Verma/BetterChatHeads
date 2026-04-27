package com.pheonix.betterchatheads;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.entity.player.SkinTextures;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PlayerSkinResolver {
    private static final Pattern CHAT_PREFIX = Pattern.compile("^<([A-Za-z0-9_]{1,16})>\\s*(.*)$");

    private final MinecraftClient client;
    private final Map<UUID, ResolvedHead> cache = new ConcurrentHashMap<>();

    public PlayerSkinResolver(MinecraftClient client) {
        this.client = client;
    }

    public Optional<PlayerListEntry> resolvePlayerEntry(String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return Optional.empty();
        }
        if (this.client.getNetworkHandler() == null) {
            return Optional.empty();
        }
        return this.client.getNetworkHandler().getPlayerList().stream()
                .filter(entry -> betterchatheads$candidateNames(entry).stream().anyMatch(name -> name.equalsIgnoreCase(playerName)))
                .findFirst();
    }

    public Optional<PlayerListEntry> resolvePlayerEntry(UUID playerUuid) {
        if (playerUuid == null || this.client.getNetworkHandler() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(this.client.getNetworkHandler().getPlayerListEntry(playerUuid));
    }

    public Optional<String> extractPlayerName(String rawChatLine) {
        return extractPlayerChat(rawChatLine).map(ChatSenderMatch::playerName);
    }

    public Optional<ResolvedChatSender> resolveChatSender(String rawChatLine) {
        if (isSkippableSystemMessage(rawChatLine)) {
            return Optional.empty();
        }

        Optional<ChatSenderMatch> vanillaMatch = extractPlayerChat(rawChatLine);
        if (vanillaMatch.isPresent()) {
            ChatSenderMatch match = vanillaMatch.get();
            return resolvePlayerEntry(match.playerName())
                    .map(entry -> new ResolvedChatSender(match.playerName(), match.message(), entry, true, ""));
        }

        if (rawChatLine == null || this.client.getNetworkHandler() == null) {
            return Optional.empty();
        }

        String line = rawChatLine.stripLeading();
        PlayerListEntry bestEntry = null;
        int bestIndex = Integer.MAX_VALUE;
        int bestNameLength = -1;
        String matchedName = null;
        for (PlayerListEntry entry : this.client.getNetworkHandler().getPlayerList()) {
            for (String playerName : betterchatheads$candidateNames(entry)) {
                int index = findNameToken(line, playerName);
                if (index < 0 || index > 200) {
                    continue;
                }
                int nameLength = playerName.length();
                if (index < bestIndex || (index == bestIndex && nameLength > bestNameLength)) {
                    bestEntry = entry;
                    bestIndex = index;
                    matchedName = playerName;
                    bestNameLength = nameLength;
                }
            }
        }

        if (bestEntry == null || matchedName == null) {
            return Optional.empty();
        }

        String playerName = bestEntry.getProfile().name();
        String rawTail = line.substring(Math.min(line.length(), bestIndex + matchedName.length()));
        String trailing = extractTrailingMessage(line, bestIndex, matchedName);
        if (!isLikelyPlayerChat(rawTail, trailing)) {
            return Optional.empty();
        }
        String prefix = line.substring(0, Math.max(0, bestIndex));
        return Optional.of(new ResolvedChatSender(playerName, trailing, bestEntry, false, prefix));
    }

    public Optional<ChatSenderMatch> extractPlayerChat(String rawChatLine) {
        if (isSkippableSystemMessage(rawChatLine)) {
            return Optional.empty();
        }
        Matcher matcher = CHAT_PREFIX.matcher(rawChatLine.stripLeading());
        if (!matcher.matches()) {
            return Optional.empty();
        }
        return Optional.of(new ChatSenderMatch(matcher.group(1), matcher.group(2)));
    }

    public ResolvedHead resolve(PlayerListEntry entry) {
        if (entry == null) {
            return fallbackHead(null, null);
        }
        return this.cache.computeIfAbsent(entry.getProfile().id(), ignored -> createResolvedHead(entry.getProfile(), entry));
    }

    public ResolvedHead resolve(GameProfile profile) {
        if (profile == null) {
            return fallbackHead(null, null);
        }
        return this.cache.computeIfAbsent(profile.id(), ignored -> createResolvedHead(profile, null));
    }

    public ResolvedHead resolveByName(String playerName) {
        return resolvePlayerEntry(playerName)
                .map(this::resolve)
                .orElseGet(() -> fallbackHead(null, playerName));
    }

    public ResolvedHead resolveByUuid(UUID playerUuid, String playerName) {
        return resolvePlayerEntry(playerUuid)
                .map(this::resolve)
                .orElseGet(() -> fallbackHead(playerUuid, playerName));
    }

    public boolean isLocalPlayer(String playerName) {
        return this.client.player != null && this.client.player.getGameProfile().name().equalsIgnoreCase(playerName);
    }

    public void clearCache() {
        this.cache.clear();
    }

    public ResolvedHead fallbackHead(UUID uuid, String playerName) {
        UUID fallbackUuid = uuid != null ? uuid : offlineUuid(playerName == null ? "Steve" : playerName);
        return new ResolvedHead(DefaultSkinHelper.getSkinTextures(fallbackUuid), true);
    }

    private ResolvedHead createResolvedHead(GameProfile profile, PlayerListEntry entry) {
        try {
            SkinTextures textures = entry != null ? entry.getSkinTextures() : DefaultSkinHelper.getSkinTextures(profile);
            if (textures != null && textures.body() != null && textures.body().texturePath() != null) {
                return new ResolvedHead(textures, true);
            }
        } catch (Throwable throwable) {
            BetterChatHeadsClient.LOGGER.debug("Falling back to default head for {}", profile.name(), throwable);
        }

        if (BetterChatHeadsClient.config().useDefaultHeadWhenMissingSkin) {
            return fallbackHead(profile.id(), profile.name());
        }

        return new ResolvedHead(DefaultSkinHelper.getSkinTextures(profile), false);
    }

    private static UUID offlineUuid(String playerName) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes(StandardCharsets.UTF_8));
    }

    private static Set<String> betterchatheads$candidateNames(PlayerListEntry entry) {
        Set<String> names = new LinkedHashSet<>();
        String profileName = entry.getProfile().name();
        if (profileName != null && !profileName.isBlank()) {
            names.add(profileName);
        }
        if (entry.getDisplayName() != null) {
            String displayName = entry.getDisplayName().getString().strip();
            if (!displayName.isBlank()) {
                names.add(displayName);
            }
        }
        return names;
    }

    public static boolean isSkippableSystemMessage(String rawChatLine) {
        if (rawChatLine == null) {
            return true;
        }
        String stripped = rawChatLine.stripLeading();
        String lower = stripped.toLowerCase();
        return stripped.contains("Saved screenshot as")
                || stripped.startsWith("[Screenshot saved")
                || stripped.startsWith("[System]")
                || stripped.startsWith("System:")
                || stripped.startsWith("[" + "CHAT" + "]")
                || stripped.startsWith("You ")
                || stripped.startsWith("Teleported ")
                || stripped.startsWith("Set ")
                || stripped.startsWith("Unknown command")
                || lower.contains(" joined the game")
                || lower.contains(" left the game")
                || lower.contains(" joined the lobby")
                || lower.contains(" left the lobby")
                || lower.contains(" solved the captcha")
                || lower.contains(" registration successful")
                || lower.startsWith("registration successful")
                || lower.contains(" captcha check")
                || lower.contains(" need to move a bit before talking")
                || lower.contains("you will not be able to play until you do that");
    }

    private static int findNameToken(String line, String playerName) {
        String lowerLine = line.toLowerCase();
        String lowerName = playerName.toLowerCase();
        int fromIndex = 0;
        while (fromIndex >= 0 && fromIndex < lowerLine.length()) {
            int index = lowerLine.indexOf(lowerName, fromIndex);
            if (index < 0) {
                return -1;
            }
            int end = index + lowerName.length();
            boolean validBefore = index == 0 || !Character.isLetterOrDigit(lowerLine.charAt(index - 1)) && lowerLine.charAt(index - 1) != '_';
            boolean validAfter = end >= lowerLine.length() || !Character.isLetterOrDigit(lowerLine.charAt(end)) && lowerLine.charAt(end) != '_';
            if (validBefore && validAfter) {
                return index;
            }
            fromIndex = index + 1;
        }
        return -1;
    }

    private static String extractTrailingMessage(String line, int playerIndex, String playerName) {
        int start = Math.min(line.length(), playerIndex + playerName.length());
        String trailing = line.substring(start).stripLeading();
        while (!trailing.isEmpty()) {
            char first = trailing.charAt(0);
            if (first == ':' || first == '-' || first == '>' || first == '»' || first == '›' || first == '|' || first == ']' || first == ')' || first == '\u2192') {
                trailing = trailing.substring(1).stripLeading();
                continue;
            }
            break;
        }
        return trailing;
    }

    private static boolean isLikelyPlayerChat(String rawTail, String trailing) {
        if (rawTail == null || trailing == null) {
            return false;
        }
        String compactTail = rawTail.stripLeading();
        String lower = trailing.stripLeading().toLowerCase();
        if (lower.isEmpty() || compactTail.isEmpty()) {
            return false;
        }
        if (lower.startsWith("joined the game")
                || lower.startsWith("left the game")
                || lower.startsWith("joined the lobby")
                || lower.startsWith("left the lobby")
                || lower.startsWith("joined")
                || lower.startsWith("left")
                || lower.startsWith("solved the captcha")
                || lower.startsWith("registration successful")
                || lower.startsWith("captcha check")
                || lower.startsWith("please type")
                || lower.startsWith("you need to move")
                || lower.startsWith("you will not be able to play")) {
            return false;
        }

        char first = compactTail.charAt(0);
        if (first == ':' || first == '>' || first == '»' || first == '›' || first == '|' || first == '-' || first == '\u2192') {
            return true;
        }

        int scanLimit = Math.min(compactTail.length(), 24);
        for (int index = 0; index < scanLimit; index++) {
            char current = compactTail.charAt(index);
            if (current == ':' || current == '>' || current == '»' || current == '›' || current == '|' || current == '\u2192') {
                return true;
            }
        }

        return compactTail.length() > 1 && Character.isWhitespace(compactTail.charAt(0));
    }

    public record ResolvedHead(SkinTextures skinTextures, boolean usable) {
    }

    public record ChatSenderMatch(String playerName, String message) {
    }

    public record ResolvedChatSender(String playerName, String message, PlayerListEntry entry, boolean restyleChat, String prefixText) {
    }
}
